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

    private static final Map<String, Map<String, String>> GRID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> REGION_NAME_CACHE = new ConcurrentHashMap<>();

    @Cacheable(value = "weatherAllData", key = "#regionCode + '_' + #liteMode", unless = "#result == null")
    public WeatherResponseDTO getAllWeatherData(String regionCode, boolean liteMode) {
        try {
            log.info("날씨 데이터 통합 조회: {} (liteMode: {})", regionCode, liteMode);

            CompletableFuture<WeatherResponseDTO.CurrentWeather> currentFuture = CompletableFuture
                    .supplyAsync(() -> getCurrentWeatherCached(regionCode));

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
                        .thenApply(v -> createWeatherSummary(todayHourlyFuture.join(), tomorrowHourlyFuture.join(),
                                weeklyFuture.join()));
            }

            CompletableFuture.allOf(currentFuture, todayHourlyFuture, tomorrowHourlyFuture, weeklyFuture, summaryFuture)
                    .join();

            String regionName = getRegionNameCached(regionCode);
            // [수정] 다중 특보 정보를 리스트로 가져옴
            List<WeatherResponseDTO.WeatherWarning> warnings = getOfficialWarnings(regionName);

            return WeatherResponseDTO.builder()
                    .regionName(regionName)
                    .regionCode(regionCode)
                    .currentTime(DateUtil.getCurrentFormattedDateTime())
                    .current(currentFuture.join())
                    .hourly(todayHourlyFuture.join())
                    .tomorrowHourly(tomorrowHourlyFuture.join())
                    .daily(weeklyFuture.join())
                    .summary(summaryFuture.join())
                    .warnings(warnings) // 리스트 전달
                    .isMock(false)
                    .build();

        } catch (Exception e) {
            log.error("날씨 통합 조회 실패", e);
            return createFallbackWeatherData(regionCode, liteMode);
        }
    }

    private List<WeatherResponseDTO.WeatherWarning> getOfficialWarnings(String regionName) {
        List<WeatherResponseDTO.WeatherWarning> warningList = new ArrayList<>();
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/1360000/WthrWrnInfoService/getWthrWrnMsg")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("stnId", "108").build().toUri();

            ResponseEntity<byte[]> responseBytes = restTemplate.getForEntity(uri, byte[].class);
            String responseBody = responseBytes.getBody() != null
                    ? new String(responseBytes.getBody(), java.nio.charset.StandardCharsets.UTF_8)
                    : null;
            if (responseBody == null || !responseBody.contains("item"))
                return warningList;

            JsonNode root = objectMapper.readTree(responseBody);
            String t6Content = root.path("response").path("body").path("items").path("item").get(0).path("t6")
                    .asText("");

            if (t6Content.isEmpty())
                return warningList;

            // 지역명 변환 (경상북도 -> 경북)
            String shortRegion = getShortRegionName(regionName);
            List<String> keywords = new ArrayList<>();
            keywords.add(shortRegion);

            // 도 단위 풀네임 및 주요 도시 추가
            if (shortRegion.equals("경남"))
                keywords.addAll(List.of("경상남도", "창원", "김해", "진주"));
            if (shortRegion.equals("경북"))
                keywords.addAll(List.of("경상북도", "포항", "구미", "경주", "안동"));
            if (shortRegion.equals("전남"))
                keywords.addAll(List.of("전라남도", "여수", "순천", "목포"));
            if (shortRegion.equals("전북"))
                keywords.addAll(List.of("전라북도", "전북자치도", "전주", "익산", "군산"));
            if (shortRegion.equals("충남"))
                keywords.addAll(List.of("충청남도", "천안", "아산", "서산"));
            if (shortRegion.equals("충북"))
                keywords.addAll(List.of("충청북도", "청주", "충주"));
            if (shortRegion.equals("경기"))
                keywords.addAll(List.of("경기도", "수원", "성남", "고양"));
            if (shortRegion.equals("강원"))
                keywords.addAll(List.of("강원도", "춘천", "원주", "강릉"));
            if (shortRegion.equals("제주"))
                keywords.addAll(List.of("제주도", "제주특별자치도"));
            if (shortRegion.equals("인천"))
                keywords.add("인천광역시");
            if (shortRegion.equals("세종"))
                keywords.add("세종특별자치시");

            String[] sections = t6Content.split("o ");
            for (String section : sections) {
                if (section.trim().isEmpty())
                    continue;

                // ⭐ [핵심 수정] 줄바꿈 문자(\n, \r)를 공백으로 변경하여 정규식 매칭 오류 해결!
                String cleanSection = section.replace("\n", " ").replace("\r", " ").trim();

                boolean isMatch = false;
                for (String kw : keywords) {
                    // 정규식 변경: 문장의 처음이거나 앞부분에 공백/기호가 있는 상태에서 키워드로 시작하는 경우 모두 허용
                    if (cleanSection.matches("(^|.*[\\s,\\(])" + kw + "([\\s,\\(,\\)]|$).*")
                            || cleanSection.contains(kw + ",")) {
                        isMatch = true;
                        break;
                    }
                }

                if (isMatch) {
                    String title = cleanSection.split(":")[0].trim();
                    if (title.contains("해제"))
                        continue;

                    String level = (title.contains("경보") || title.contains("심각")) ? "danger" : "caution";
                    warningList.add(new WeatherResponseDTO.WeatherWarning(true, title, title + " 발효 중", level));
                    log.info("🎯 [특보확인] {} ({}) -> {}", regionName, shortRegion, title);
                }
            }
        } catch (Exception e) {
            log.error("특보 파싱 에러", e);
        }
        return warningList.isEmpty() ? Collections.singletonList(createEmptyWarning()) : warningList;
    }

    // [추가] 지역명을 2글자 표준 약어로 변환하는 헬퍼 메서드
    private String getShortRegionName(String regionName) {
        if (regionName.contains("경상북도") || regionName.contains("경북"))
            return "경북";
        if (regionName.contains("경상남도") || regionName.contains("경남"))
            return "경남";
        if (regionName.contains("충청북도") || regionName.contains("충북"))
            return "충북";
        if (regionName.contains("충청남도") || regionName.contains("충남"))
            return "충남";
        if (regionName.contains("전라북도") || regionName.contains("전북"))
            return "전북";
        if (regionName.contains("전라남도") || regionName.contains("전남"))
            return "전남";
        if (regionName.contains("경기"))
            return "경기";
        if (regionName.contains("강원"))
            return "강원";
        if (regionName.contains("제주"))
            return "제주";
        if (regionName.contains("세종"))
            return "세종";
        if (regionName.contains("서울"))
            return "서울";
        if (regionName.contains("부산"))
            return "부산";
        if (regionName.contains("대구"))
            return "대구";
        if (regionName.contains("인천"))
            return "인천";
        if (regionName.contains("광주"))
            return "광주";
        if (regionName.contains("대전"))
            return "대전";
        if (regionName.contains("울산"))
            return "울산";
        return regionName.substring(0, Math.min(regionName.length(), 2));
    }

    private WeatherResponseDTO.WeatherWarning createEmptyWarning() {
        return new WeatherResponseDTO.WeatherWarning(false, "특보 없음", "현재 발효된 특보가 없습니다.", "safe");
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
            return dayOffset == 0 ? getTodayHourlyForecast(regionCode) : getTomorrowHourlyForecast(regionCode);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Cacheable(value = "weeklyForecast", key = "#regionCode", unless = "#result.isEmpty()")
    public List<WeatherResponseDTO.DailyForecast> getWeeklyForecastCached(String regionCode) {
        try {
            return getShortTermWeeklyForecast(regionCode);
        } catch (Exception e) {
            return new ArrayList<>();
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

            ResponseEntity<byte[]> response = restTemplate.getForEntity(uri, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(responseBody);
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
            String baseTime = getBaseTimeForUltraShortFcst(now);
            Map<String, String> gridCoords = getGridCoordinatesCached(regionCode);

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getUltraSrtFcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1).queryParam("numOfRows", 60).queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate).queryParam("base_time", baseTime)
                    .queryParam("nx", gridCoords.get("nx")).queryParam("ny", gridCoords.get("ny"))
                    .build().toUri();

            ResponseEntity<byte[]> responseBytes = restTemplate.getForEntity(uri, byte[].class);
            String responseBody = responseBytes.getBody() != null
                    ? new String(responseBytes.getBody(), java.nio.charset.StandardCharsets.UTF_8)
                    : "{}";
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                Map<String, String> data = new HashMap<>();
                String targetTime = items.get(0).path("fcstTime").asText();
                for (JsonNode item : items) {
                    if (item.path("fcstTime").asText().equals(targetTime)) {
                        data.put(item.path("category").asText(), item.path("fcstValue").asText());
                    }
                }

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
        Map<String, Object> allData = fetchAllWeatherDataInOneCall(regionCode);
        return allData.containsKey("hourly") ? (List<WeatherResponseDTO.HourlyForecast>) allData.get("hourly")
                : createDefaultHourlyForecast24h(0);
    }

    private List<WeatherResponseDTO.HourlyForecast> getTomorrowHourlyForecast(String regionCode) {
        Map<String, Object> allData = fetchAllWeatherDataInOneCall(regionCode);
        return allData.containsKey("tomorrowHourly")
                ? (List<WeatherResponseDTO.HourlyForecast>) allData.get("tomorrowHourly")
                : createDefaultHourlyForecast24h(1);
    }

    private List<WeatherResponseDTO.DailyForecast> getShortTermWeeklyForecast(String regionCode) {
        Map<String, Object> allData = fetchAllWeatherDataInOneCall(regionCode);
        return allData.containsKey("weekly") ? (List<WeatherResponseDTO.DailyForecast>) allData.get("weekly")
                : createDefaultWeeklyForecastWithAmPm();
    }

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
        return WeatherResponseDTO.CurrentWeather.builder().temperature(temp)
                .feelsLike(calculateFeelsLike(temp, humidity, windSpeed)).humidity(humidity).windSpeed(windSpeed)
                .windDirection("북서풍").precipitation(0.0).weatherCondition(getWeatherConditionFromCode(sky, pty))
                .weatherIcon(getWeatherIconFromCode(sky, pty, now.getHour())).updateTime(LocalDateTime.now()).build();
    }

    private List<WeatherResponseDTO.HourlyForecast> parseHourlyForecastFromItems(JsonNode items, int dayOffset) {
        LocalDate targetDate = LocalDate.now().plusDays(dayOffset);
        String targetDateStr = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        Map<Integer, WeatherDataPoint> hourData = new TreeMap<>();

        for (JsonNode item : items) {
            if (!item.path("fcstDate").asText().equals(targetDateStr))
                continue;
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
                case "TMP" -> {
                    double tmp = parseDoubleSafe(value);
                    dayData.temps.add(tmp);
                    if (isAm)
                        dayData.amTemp = tmp;
                    else
                        dayData.pmTemp = tmp;
                }
                case "TMN" -> dayData.minTemp = parseDoubleSafe(value);
                case "TMX" -> dayData.maxTemp = parseDoubleSafe(value);
                case "SKY" -> {
                    if (isAm)
                        dayData.amSky = value;
                    else
                        dayData.pmSky = value;
                }
                case "PTY" -> {
                    if (isAm)
                        dayData.amPty = value;
                    else
                        dayData.pmPty = value;
                }
                case "POP" -> dayData.pop = Math.max(dayData.pop != null ? dayData.pop : 0, parseDoubleSafe(value));
            }
        }

        List<WeatherResponseDTO.DailyForecast> weekly = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            String dateKey = date.format(DateTimeFormatter.BASIC_ISO_DATE);
            if (dailyData.containsKey(dateKey)) {
                DailyWeatherData d = dailyData.get(dateKey);
                if (d.minTemp == null && !d.temps.isEmpty())
                    d.minTemp = Collections.min(d.temps);
                if (d.maxTemp == null && !d.temps.isEmpty())
                    d.maxTemp = Collections.max(d.temps);
                weekly.add(WeatherResponseDTO.DailyForecast.builder()
                        .date(date.format(DateTimeFormatter.ofPattern("MM/dd")))
                        .dayOfWeek(formatDayOfWeek(date))
                        .maxTemp(d.maxTemp != null ? d.maxTemp : 0.0).minTemp(d.minTemp != null ? d.minTemp : 0.0)
                        .amTemp(d.amTemp != null ? d.amTemp : 0.0).pmTemp(d.pmTemp != null ? d.pmTemp : 0.0)
                        .dayWeather(getWeatherConditionFromCode(d.pmSky, d.pmPty))
                        .nightWeather(getWeatherConditionFromCode(d.amSky, d.amPty))
                        .dayIcon(getWeatherIconFromCode(d.pmSky, d.pmPty, 14))
                        .nightIcon(getWeatherIconFromCode(d.amSky, d.amPty, 8))
                        .precipitationProbability(d.pop != null ? d.pop : 0.0).build());
            }
        }
        return weekly;
    }

    private Map<String, String> getGridCoordinatesCached(String regionCode) {
        return GRID_CACHE.computeIfAbsent(regionCode, this::getGridCoordinates);
    }

    private String getRegionNameCached(String regionCode) {
        return REGION_NAME_CACHE.computeIfAbsent(regionCode, this::getRegionNameFromCode);
    }

    private String getBaseTimeForUltraShortFcst(LocalDateTime now) {
        int h = now.getHour(), m = now.getMinute();
        if (m < 45)
            h--;
        if (h < 0)
            h = 23;
        return String.format("%02d30", h);
    }

    private String getBaseTimeForShortTerm(LocalDateTime now) {
        int h = now.getHour();
        int[] t = { 2, 5, 8, 11, 14, 17, 20, 23 };
        for (int i = t.length - 1; i >= 0; i--)
            if (h >= t[i])
                return String.format("%02d00", t[i]);
        return "2300";
    }

    private double calculateFeelsLike(double t, double h, double w) {
        if (t >= 26)
            return t + 0.1 * h;
        if (t <= 10)
            return 13.12 + 0.6215 * t - 11.37 * Math.pow(w, 0.16) + 0.3965 * t * Math.pow(w, 0.16);
        return t;
    }

    private String getWeatherConditionFromCode(String s, String p) {
        if ("1".equals(p))
            return "비";
        if (!"0".equals(p))
            return "눈/비";
        if ("1".equals(s))
            return "맑음";
        if ("3".equals(s))
            return "구름많음";
        return "흐림";
    }

    private String getWeatherIconFromCode(String s, String p, int h) {
        boolean d = h >= 6 && h <= 18;
        if (!"0".equals(p))
            return "fas fa-umbrella";
        if ("1".equals(s))
            return d ? "fas fa-sun" : "fas fa-moon";
        return "fas fa-cloud";
    }

    private String formatHourToKorean(int h) {
        return h < 12 ? "오전 " + (h == 0 ? 12 : h) + "시" : "오후 " + (h == 12 ? 12 : h - 12) + "시";
    }

    private String formatDayOfWeek(LocalDate d) {
        String[] w = { "일", "월", "화", "수", "목", "금", "토" };
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
        if (hours.size() < 2)
            return list;
        for (int i = 0; i < hours.size() - 1; i++) {
            int start = hours.get(i), end = hours.get(i + 1);
            WeatherDataPoint sp = map.get(start), ep = map.get(end);
            for (int h = start; h < end; h++) {
                double r = (double) (h - start) / (end - start);
                list.add(WeatherResponseDTO.HourlyForecast.builder()
                        .time(formatHourToKorean(h)).temperature(interpolate(sp.temperature, ep.temperature, r))
                        .weatherCondition(
                                getWeatherConditionFromCode(r < 0.5 ? sp.sky : ep.sky, r < 0.5 ? sp.pty : ep.pty))
                        .weatherIcon(getWeatherIconFromCode(r < 0.5 ? sp.sky : ep.sky, r < 0.5 ? sp.pty : ep.pty, h))
                        .precipitationProbability(interpolate(sp.pop, ep.pop, r))
                        .humidity(interpolate(sp.humidity, ep.humidity, r))
                        .windSpeed(interpolate(sp.windSpeed, ep.windSpeed, r)).build());
            }
        }
        return list;
    }

    private double interpolate(Double s, Double e, double r) {
        return (s == null ? 20.0 : s) + ((e == null ? 20.0 : e) - (s == null ? 20.0 : s)) * r;
    }

    private WeatherResponseDTO.WeatherSummary createWeatherSummary(List<WeatherResponseDTO.HourlyForecast> h,
            List<WeatherResponseDTO.HourlyForecast> t, List<WeatherResponseDTO.DailyForecast> w) {

        String ultraShortSummary = "오늘 날씨 정보입니다.";
        if (h != null && !h.isEmpty()) {
            double maxTemp = h.stream().map(WeatherResponseDTO.HourlyForecast::getTemperature)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(20.0);
            double minTemp = h.stream().map(WeatherResponseDTO.HourlyForecast::getTemperature)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).min().orElse(20.0);
            double maxPop = h.stream().map(WeatherResponseDTO.HourlyForecast::getPrecipitationProbability)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0.0);

            if (maxPop >= 60.0) {
                ultraShortSummary = "오늘은 비나 눈 소식이 있습니다. 우산을 챙기세요!";
            } else if (maxTemp >= 30.0) {
                ultraShortSummary = "오늘은 무더운 날씨가 예상됩니다. 온열 질환에 유의하세요.";
            } else if (minTemp <= 0.0) {
                ultraShortSummary = "오늘은 영하권으로 매우 춥습니다. 옷차림을 든든히 하세요!";
            } else if (maxTemp - minTemp >= 10.0) {
                ultraShortSummary = "오늘은 일교차가 큽니다. 겉옷을 챙기시는 것이 좋겠습니다.";
            } else {
                ultraShortSummary = "오늘은 낮 최고 기온 " + Math.round(maxTemp) + "도로 활동하기 무난한 날씨입니다.";
            }
        }

        String shortSummary = "내일 예보입니다.";
        if (t != null && !t.isEmpty()) {
            double maxTemp = t.stream().map(WeatherResponseDTO.HourlyForecast::getTemperature)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(20.0);
            double minTemp = t.stream().map(WeatherResponseDTO.HourlyForecast::getTemperature)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).min().orElse(20.0);
            double maxPop = t.stream().map(WeatherResponseDTO.HourlyForecast::getPrecipitationProbability)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0.0);

            if (maxPop >= 60.0) {
                shortSummary = "내일은 강수 확률이 높아 비나 눈이 올 수 있습니다.";
            } else if (maxTemp - minTemp >= 10.0) {
                shortSummary = "내일은 최저 " + Math.round(minTemp) + "도, 최고 " + Math.round(maxTemp) + "도로 일교차가 큽니다.";
            } else if (minTemp <= 0.0) {
                shortSummary = "내일은 대체로 춥겠으니 따뜻하게 입으세요.";
            } else {
                shortSummary = "내일은 대체로 맑고 포근한 날씨가 예상됩니다.";
            }
        }

        String midSummary = "주간 예보입니다.";
        if (w != null && !w.isEmpty()) {
            boolean hasRain = w.stream()
                    .anyMatch(d -> (d.getPrecipitationProbability() != null && d.getPrecipitationProbability() >= 60.0)
                            ||
                            (d.getDayWeather() != null
                                    && (d.getDayWeather().contains("비") || d.getDayWeather().contains("눈")))
                            ||
                            (d.getNightWeather() != null
                                    && (d.getNightWeather().contains("비") || d.getNightWeather().contains("눈"))));

            if (hasRain) {
                midSummary = "이번 주에는 비나 눈 소식이 포함되어 있습니다. 우산을 미리 준비하세요.";
            } else {
                midSummary = "이번 주는 뚜렷한 비 소식 없이 대체로 맑거나 구름 많은 날씨가 이어집니다.";
            }
        }

        return WeatherResponseDTO.WeatherSummary.builder()
                .ultraShortSummary(ultraShortSummary)
                .shortSummary(shortSummary)
                .midSummary(midSummary)
                .build();
    }

    private WeatherResponseDTO createFallbackWeatherData(String regionCode, boolean liteMode) {
        WeatherResponseDTO response = WeatherResponseDTO.builder()
                .regionName(getRegionNameCached(regionCode)).regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime()).current(createDefaultCurrentWeather())
                .isMock(true)
                .warnings((List<WeatherResponseDTO.WeatherWarning>) new WeatherResponseDTO.WeatherWarning(true,
                        "데이터 지연", "임시 데이터를 표시합니다.", "caution"))
                .build();
        return liteMode ? WeatherResponseDTO.createLiteVersion(response) : response;
    }

    private WeatherResponseDTO.CurrentWeather createDefaultCurrentWeather() {
        return WeatherResponseDTO.CurrentWeather.builder().temperature(20.0).feelsLike(20.0).humidity(50.0)
                .windSpeed(2.0).weatherCondition("맑음").weatherIcon("fas fa-sun").updateTime(LocalDateTime.now())
                .build();
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
            case "3600000000":
                coords.put("nx", "66");
                coords.put("ny", "103");
                break; // 세종
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
        m.put("3600000000", "세종특별자치시"); // ⭐ 세종 추가
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