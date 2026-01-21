package me.shinsunyoung.projectweatherly.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.config.ApiConfig;
import me.shinsunyoung.projectweatherly.common.util.DateUtil;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiConfig apiConfig;

    // 좌표 및 지역명 캐시
    private static final Map<String, Map<String, String>> GRID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> REGION_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * 통합 날씨 데이터 조회 (특보 분석 포함)
     */
    @Cacheable(value = "weatherAllData", key = "#regionCode + '_' + #liteMode", unless = "#result == null")
    public WeatherResponseDTO getAllWeatherData(String regionCode, boolean liteMode) {
        try {
            log.info("날씨 데이터 통합 조회: {} (liteMode: {})", regionCode, liteMode);

            // 1. 현재 날씨 조회
            CompletableFuture<WeatherResponseDTO.CurrentWeather> currentFuture =
                    CompletableFuture.supplyAsync(() -> getCurrentWeatherCached(regionCode));

            // 2. 예보 조회 (Lite 모드 분기)
            CompletableFuture<List<WeatherResponseDTO.HourlyForecast>> todayHourlyFuture;
            CompletableFuture<List<WeatherResponseDTO.HourlyForecast>> tomorrowHourlyFuture;
            CompletableFuture<List<WeatherResponseDTO.DailyForecast>> weeklyFuture;

            if (liteMode) {
                todayHourlyFuture = CompletableFuture.completedFuture(null);
                tomorrowHourlyFuture = CompletableFuture.completedFuture(null);
                weeklyFuture = CompletableFuture.completedFuture(null);
            } else {
                todayHourlyFuture = CompletableFuture.supplyAsync(() -> getHourlyForecastCached(regionCode, 0));
                tomorrowHourlyFuture = CompletableFuture.supplyAsync(() -> getHourlyForecastCached(regionCode, 1));
                weeklyFuture = CompletableFuture.supplyAsync(() -> getWeeklyForecastCached(regionCode));
            }

            // 3. 요약 정보 생성
            CompletableFuture<WeatherResponseDTO.WeatherSummary> summaryFuture;
            if (liteMode) {
                summaryFuture = CompletableFuture.completedFuture(null);
            } else {
                summaryFuture = CompletableFuture.allOf(todayHourlyFuture, tomorrowHourlyFuture, weeklyFuture)
                        .thenApply(v -> createWeatherSummary(todayHourlyFuture.join(), tomorrowHourlyFuture.join(), weeklyFuture.join()));
            }

            CompletableFuture.allOf(currentFuture, todayHourlyFuture, tomorrowHourlyFuture, weeklyFuture, summaryFuture).join();

            WeatherResponseDTO.CurrentWeather current = currentFuture.join();

            // [핵심] 현재 위치의 날씨를 기반으로 기상 특보 분석
            WeatherResponseDTO.WeatherWarning warning = analyzeWeatherWarning(current);

            return WeatherResponseDTO.builder()
                    .regionName(getRegionNameCached(regionCode))
                    .regionCode(regionCode)
                    .currentTime(DateUtil.getCurrentFormattedDateTime())
                    .current(current)
                    .hourly(todayHourlyFuture.join())
                    .tomorrowHourly(tomorrowHourlyFuture.join())
                    .daily(weeklyFuture.join())
                    .summary(summaryFuture.join())
                    .warning(warning) // 특보 정보 포함
                    .isMock(false)
                    .build();

        } catch (Exception e) {
            log.error("날씨 통합 조회 실패", e);
            return createFallbackWeatherData(regionCode, liteMode);
        }
    }

    /**
     * [분석 로직] 실시간 날씨 기반 특보 판단
     */
    private WeatherResponseDTO.WeatherWarning analyzeWeatherWarning(WeatherResponseDTO.CurrentWeather current) {
        if (current == null) return new WeatherResponseDTO.WeatherWarning(false, "정보 없음", "기상 정보를 불러올 수 없습니다.", "safe");

        double temp = current.getTemperature() != null ? current.getTemperature() : 0.0;
        double rain = current.getPrecipitation() != null ? current.getPrecipitation() : 0.0;
        double wind = current.getWindSpeed() != null ? current.getWindSpeed() : 0.0;

        // 1. 호우
        if (rain >= 30)
            return new WeatherResponseDTO.WeatherWarning(true, "호우경보", "시간당 30mm 이상의 매우 강한 비가 내리고 있습니다.", "danger");
        if (rain >= 15)
            return new WeatherResponseDTO.WeatherWarning(true, "호우주의보", "강한 비가 내리고 있습니다. 안전에 유의하세요.", "caution");

        // 2. 강풍
        if (wind >= 20)
            return new WeatherResponseDTO.WeatherWarning(true, "강풍경보", "매우 강한 바람이 불고 있습니다. 시설물 관리에 유의하세요.", "danger");
        if (wind >= 14) return new WeatherResponseDTO.WeatherWarning(true, "강풍주의보", "강한 바람이 불고 있습니다.", "caution");

        // 3. 폭염
        if (temp >= 35)
            return new WeatherResponseDTO.WeatherWarning(true, "폭염경보", "체감온도 35℃ 이상의 무더위가 이어집니다.", "danger");
        if (temp >= 33)
            return new WeatherResponseDTO.WeatherWarning(true, "폭염주의보", "무더운 날씨입니다. 야외활동을 자제하세요.", "caution");

        // 4. 한파
        if (temp <= -15) return new WeatherResponseDTO.WeatherWarning(true, "한파경보", "매우 심한 추위가 예상됩니다.", "danger");
        if (temp <= -12) return new WeatherResponseDTO.WeatherWarning(true, "한파주의보", "급격한 기온 하강에 유의하세요.", "caution");

        // 5. 특보 없음
        return new WeatherResponseDTO.WeatherWarning(false, "기상특보 없음", "현재 발효된 기상특보가 없습니다.", "safe");
    }

    // ===== 캐시 적용 개별 조회 메서드 =====

    @Cacheable(value = "currentWeather", key = "#regionCode", unless = "#result == null")
    public WeatherResponseDTO.CurrentWeather getCurrentWeatherCached(String regionCode) {
        try {
            return getCurrentWeatherFromUltraShort(regionCode);
        } catch (Exception e) {
            return createDefaultCurrentWeather();
        }
    }

    @Cacheable(value = "hourlyForecast", key = "#regionCode + '_' + #dayOffset", unless = "#result.isEmpty()")
    public List<WeatherResponseDTO.HourlyForecast> getHourlyForecastCached(String regionCode, int dayOffset) {
        try {
            return dayOffset == 0 ? getTodayHourlyForecast(regionCode) : getTomorrowHourlyForecast(regionCode);
        } catch (Exception e) {
            return createDefaultHourlyForecast24h(dayOffset);
        }
    }

    @Cacheable(value = "weeklyForecast", key = "#regionCode", unless = "#result.isEmpty()")
    public List<WeatherResponseDTO.DailyForecast> getWeeklyForecastCached(String regionCode) {
        try {
            return getShortTermWeeklyForecast(regionCode);
        } catch (Exception e) {
            return createDefaultWeeklyForecastWithAmPm();
        }
    }

    // ===== API 호출 로직 =====

    private Map<String, Object> fetchAllWeatherDataInOneCall(String regionCode) {
        try {
            Map<String, String> gridCoords = getGridCoordinatesCached(regionCode);
            LocalDateTime now = LocalDateTime.now();
            String baseDate = DateUtil.formatDateOnly(now);
            String baseTime = getBaseTimeForShortTerm(now);

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getVilageFcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1).queryParam("numOfRows", 1000).queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate).queryParam("base_time", baseTime)
                    .queryParam("nx", gridCoords.get("nx")).queryParam("ny", gridCoords.get("ny"))
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("response").path("body").path("items").path("item");
                if (items.isArray() && items.size() > 0) {
                    Map<String, Object> allData = new HashMap<>();
                    allData.put("current", parseCurrentWeatherFromItems(items));
                    allData.put("hourly", parseHourlyForecastFromItems(items, 0));
                    allData.put("tomorrowHourly", parseHourlyForecastFromItems(items, 1));
                    allData.put("weekly", parseWeeklyForecastFromItems(items));
                    return allData;
                }
            }
        } catch (Exception e) {
            log.error("통합 API 호출 실패: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private WeatherResponseDTO.CurrentWeather getCurrentWeatherFromUltraShort(String regionCode) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String baseDate = DateUtil.formatDateOnly(now);
            String baseTime = getBaseTimeForCurrent(now);
            Map<String, String> gridCoords = getGridCoordinatesCached(regionCode);

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getUltraSrtNcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1).queryParam("numOfRows", 10).queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate).queryParam("base_time", baseTime)
                    .queryParam("nx", gridCoords.get("nx")).queryParam("ny", gridCoords.get("ny"))
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                Map<String, String> data = new HashMap<>();
                for (JsonNode item : items) data.put(item.path("category").asText(), item.path("obsrValue").asText());

                double temp = parseDoubleSafe(data.get("T1H"), 20.0);
                double humidity = parseDoubleSafe(data.get("REH"), 50.0);
                double windSpeed = parseDoubleSafe(data.get("WSD"), 2.5);
                String ptyCode = data.getOrDefault("PTY", "0");
                String skyCode = data.getOrDefault("SKY", "1");

                return WeatherResponseDTO.CurrentWeather.builder()
                        .temperature(temp).feelsLike(calculateFeelsLike(temp, humidity, windSpeed))
                        .humidity(humidity).windSpeed(windSpeed).windDirection("북서풍")
                        .precipitation(parseDoubleSafe(data.get("RN1"), 0.0))
                        .weatherCondition(getWeatherConditionFromCode(skyCode, ptyCode))
                        .weatherIcon(getWeatherIconFromCode(skyCode, ptyCode, now.getHour()))
                        .updateTime(LocalDateTime.now()).build();
            }
        } catch (Exception e) {
        }
        return createDefaultCurrentWeather();
    }

    private List<WeatherResponseDTO.HourlyForecast> getTodayHourlyForecast(String regionCode) {
        try {
            Map<String, Object> allData = fetchAllWeatherDataInOneCall(regionCode);
            if (allData.containsKey("hourly")) return (List<WeatherResponseDTO.HourlyForecast>) allData.get("hourly");
        } catch (Exception e) {
        }
        return createDefaultHourlyForecast24h(0);
    }

    private List<WeatherResponseDTO.HourlyForecast> getTomorrowHourlyForecast(String regionCode) {
        try {
            Map<String, Object> allData = fetchAllWeatherDataInOneCall(regionCode);
            if (allData.containsKey("tomorrowHourly"))
                return (List<WeatherResponseDTO.HourlyForecast>) allData.get("tomorrowHourly");
        } catch (Exception e) {
        }
        return createDefaultHourlyForecast24h(1);
    }

    private List<WeatherResponseDTO.DailyForecast> getShortTermWeeklyForecast(String regionCode) {
        try {
            Map<String, Object> allData = fetchAllWeatherDataInOneCall(regionCode);
            if (allData.containsKey("weekly")) return (List<WeatherResponseDTO.DailyForecast>) allData.get("weekly");
        } catch (Exception e) {
        }
        return createDefaultWeeklyForecastWithAmPm();
    }

    // ===== 파싱 로직 =====

    private WeatherResponseDTO.CurrentWeather parseCurrentWeatherFromItems(JsonNode items) {
        LocalDateTime now = LocalDateTime.now();
        String nowDate = DateUtil.formatDateOnly(now);
        String nowTime = String.format("%02d00", now.getHour());
        double temp = 20.0, humidity = 50.0, windSpeed = 2.5;
        String sky = "1", pty = "0";

        for (JsonNode item : items) {
            if (item.path("fcstDate").asText().equals(nowDate) && item.path("fcstTime").asText().equals(nowTime)) {
                String cat = item.path("category").asText();
                String val = item.path("fcstValue").asText();
                switch (cat) {
                    case "TMP" -> temp = parseDoubleSafe(val);
                    case "REH" -> humidity = parseDoubleSafe(val);
                    case "WSD" -> windSpeed = parseDoubleSafe(val);
                    case "SKY" -> sky = val;
                    case "PTY" -> pty = val;
                }
            }
        }
        return WeatherResponseDTO.CurrentWeather.builder().temperature(temp).feelsLike(calculateFeelsLike(temp, humidity, windSpeed)).humidity(humidity).windSpeed(windSpeed).windDirection("북서풍").precipitation(0.0).weatherCondition(getWeatherConditionFromCode(sky, pty)).weatherIcon(getWeatherIconFromCode(sky, pty, now.getHour())).updateTime(LocalDateTime.now()).build();
    }

    private List<WeatherResponseDTO.HourlyForecast> parseHourlyForecastFromItems(JsonNode items, int dayOffset) {
        LocalDate targetDate = LocalDate.now().plusDays(dayOffset);
        String targetDateStr = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        Map<Integer, WeatherDataPoint> hourData = new TreeMap<>();

        for (JsonNode item : items) {
            if (!item.path("fcstDate").asText().equals(targetDateStr)) continue;
            int hour = Integer.parseInt(item.path("fcstTime").asText().substring(0, 2));
            hourData.putIfAbsent(hour, new WeatherDataPoint(hour));
            WeatherDataPoint point = hourData.get(hour);
            String cat = item.path("category").asText();
            String val = item.path("fcstValue").asText();
            switch (cat) {
                case "TMP" -> point.temperature = parseDoubleSafe(val);
                case "SKY" -> point.sky = val;
                case "PTY" -> point.pty = val;
                case "POP" -> point.pop = parseDoubleSafe(val);
                case "REH" -> point.humidity = parseDoubleSafe(val);
                case "WSD" -> point.windSpeed = parseDoubleSafe(val);
            }
        }
        return interpolateToHourly(hourData);
    }

    private List<WeatherResponseDTO.DailyForecast> parseWeeklyForecastFromItems(JsonNode items) {
        Map<String, DailyWeatherData> dailyData = new TreeMap<>();
        for (JsonNode item : items) {
            String fcstDate = item.path("fcstDate").asText();
            String fcstTime = item.path("fcstTime").asText();
            String category = item.path("category").asText();
            String value = item.path("fcstValue").asText();

            dailyData.putIfAbsent(fcstDate, new DailyWeatherData());
            DailyWeatherData dayData = dailyData.get(fcstDate);
            int hour = Integer.parseInt(fcstTime.substring(0, 2));
            boolean isAm = hour < 12;

            switch (category) {
                case "TMP":
                    double tmp = parseDoubleSafe(value);
                    dayData.temps.add(tmp);
                    if (isAm) dayData.amTemp = tmp;
                    else dayData.pmTemp = tmp;
                    break;
                case "TMN":
                    dayData.minTemp = parseDoubleSafe(value);
                    break;
                case "TMX":
                    dayData.maxTemp = parseDoubleSafe(value);
                    break;
                case "SKY":
                    if (isAm) dayData.amSky = value;
                    else dayData.pmSky = value;
                    break;
                case "PTY":
                    if (isAm) dayData.amPty = value;
                    else dayData.pmPty = value;
                    break;
                case "POP":
                    dayData.pop = Math.max(dayData.pop != null ? dayData.pop : 0, parseDoubleSafe(value));
                    break;
            }
        }

        List<WeatherResponseDTO.DailyForecast> weekly = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            String dateKey = date.format(DateTimeFormatter.BASIC_ISO_DATE);

            if (dailyData.containsKey(dateKey)) {
                DailyWeatherData dayData = dailyData.get(dateKey);

                // [보정] 오전 데이터 누락 시 보완
                if (dayData.minTemp == null && !dayData.temps.isEmpty())
                    dayData.minTemp = Collections.min(dayData.temps);
                if (dayData.maxTemp == null && !dayData.temps.isEmpty())
                    dayData.maxTemp = Collections.max(dayData.temps);
                if (i == 0) {
                    if (dayData.amSky == null) dayData.amSky = dayData.pmSky;
                    if (dayData.amPty == null) dayData.amPty = dayData.pmPty;
                    if (dayData.amTemp == null) dayData.amTemp = dayData.minTemp;
                }
                // 안전값
                if (dayData.maxTemp == null) dayData.maxTemp = 0.0;
                if (dayData.minTemp == null) dayData.minTemp = 0.0;
                if (dayData.amTemp == null) dayData.amTemp = dayData.minTemp;
                if (dayData.pmTemp == null) dayData.pmTemp = dayData.maxTemp;
                if (dayData.amSky == null) dayData.amSky = "1";
                if (dayData.pmSky == null) dayData.pmSky = "1";
                if (dayData.amPty == null) dayData.amPty = "0";
                if (dayData.pmPty == null) dayData.pmPty = "0";

                weekly.add(WeatherResponseDTO.DailyForecast.builder()
                        .date(date.format(DateTimeFormatter.ofPattern("MM/dd")))
                        .dayOfWeek(formatDayOfWeek(date))
                        .maxTemp(dayData.maxTemp).minTemp(dayData.minTemp)
                        .amTemp(dayData.amTemp).pmTemp(dayData.pmTemp)
                        .dayWeather(getWeatherConditionFromCode(dayData.pmSky, dayData.pmPty))
                        .nightWeather(getWeatherConditionFromCode(dayData.amSky, dayData.amPty))
                        .dayIcon(getWeatherIconFromCode(dayData.pmSky, dayData.pmPty, 14))
                        .nightIcon(getWeatherIconFromCode(dayData.amSky, dayData.amPty, 8))
                        .precipitationProbability(dayData.pop).build());
            }
        }
        return weekly;
    }

    // ===== 유틸리티 =====

    private Map<String, String> getGridCoordinatesCached(String regionCode) {
        return GRID_CACHE.computeIfAbsent(regionCode, this::getGridCoordinates);
    }

    private String getRegionNameCached(String regionCode) {
        return REGION_NAME_CACHE.computeIfAbsent(regionCode, this::getRegionNameFromCode);
    }

    private String getBaseTimeForCurrent(LocalDateTime now) {
        int hour = now.getHour(), min = now.getMinute();
        if (min < 40) hour--;
        if (hour < 0) hour = 23;
        return String.format("%02d00", hour);
    }

    private String getBaseTimeForShortTerm(LocalDateTime now) {
        int hour = now.getHour();
        int[] times = {2, 5, 8, 11, 14, 17, 20, 23};
        for (int i = times.length - 1; i >= 0; i--) {
            if (hour >= times[i]) return String.format("%02d00", times[i]);
        }
        return "2300";
    }

    private double interpolate(Double s, Double e, double r) {
        return (s == null ? 20.0 : s) + ((e == null ? 20.0 : e) - (s == null ? 20.0 : s)) * r;
    }

    private double calculateFeelsLike(double t, double h, double w) {
        if (t >= 26) return t + 0.1 * h;
        if (t <= 10) return 13.12 + 0.6215 * t - 11.37 * Math.pow(w, 0.16) + 0.3965 * t * Math.pow(w, 0.16);
        return t;
    }

    private String getWindDirection(String vec) {
        return "북풍";
    }

    private String getWeatherConditionFromCode(String s, String p) {
        if ("1".equals(p)) return "비";
        if (!"0".equals(p)) return "눈/비";
        if ("1".equals(s)) return "맑음";
        if ("3".equals(s)) return "구름많음";
        return "흐림";
    }

    private String getWeatherIconFromCode(String s, String p, int h) {
        boolean d = h >= 6 && h <= 18;
        if (!"0".equals(p)) return "fas fa-umbrella";
        if ("1".equals(s)) return d ? "fas fa-sun" : "fas fa-moon";
        return "fas fa-cloud";
    }

    private String formatHourToKorean(int h) {
        return h < 12 ? "오전 " + (h == 0 ? 12 : h) + "시" : "오후 " + (h == 12 ? 12 : h - 12) + "시";
    }

    private String formatDayOfWeek(LocalDate d) {
        String[] w = {"일", "월", "화", "수", "목", "금", "토"};
        return w[d.getDayOfWeek().getValue() % 7];
    }

    private double parseDoubleSafe(String v) {
        return parseDoubleSafe(v, 0.0);
    }

    private double parseDoubleSafe(String v, double d) {
        try {
            return v == null ? d : Double.parseDouble(v);
        } catch (Exception e) {
            return d;
        }
    }

    private List<WeatherResponseDTO.HourlyForecast> interpolateToHourly(Map<Integer, WeatherDataPoint> map) {
        List<WeatherResponseDTO.HourlyForecast> list = new ArrayList<>();
        List<Integer> hours = new ArrayList<>(map.keySet());
        if (hours.size() < 2) return list;
        for (int i = 0; i < hours.size() - 1; i++) {
            int start = hours.get(i), end = hours.get(i + 1);
            WeatherDataPoint sp = map.get(start), ep = map.get(end);
            for (int h = start; h < end; h++) {
                double r = (double) (h - start) / (end - start);
                list.add(WeatherResponseDTO.HourlyForecast.builder()
                        .time(formatHourToKorean(h))
                        .temperature(interpolate(sp.temperature, ep.temperature, r))
                        .weatherCondition(getWeatherConditionFromCode(r < 0.5 ? sp.sky : ep.sky, r < 0.5 ? sp.pty : ep.pty))
                        .weatherIcon(getWeatherIconFromCode(r < 0.5 ? sp.sky : ep.sky, r < 0.5 ? sp.pty : ep.pty, h))
                        .precipitationProbability(interpolate(sp.pop, ep.pop, r))
                        .humidity(interpolate(sp.humidity, ep.humidity, r))
                        .windSpeed(interpolate(sp.windSpeed, ep.windSpeed, r)).build());
            }
        }
        if (!hours.isEmpty()) {
            int last = hours.get(hours.size() - 1);
            WeatherDataPoint lp = map.get(last);
            list.add(WeatherResponseDTO.HourlyForecast.builder().time(formatHourToKorean(last)).temperature(lp.temperature).weatherCondition(getWeatherConditionFromCode(lp.sky, lp.pty)).weatherIcon(getWeatherIconFromCode(lp.sky, lp.pty, last)).precipitationProbability(lp.pop).humidity(lp.humidity).windSpeed(lp.windSpeed).build());
        }
        return list;
    }

    private WeatherResponseDTO.WeatherSummary createWeatherSummary(List<WeatherResponseDTO.HourlyForecast> h, List<WeatherResponseDTO.HourlyForecast> t, List<WeatherResponseDTO.DailyForecast> w) {
        return WeatherResponseDTO.WeatherSummary.builder().ultraShortSummary("오늘 날씨 정보입니다.").shortSummary("내일 예보입니다.").midSummary("주간 예보입니다.").build();
    }

    // [Fallback] 가상 데이터 생성 시 Warning도 함께 생성
    private WeatherResponseDTO createFallbackWeatherData(String regionCode, boolean liteMode) {
        String regionName = getRegionNameCached(regionCode);
        WeatherResponseDTO response = WeatherResponseDTO.builder()
                .regionName(regionName)
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .current(createDefaultCurrentWeather())
                .isMock(true) // [가상 데이터 표시]
                .warning(new WeatherResponseDTO.WeatherWarning(true, "데이터 지연", "기상청 연결 불안정으로 임시 데이터를 표시합니다.", "caution"))
                .summary(WeatherResponseDTO.WeatherSummary.builder()
                        .ultraShortSummary("⚠️ [가상 데이터] 기상청 API 연결 실패로 임시 데이터를 표시합니다.")
                        .shortSummary("잠시 후 다시 시도해주세요.")
                        .midSummary("API 연결 상태를 확인하세요.")
                        .build())
                .build();
        return liteMode ? WeatherResponseDTO.createLiteVersion(response) : response;
    }

    private WeatherResponseDTO.CurrentWeather createDefaultCurrentWeather() {
        return WeatherResponseDTO.CurrentWeather.builder().temperature(20.0).feelsLike(20.0).humidity(50.0).windSpeed(2.0).weatherCondition("맑음").weatherIcon("fas fa-sun").updateTime(LocalDateTime.now()).build();
    }

    private List<WeatherResponseDTO.HourlyForecast> createDefaultHourlyForecast24h(int d) {
        return new ArrayList<>();
    }

    private List<WeatherResponseDTO.DailyForecast> createDefaultWeeklyForecastWithAmPm() {
        return new ArrayList<>();
    }

    // [상세 격자 좌표 매핑]
    private Map<String, String> getGridCoordinates(String regionCode) {
        Map<String, String> coords = new HashMap<>();
        switch (regionCode) {
            case "1100000000":
                coords.put("nx", "60");
                coords.put("ny", "127");
                break; // 서울
            case "2600000000":
                coords.put("nx", "98");
                coords.put("ny", "76");
                break; // 부산
            case "2700000000":
                coords.put("nx", "89");
                coords.put("ny", "90");
                break; // 대구
            case "2800000000":
                coords.put("nx", "55");
                coords.put("ny", "124");
                break; // 인천
            case "2900000000":
                coords.put("nx", "58");
                coords.put("ny", "74");
                break; // 광주
            case "3000000000":
                coords.put("nx", "67");
                coords.put("ny", "100");
                break; // 대전
            case "3100000000":
                coords.put("nx", "102");
                coords.put("ny", "84");
                break; // 울산
            case "5000000000":
                coords.put("nx", "52");
                coords.put("ny", "38");
                break; // 제주
            case "4100000000":
                coords.put("nx", "60");
                coords.put("ny", "120");
                break; // 경기
            case "4200000000":
                coords.put("nx", "73");
                coords.put("ny", "134");
                break; // 강원
            case "4300000000":
                coords.put("nx", "69");
                coords.put("ny", "107");
                break; // 충북
            case "4400000000":
                coords.put("nx", "68");
                coords.put("ny", "100");
                break; // 충남
            case "4500000000":
                coords.put("nx", "63");
                coords.put("ny", "89");
                break; // 전북
            case "4600000000":
                coords.put("nx", "51");
                coords.put("ny", "67");
                break; // 전남
            case "4700000000":
                coords.put("nx", "87");
                coords.put("ny", "106");
                break; // 경북
            case "4800000000":
                coords.put("nx", "91");
                coords.put("ny", "77");
                break; // 경남
            default:
                coords.put("nx", "60");
                coords.put("ny", "127");
        }
        return coords;
    }

    private String getRegionNameFromCode(String r) {
        Map<String, String> m = new HashMap<>();
        m.put("1100000000", "서울특별시");
        m.put("2600000000", "부산광역시");
        m.put("2700000000", "대구광역시");
        m.put("2800000000", "인천광역시");
        m.put("2900000000", "광주광역시");
        m.put("3000000000", "대전광역시");
        m.put("3100000000", "울산광역시");
        m.put("5000000000", "제주특별자치도");
        m.put("4100000000", "경기도");
        m.put("4200000000", "강원도");
        m.put("4300000000", "충청북도");
        m.put("4400000000", "충청남도");
        m.put("4500000000", "전라북도");
        m.put("4600000000", "전라남도");
        m.put("4700000000", "경상북도");
        m.put("4800000000", "경상남도");
        return m.getOrDefault(r, "대한민국");
    }

    private static class WeatherDataPoint {
        int hour;
        Double temperature = 20.0, pop = 0.0, humidity = 50.0, windSpeed = 2.5;
        String sky = "1", pty = "0";

        WeatherDataPoint(int h) {
            this.hour = h;
        }
    }

    private static class DailyWeatherData {
        Double maxTemp, minTemp, amTemp, pmTemp, pop;
        String amSky = "1", pmSky = "1", amPty = "0", pmPty = "0";
        List<Double> temps = new ArrayList<>();
    }
}