package me.shinsunyoung.projectweatherly.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.config.ApiConfig;
import me.shinsunyoung.projectweatherly.common.util.DateUtil;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiConfig apiConfig;

    // 중기예보용 고정 URL
    private static final String MID_TERM_API_URL = "http://apis.data.go.kr/1360000/MidFcstInfoService";

    // =================================================================================
    // [1] GPS 및 통합 조회
    // =================================================================================

    public WeatherResponseDTO getCurrentWeatherByGps(double lat, double lng) {
        Map<String, String> grid = convertGpsToGrid(lat, lng);
        String regionName = findRegionNameFromGps(lat, lng);
        return fetchAndBuildWeather(grid.get("nx"), grid.get("ny"), regionName, false);
    }

    @Cacheable(value = "weatherAllData", key = "#regionCode + '_' + #liteMode", unless = "#result == null")
    public WeatherResponseDTO getAllWeatherData(String regionCode, boolean liteMode) {
        Map<String, String> coords = getGridCoordinates(regionCode);
        String regionName = getRegionNameCached(regionCode);
        return fetchAndBuildWeather(coords.get("nx"), coords.get("ny"), regionName, liteMode);
    }

    private WeatherResponseDTO fetchAndBuildWeather(String nx, String ny, String regionName, boolean liteMode) {
        String regionCode = findRegionCodeByName(regionName);

        try {
            // (1) 현재 날씨
            WeatherResponseDTO.CurrentWeather current = getCurrentWeatherFromGrid(nx, ny);

            if (liteMode) {
                return WeatherResponseDTO.builder()
                        .regionName(regionName)
                        .regionCode(regionCode)
                        .currentTime(DateUtil.getCurrentFormattedDateTime())
                        .current(current)
                        .isMock(false)
                        .build();
            }

            // (2) 예보 데이터 조회 (오늘, 내일, 모레, 중기)
            List<WeatherResponseDTO.HourlyForecast> today = new ArrayList<>();
            List<WeatherResponseDTO.HourlyForecast> tomorrow = new ArrayList<>();
            List<WeatherResponseDTO.HourlyForecast> dayAfterTomorrow = new ArrayList<>();
            List<WeatherResponseDTO.DailyForecast> midTerm = new ArrayList<>();

            try {
                // 단기 예보 (1000 rows로 모레까지 커버)
                CompletableFuture<List<WeatherResponseDTO.HourlyForecast>> todayFut =
                        CompletableFuture.supplyAsync(() -> getHourlyForecastFromGrid(nx, ny, 0));
                CompletableFuture<List<WeatherResponseDTO.HourlyForecast>> tomorrowFut =
                        CompletableFuture.supplyAsync(() -> getHourlyForecastFromGrid(nx, ny, 1));
                CompletableFuture<List<WeatherResponseDTO.HourlyForecast>> dayAfterFut =
                        CompletableFuture.supplyAsync(() -> getHourlyForecastFromGrid(nx, ny, 2));

                // 중기 예보 (3일 후 ~ 10일 후)
                CompletableFuture<List<WeatherResponseDTO.DailyForecast>> midTermFut =
                        CompletableFuture.supplyAsync(() -> getWeeklyForecastFromRegion(regionName));

                CompletableFuture.allOf(todayFut, tomorrowFut, dayAfterFut, midTermFut).join();

                today = todayFut.join();
                tomorrow = tomorrowFut.join();
                dayAfterTomorrow = dayAfterFut.join();
                midTerm = midTermFut.join();

            } catch (Exception e) {
                log.error("예보 데이터 로딩 실패: {}", e.getMessage());
            }

            // (3) 데이터 병합 (조작 없이 순수 병합)
            List<WeatherResponseDTO.DailyForecast> fullDailyList = new ArrayList<>();

            // 단기 (Day 0, 1, 2)
            fullDailyList.add(summarizeDaily(today, 0));
            fullDailyList.add(summarizeDaily(tomorrow, 1));
            fullDailyList.add(summarizeDaily(dayAfterTomorrow, 2));

            // 중기 (Day 3 ~ 7)
            if (midTerm != null && !midTerm.isEmpty()) {
                fullDailyList.addAll(midTerm);
            }

            WeatherResponseDTO.WeatherSummary summary = createWeatherSummary(current, today, tomorrow, midTerm);

            return WeatherResponseDTO.builder()
                    .regionName(regionName)
                    .regionCode(regionCode)
                    .currentTime(DateUtil.getCurrentFormattedDateTime())
                    .current(current)
                    .hourly(today)
                    .tomorrowHourly(tomorrow)
                    .daily(fullDailyList)
                    .summary(summary)
                    .isMock(false)
                    .build();

        } catch (Exception e) {
            log.error("치명적 오류 발생: {}", regionName, e);
            return createFallbackWeatherData(regionName, liteMode);
        }
    }

    // =================================================================================
    // [2] API 호출부
    // =================================================================================

    // 1. 현재 날씨
    private WeatherResponseDTO.CurrentWeather getCurrentWeatherFromGrid(String nx, String ny) {
        try {
            String key = URLDecoder.decode(apiConfig.getKmaApiKey(), StandardCharsets.UTF_8);
            LocalDateTime now = LocalDateTime.now();
            if (now.getMinute() < 45) now = now.minusHours(1);

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getUltraSrtNcst")
                    .queryParam("serviceKey", key)
                    .queryParam("pageNo", 1).queryParam("numOfRows", 10).queryParam("dataType", "JSON")
                    .queryParam("base_date", DateUtil.formatDateOnly(now))
                    .queryParam("base_time", String.format("%02d00", now.getHour()))
                    .queryParam("nx", nx).queryParam("ny", ny)
                    .build().toUri();

            String responseStr = restTemplate.getForObject(uri, String.class);
            JsonNode items = objectMapper.readTree(responseStr).path("response").path("body").path("items").path("item");

            Map<String, Double> val = new HashMap<>();
            String pty = "0";

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String cat = item.path("category").asText();
                    double obs = parseDoubleSafe(item.path("obsrValue").asText(), 0.0);
                    val.put(cat, obs);
                    if (cat.equals("PTY")) pty = item.path("obsrValue").asText();
                }
            }

            double temp = val.getOrDefault("T1H", 0.0);
            double wind = val.getOrDefault("WSD", 0.0);
            double humid = val.getOrDefault("REH", 0.0);
            double feelsLike = calculateFeelsLike(temp, humid, wind);

            return WeatherResponseDTO.CurrentWeather.builder()
                    .temperature(temp)
                    .humidity(humid)
                    .windSpeed(wind)
                    .feelsLike(feelsLike)
                    .precipitation(val.getOrDefault("RN1", 0.0))
                    .weatherCondition(getWeatherConditionFromCode("1", pty))
                    .weatherIcon(getWeatherIconFromCode("1", pty, LocalDateTime.now().getHour()))
                    .build();
        } catch (Exception e) {
            log.error("현재 날씨 API 실패", e);
            throw new RuntimeException("Current weather failed");
        }
    }

    // 2. 단기 예보 (시간별)
    private List<WeatherResponseDTO.HourlyForecast> getHourlyForecastFromGrid(String nx, String ny, int dayOffset) {
        List<WeatherResponseDTO.HourlyForecast> result = new ArrayList<>();
        try {
            String key = URLDecoder.decode(apiConfig.getKmaApiKey(), StandardCharsets.UTF_8);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime base = getSafeBaseDateTimeForForecast(now);

            // [중요] 1000 row 요청으로 모레 데이터까지 확보
            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getVilageFcst")
                    .queryParam("serviceKey", key)
                    .queryParam("pageNo", 1).queryParam("numOfRows", 1000).queryParam("dataType", "JSON")
                    .queryParam("base_date", DateUtil.formatDateOnly(base))
                    .queryParam("base_time", String.format("%02d00", base.getHour()))
                    .queryParam("nx", nx).queryParam("ny", ny)
                    .build().toUri();

            String responseStr = restTemplate.getForObject(uri, String.class);
            JsonNode items = objectMapper.readTree(responseStr).path("response").path("body").path("items").path("item");

            String targetDate = DateUtil.formatDateOnly(now.plusDays(dayOffset));
            Map<String, Map<String, String>> hourlyData = new TreeMap<>();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    if (!item.path("fcstDate").asText().equals(targetDate)) continue;
                    hourlyData.computeIfAbsent(item.path("fcstTime").asText(), k -> new HashMap<>())
                            .put(item.path("category").asText(), item.path("fcstValue").asText());
                }
            }

            for (Map.Entry<String, Map<String, String>> entry : hourlyData.entrySet()) {
                String time = entry.getKey();
                Map<String, String> v = entry.getValue();
                result.add(WeatherResponseDTO.HourlyForecast.builder()
                        .time(time.substring(0, 2) + ":00")
                        .temperature(parseDoubleSafe(v.get("TMP"), 0.0))
                        .humidity(parseDoubleSafe(v.get("REH"), 0.0))
                        .weatherIcon(getWeatherIconFromCode(v.getOrDefault("SKY", "1"), v.getOrDefault("PTY", "0"), Integer.parseInt(time.substring(0, 2))))
                        .build());
            }
        } catch (Exception e) {
            log.warn("단기 예보 조회 실패 (offset {}): {}", dayOffset, e.getMessage());
        }
        return result;
    }

    // 3. 중기 예보 (3일후 ~ 7일후)
    private List<WeatherResponseDTO.DailyForecast> getWeeklyForecastFromRegion(String regionName) {
        List<WeatherResponseDTO.DailyForecast> result = new ArrayList<>();

        String landCode = convertRegionToLandCode(regionName);
        String tempCode = convertRegionToTempCode(regionName);

        try {
            String key = URLDecoder.decode(apiConfig.getKmaApiKey(), StandardCharsets.UTF_8);
            LocalDateTime now = LocalDateTime.now();

            String tmFc;
            if (now.getHour() < 6) tmFc = DateUtil.formatDateOnly(now.minusDays(1)) + "1800";
            else if (now.getHour() < 18) tmFc = DateUtil.formatDateOnly(now) + "0600";
            else tmFc = DateUtil.formatDateOnly(now) + "1800";

            URI landUri = UriComponentsBuilder.fromHttpUrl(MID_TERM_API_URL + "/getMidLandFcst")
                    .queryParam("serviceKey", key).queryParam("dataType", "JSON")
                    .queryParam("numOfRows", 10).queryParam("pageNo", 1)
                    .queryParam("regId", landCode).queryParam("tmFc", tmFc)
                    .build().toUri();

            URI tempUri = UriComponentsBuilder.fromHttpUrl(MID_TERM_API_URL + "/getMidTa")
                    .queryParam("serviceKey", key).queryParam("dataType", "JSON")
                    .queryParam("numOfRows", 10).queryParam("pageNo", 1)
                    .queryParam("regId", tempCode).queryParam("tmFc", tmFc)
                    .build().toUri();

            CompletableFuture<JsonNode> landFut = CompletableFuture.supplyAsync(() -> restTemplate.getForObject(landUri, JsonNode.class));
            CompletableFuture<JsonNode> tempFut = CompletableFuture.supplyAsync(() -> restTemplate.getForObject(tempUri, JsonNode.class));

            CompletableFuture.allOf(landFut, tempFut).join();

            JsonNode landItem = landFut.join().path("response").path("body").path("items").path("item").get(0);
            JsonNode tempItem = tempFut.join().path("response").path("body").path("items").path("item").get(0);

            if (landItem != null && tempItem != null) {
                // 3일 후 ~ 7일 후 데이터 파싱
                for (int i = 3; i <= 7; i++) {
                    LocalDateTime date = now.plusDays(i);
                    String dateStr = DateUtil.formatDateOnly(date);
                    String formattedDate = dateStr.substring(4, 6) + "." + dateStr.substring(6, 8);

                    // [핵심] 기본 키값(taMin3)이 0이면 보조 키값(taMin3Low) 사용
                    double min = parseDoubleSafe(tempItem.path("taMin" + i).asText(), -999.0);
                    double max = parseDoubleSafe(tempItem.path("taMax" + i).asText(), -999.0);

                    // 기본값이 0이거나 비정상(-999)일 때, 예비 데이터(Low/High) 확인
                    if (min == -999.0 || (min == 0.0 && max == 0.0)) {
                        // taMin3Low, taMax3High 같은 키를 사용
                        double minLow = parseDoubleSafe(tempItem.path("taMin" + i + "Low").asText(), -999.0);
                        double maxHigh = parseDoubleSafe(tempItem.path("taMax" + i + "High").asText(), -999.0);

                        if (minLow != -999.0) min = minLow;
                        if (maxHigh != -999.0) max = maxHigh;
                    }

                    // 그래도 없으면 0.0으로 남지만, API가 예비 데이터는 99% 줌

                    result.add(WeatherResponseDTO.DailyForecast.builder()
                            .dayOfWeek(date.getDayOfWeek().toString().substring(0, 3))
                            .date(formattedDate)
                            .minTemp(min)
                            .maxTemp(max)
                            .dayIcon(getWeatherIconFromDesc(landItem.path("wf" + i + "Pm").asText()))
                            .nightIcon(getWeatherIconFromDesc(landItem.path("wf" + i + "Am").asText()))
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("중기 예보 로드 실패: {}", e.getMessage());
        }
        return result;
    }

    // =================================================================================
    // [3] 유틸리티 및 헬퍼 메소드
    // =================================================================================

    private double calculateFeelsLike(double temp, double humid, double wind) {
        if (temp <= 10.0) {
            double vKm = wind * 3.6;
            if (vKm < 4.8) return temp;
            return 13.12 + 0.6215 * temp - 11.37 * Math.pow(vKm, 0.16) + 0.3965 * temp * Math.pow(vKm, 0.16);
        } else if (temp >= 25.0) {
            return temp + (humid - 50) * 0.1;
        } else {
            return temp - (wind * 0.7);
        }
    }

    private WeatherResponseDTO.DailyForecast summarizeDaily(List<WeatherResponseDTO.HourlyForecast> hourly, int dayOffset) {
        LocalDateTime date = LocalDateTime.now().plusDays(dayOffset);
        String dateStr = DateUtil.formatDateOnly(date);
        String formattedDate = dateStr.substring(4, 6) + "." + dateStr.substring(6, 8);

        if (hourly == null || hourly.isEmpty()) {
            return WeatherResponseDTO.DailyForecast.builder()
                    .dayOfWeek(date.getDayOfWeek().toString().substring(0, 3))
                    .date(formattedDate)
                    .maxTemp(0.0).minTemp(0.0).build();
        }
        double max = hourly.stream().mapToDouble(WeatherResponseDTO.HourlyForecast::getTemperature).max().orElse(0.0);
        double min = hourly.stream().mapToDouble(WeatherResponseDTO.HourlyForecast::getTemperature).min().orElse(0.0);
        String icon = hourly.stream()
                .filter(h -> h.getTime().startsWith("12") || h.getTime().startsWith("14"))
                .findFirst().map(WeatherResponseDTO.HourlyForecast::getWeatherIcon).orElse("fas fa-sun");

        return WeatherResponseDTO.DailyForecast.builder()
                .dayOfWeek(date.getDayOfWeek().toString().substring(0, 3))
                .date(formattedDate)
                .maxTemp(max).minTemp(min)
                .dayIcon(icon).nightIcon(icon).build();
    }

    private Map<String, String> convertGpsToGrid(double lat, double lng) {
        Map<String, String> grid = new HashMap<>();
        if (lat >= 37.1 && lat <= 37.6 && lng >= 131.5 && lng <= 132.5) {
            grid.put("nx", "144");
            grid.put("ny", "123");
            return grid;
        }
        if (lat >= 33.0 && lat <= 34.5 && lng >= 126.0 && lng <= 127.0) {
            grid.put("nx", "52");
            grid.put("ny", "38");
            return grid;
        }
        double RE = 6371.00877, GRID = 5.0, SLAT1 = 30.0, SLAT2 = 60.0, OLON = 126.0, OLAT = 38.0, XO = 43, YO = 136;
        double DEGRAD = Math.PI / 180.0;
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD, slat2 = SLAT2 * DEGRAD, olon = OLON * DEGRAD, olat = OLAT * DEGRAD;
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);
        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lng * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;
        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        grid.put("nx", String.valueOf(nx));
        grid.put("ny", String.valueOf(ny));
        return grid;
    }

    private Map<String, String> getGridCoordinates(String regionCode) {
        Map<String, String> c = new HashMap<>();
        if (regionCode.startsWith("4794")) {
            c.put("nx", "144");
            c.put("ny", "123");
        } else if (regionCode.startsWith("50")) {
            c.put("nx", "52");
            c.put("ny", "38");
        } else if (regionCode.startsWith("26")) {
            c.put("nx", "98");
            c.put("ny", "76");
        } else if (regionCode.startsWith("27")) {
            c.put("nx", "89");
            c.put("ny", "90");
        } else if (regionCode.startsWith("29")) {
            c.put("nx", "58");
            c.put("ny", "74");
        } else if (regionCode.startsWith("30")) {
            c.put("nx", "67");
            c.put("ny", "100");
        } else {
            c.put("nx", "60");
            c.put("ny", "127");
        }
        return c;
    }

    private String convertRegionToLandCode(String name) {
        if (name == null) return "11B00000";
        if (name.contains("강원")) {
            if (name.contains("영동")) return "11D20000";
            return "11D10000";
        }
        if (name.contains("대전") || name.contains("세종") || name.contains("충남")) return "11C20000";
        if (name.contains("충북")) return "11C10000";
        if (name.contains("광주") || name.contains("전남")) return "11F20000";
        if (name.contains("전북")) return "11F10000";
        if (name.contains("대구") || name.contains("경북")) return "11H10000";
        if (name.contains("부산") || name.contains("울산") || name.contains("경남")) return "11H20000";
        if (name.contains("제주")) return "11G00000";
        return "11B00000";
    }

    private String convertRegionToTempCode(String name) {
        if (name == null) return "11B10101";
        if (name.contains("인천")) return "11B20201";
        if (name.contains("수원") || name.contains("경기")) return "11B20601";
        if (name.contains("강릉")) return "11D20501";
        if (name.contains("춘천") || name.contains("강원")) return "11D10301";
        if (name.contains("대전")) return "11C20401";
        if (name.contains("청주") || name.contains("충북")) return "11C10301";
        if (name.contains("광주")) return "11F20501";
        if (name.contains("전주") || name.contains("전북")) return "11F10201";
        if (name.contains("대구")) return "11H10701";
        if (name.contains("부산")) return "11H20201";
        if (name.contains("울산")) return "11H20101";
        if (name.contains("제주")) return "11G00201";
        return "11B10101";
    }

    private String findRegionCodeByName(String name) {
        if (name == null) return "1100000000";
        if (name.contains("제주")) return "5000000000";
        if (name.contains("독도")) return "4794000000";
        if (name.contains("부산")) return "2600000000";
        if (name.contains("대구")) return "2700000000";
        if (name.contains("광주")) return "2900000000";
        if (name.contains("대전")) return "3000000000";
        return "1100000000";
    }

    private String findRegionNameFromGps(double lat, double lng) {
        if (lat >= 37.1 && lat <= 37.6 && lng >= 131.5 && lng <= 132.5) return "독도";
        if (lat >= 33.0 && lat <= 34.5 && lng >= 126.0 && lng <= 127.0) return "제주특별자치도";
        return "현 위치";
    }

    private String getRegionNameCached(String r) {
        if (r.startsWith("50")) return "제주특별자치도";
        if (r.startsWith("4794")) return "독도";
        if (r.startsWith("26")) return "부산광역시";
        if (r.startsWith("27")) return "대구광역시";
        if (r.startsWith("29")) return "광주광역시";
        if (r.startsWith("30")) return "대전광역시";
        return "서울특별시";
    }

    private WeatherResponseDTO createFallbackWeatherData(String r, boolean l) {
        return WeatherResponseDTO.builder().regionName(r).isMock(true).build();
    }

    private double parseDoubleSafe(String v, double d) {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return d;
        }
    }

    private String getWeatherConditionFromCode(String s, String p) {
        if ("1".equals(p)) return "비";
        if (!"0".equals(p)) return "눈/비";
        if ("1".equals(s)) return "맑음";
        return "흐림";
    }

    private String getWeatherIconFromCode(String s, String p, int h) {
        boolean d = h >= 6 && h <= 18;
        if (!"0".equals(p)) return "fas fa-umbrella";
        if ("1".equals(s)) return d ? "fas fa-sun" : "fas fa-moon";
        return "fas fa-cloud";
    }

    private String getWeatherIconFromDesc(String d) {
        if (d.contains("맑음")) return "fas fa-sun";
        if (d.contains("비")) return "fas fa-umbrella";
        return "fas fa-cloud";
    }

    private WeatherResponseDTO.WeatherSummary createWeatherSummary(WeatherResponseDTO.CurrentWeather c, List<WeatherResponseDTO.HourlyForecast> t, List<WeatherResponseDTO.HourlyForecast> tm, List<WeatherResponseDTO.DailyForecast> m) {
        String u = "현재 기온 " + Math.round(c.getTemperature()) + "도";
        double tMax = t.stream().mapToDouble(WeatherResponseDTO.HourlyForecast::getTemperature).max().orElse(0.0);
        double tmMax = tm.stream().mapToDouble(WeatherResponseDTO.HourlyForecast::getTemperature).max().orElse(0.0);
        String s = "오늘 최고 " + Math.round(tMax) + "도, 내일 최고 " + Math.round(tmMax) + "도";
        String mTxt = (m != null && !m.isEmpty()) ? "주간 기온 " + Math.round(m.get(0).getMinTemp()) + "~" + Math.round(m.get(0).getMaxTemp()) + "도" : "정보 없음";
        return WeatherResponseDTO.WeatherSummary.builder().ultraShortSummary(u).shortSummary(s).midSummary(mTxt).build();
    }

    private LocalDateTime getSafeBaseDateTimeForForecast(LocalDateTime now) {
        int h = now.getHour();
        if (h < 2 || (h == 2 && now.getMinute() < 10)) return now.minusDays(1).withHour(23).withMinute(0);
        int[] times = {2, 5, 8, 11, 14, 17, 20, 23};
        for (int i = times.length - 1; i >= 0; i--) if (h >= times[i]) return now.withHour(times[i]).withMinute(0);
        return now.withHour(2).withMinute(0);
    }
}