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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiConfig apiConfig;

    // 지역 좌표 캐시
    private static final Map<String, Map<String, String>> GRID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> REGION_NAME_CACHE = new ConcurrentHashMap<>();

    @Cacheable(value = "weatherAllData", key = "#regionCode", unless = "#result == null")
    public WeatherResponseDTO getAllWeatherData(String regionCode) {
        return getAllWeatherData(regionCode, false);
    }

    @Cacheable(value = "weatherAllData", key = "#regionCode + '_' + #liteMode", unless = "#result == null")
    public WeatherResponseDTO getAllWeatherData(String regionCode, boolean liteMode) {
        try {
            log.info("날씨 데이터 통합 조회 (병렬): {} (liteMode: {})", regionCode, liteMode);

            CompletableFuture<WeatherResponseDTO.CurrentWeather> currentFuture =
                    CompletableFuture.supplyAsync(() -> getCurrentWeatherCached(regionCode));

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

            CompletableFuture<WeatherResponseDTO.WeatherSummary> summaryFuture;
            if (liteMode) {
                summaryFuture = CompletableFuture.completedFuture(null);
            } else {
                summaryFuture = CompletableFuture.allOf(todayHourlyFuture, tomorrowHourlyFuture, weeklyFuture)
                        .thenApply(v -> createWeatherSummary(
                                todayHourlyFuture.join(),
                                tomorrowHourlyFuture.join(),
                                weeklyFuture.join()
                        ));
            }

            CompletableFuture.allOf(currentFuture, todayHourlyFuture, tomorrowHourlyFuture, weeklyFuture, summaryFuture).join();

            return WeatherResponseDTO.builder()
                    .regionName(getRegionNameCached(regionCode))
                    .regionCode(regionCode)
                    .currentTime(DateUtil.getCurrentFormattedDateTime())
                    .current(currentFuture.join())
                    .hourly(todayHourlyFuture.join())
                    .tomorrowHourly(tomorrowHourlyFuture.join())
                    .daily(weeklyFuture.join())
                    .summary(summaryFuture.join())
                    .isMock(false) // 정상 데이터
                    .build();

        } catch (Exception e) {
            log.error("날씨 데이터 통합 조회 실패: {}", e.getMessage(), e);
            return createFallbackWeatherData(regionCode, liteMode);
        }
    }

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
            if (dayOffset == 0) return getTodayHourlyForecast(regionCode);
            else return getTomorrowHourlyForecast(regionCode);
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
                        .humidity(humidity).windSpeed(windSpeed).windDirection(getWindDirection(data.get("VEC")))
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
        return WeatherResponseDTO.CurrentWeather.builder().temperature(temp).feelsLike(calculateFeelsLike(temp, humidity, windSpeed)).humidity(humidity).windSpeed(windSpeed).windDirection("서풍").precipitation(0.0).weatherCondition(getWeatherConditionFromCode(sky, pty)).weatherIcon(getWeatherIconFromCode(sky, pty, now.getHour())).updateTime(LocalDateTime.now()).build();
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
                if (dayData.minTemp == null && !dayData.temps.isEmpty())
                    dayData.minTemp = Collections.min(dayData.temps);
                if (dayData.maxTemp == null && !dayData.temps.isEmpty())
                    dayData.maxTemp = Collections.max(dayData.temps);
                if (i == 0) {
                    if (dayData.amSky == null) dayData.amSky = dayData.pmSky;
                    if (dayData.amPty == null) dayData.amPty = dayData.pmPty;
                    if (dayData.amTemp == null) dayData.amTemp = dayData.minTemp;
                }
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

    // [핵심] 가상 데이터 생성 로직 (isMock = true 설정)
    private WeatherResponseDTO createFallbackWeatherData(String regionCode, boolean liteMode) {
        String regionName = getRegionNameCached(regionCode);

        WeatherResponseDTO response = WeatherResponseDTO.builder()
                .regionName(regionName)
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .current(createDefaultCurrentWeather())
                .hourly(liteMode ? null : createDefaultHourlyForecast24h(0))
                .tomorrowHourly(liteMode ? null : createDefaultHourlyForecast24h(1))
                .daily(liteMode ? null : createDefaultWeeklyForecastWithAmPm())
                // [가상 데이터 표시]
                .isMock(true)
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

    private Map<String, String> getGridCoordinates(String regionCode) {
        Map<String, String> coords = new HashMap<>();
        switch (regionCode) {
            case "1100000000":
                coords.put("nx", "60");
                coords.put("ny", "127");
                break;
            case "2600000000":
                coords.put("nx", "98");
                coords.put("ny", "76");
                break;
            case "2700000000":
                coords.put("nx", "89");
                coords.put("ny", "90");
                break;
            case "2800000000":
                coords.put("nx", "55");
                coords.put("ny", "124");
                break;
            case "2900000000":
                coords.put("nx", "58");
                coords.put("ny", "74");
                break;
            case "3000000000":
                coords.put("nx", "67");
                coords.put("ny", "100");
                break;
            case "3100000000":
                coords.put("nx", "102");
                coords.put("ny", "84");
                break;
            case "5000000000":
                coords.put("nx", "52");
                coords.put("ny", "38");
                break;
            case "4100000000":
                coords.put("nx", "60");
                coords.put("ny", "120");
                break;
            case "4200000000":
                coords.put("nx", "73");
                coords.put("ny", "134");
                break;
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
        String amSky, pmSky, amPty, pmPty;
        List<Double> temps = new ArrayList<>();
    }
}