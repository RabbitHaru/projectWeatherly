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

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiConfig apiConfig;

    /**
     * 모든 날씨 데이터 통합 조회 (초단기 + 단기)
     */
    @Cacheable(value = "weatherData", key = "#regionCode")
    public WeatherResponseDTO getAllWeatherData(String regionCode) {
        try {
            log.info("날씨 데이터 통합 조회: {}", regionCode);

            // 1. 현재 날씨 (초단기실황)
            WeatherResponseDTO.CurrentWeather current = getCurrentWeatherFromUltraShort(regionCode);

            // 2. 오늘 시간별 예보 (초단기 우선, 없으면 단기)
            List<WeatherResponseDTO.HourlyForecast> todayHourly = getTodayHourlyForecast(regionCode);

            // 3. 내일 시간별 예보 (단기예보에서)
            List<WeatherResponseDTO.HourlyForecast> tomorrowHourly = getTomorrowHourlyForecast(regionCode);

            // 4. 주간 예보 (7일, 오전/오후 구분) - 단기 우선, 없으면 중기
            List<WeatherResponseDTO.DailyForecast> weekly = getWeeklyForecastWithAmPm(regionCode);

            // 5. 요약 정보 생성
            WeatherResponseDTO.WeatherSummary summary = createWeatherSummary(todayHourly, tomorrowHourly, weekly);

            return WeatherResponseDTO.builder()
                    .regionName(getRegionNameFromCode(regionCode))
                    .regionCode(regionCode)
                    .currentTime(DateUtil.getCurrentFormattedDateTime())
                    .current(current)
                    .hourly(todayHourly)
                    .tomorrowHourly(tomorrowHourly)
                    .daily(weekly)
                    .summary(summary)
                    .build();

        } catch (Exception e) {
            log.error("날씨 데이터 통합 조회 실패: {}", e.getMessage(), e);
            return createFallbackWeatherData(regionCode);
        }
    }

    /**
     * 현재 날씨 정보 조회 (초단기실황)
     */
    private WeatherResponseDTO.CurrentWeather getCurrentWeatherFromUltraShort(String regionCode) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String baseDate = DateUtil.formatDateOnly(now);
            String baseTime = getBaseTimeForCurrent(now);

            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getUltraSrtNcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 10)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUri();

            log.info("현재 날씨 API 호출: {}", regionCode);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                Map<String, String> data = new HashMap<>();
                for (JsonNode item : items) {
                    String category = item.path("category").asText();
                    String value = item.path("obsrValue").asText();
                    data.put(category, value);
                }

                double temp = parseDoubleSafe(data.get("T1H"), 20.0);
                double humidity = parseDoubleSafe(data.get("REH"), 50.0);
                double windSpeed = parseDoubleSafe(data.get("WSD"), 2.5);
                String ptyCode = data.getOrDefault("PTY", "0");
                String skyCode = data.getOrDefault("SKY", "1");

                return WeatherResponseDTO.CurrentWeather.builder()
                        .temperature(temp)
                        .feelsLike(calculateFeelsLike(temp, humidity, windSpeed))
                        .humidity(humidity)
                        .windSpeed(windSpeed)
                        .windDirection(getWindDirection(data.get("VEC")))
                        .precipitation(parseDoubleSafe(data.get("RN1"), 0.0))
                        .weatherCondition(getWeatherConditionFromCode(skyCode, ptyCode))
                        .weatherIcon(getWeatherIconFromCode(skyCode, ptyCode, now.getHour()))
                        .updateTime(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.error("초단기 현재 날씨 조회 실패: {}", e.getMessage());
        }
        return createDefaultCurrentWeather();
    }

    /**
     * 오늘 시간별 예보 조회 (초단기 우선, 없으면 단기)
     */
    private List<WeatherResponseDTO.HourlyForecast> getTodayHourlyForecast(String regionCode) {
        try {
            // 1. 초단기예보 시도 (6시간)
            List<WeatherResponseDTO.HourlyForecast> ultraShort = getUltraShortForecastDetail(regionCode);

            if (!ultraShort.isEmpty()) {
                log.info("초단기예보 사용 ({}개 항목)", ultraShort.size());
                // 초단기 데이터가 있으면 24시간으로 확장
                return extendTo24Hours(ultraShort);
            }

            // 2. 초단기 없으면 단기예보에서 오늘 데이터 가져오기
            log.info("초단기예보 없음, 단기예보 사용");
            List<WeatherResponseDTO.HourlyForecast> shortTerm = getHourlyFromShortTerm(regionCode, 0);

            if (!shortTerm.isEmpty()) {
                return shortTerm;
            }

        } catch (Exception e) {
            log.error("오늘 시간별 예보 조회 실패: {}", e.getMessage());
        }

        // 3. 모두 실패하면 기본 데이터
        return createDefaultHourlyForecast24h(0);
    }

    /**
     * 내일 시간별 예보 조회 (단기예보)
     */
    private List<WeatherResponseDTO.HourlyForecast> getTomorrowHourlyForecast(String regionCode) {
        try {
            List<WeatherResponseDTO.HourlyForecast> tomorrowData = getHourlyFromShortTerm(regionCode, 1);

            if (!tomorrowData.isEmpty()) {
                return tomorrowData;
            }

        } catch (Exception e) {
            log.error("내일 시간별 예보 조회 실패: {}", e.getMessage());
        }

        return createDefaultHourlyForecast24h(1);
    }

    /**
     * 주간 예보 조회 (단기 우선, 없으면 중기)
     */
    private List<WeatherResponseDTO.DailyForecast> getWeeklyForecastWithAmPm(String regionCode) {
        try {
            // 1. 단기예보에서 주간 데이터 가져오기
            List<WeatherResponseDTO.DailyForecast> shortTermWeekly = getShortTermWeeklyForecast(regionCode);

            if (!shortTermWeekly.isEmpty() && shortTermWeekly.size() >= 3) {
                log.info("단기예보 사용 ({}일)", shortTermWeekly.size());
                return shortTermWeekly;
            }

            // 2. 단기 없으면 중기예보 시도
            log.info("단기예보 부족, 중기예보 시도");
            List<WeatherResponseDTO.DailyForecast> midTermWeekly = getMidTermWeeklyForecast(regionCode);

            if (!midTermWeekly.isEmpty()) {
                return midTermWeekly;
            }

        } catch (Exception e) {
            log.error("주간 예보 조회 실패: {}", e.getMessage());
        }

        return createDefaultWeeklyForecastWithAmPm();
    }

    /**
     * 초단기예보 상세 조회 (6시간)
     */
    private List<WeatherResponseDTO.HourlyForecast> getUltraShortForecastDetail(String regionCode) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String baseDate = DateUtil.formatDateOnly(now);
            String baseTime = getBaseTimeForUltraShort(now);

            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getUltraSrtFcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                List<WeatherResponseDTO.HourlyForecast> hourlyList = new ArrayList<>();

                // 시간별 데이터 그룹화
                Map<String, Map<String, String>> hourlyData = new TreeMap<>();

                for (JsonNode item : items) {
                    String fcstTime = item.path("fcstTime").asText();
                    String category = item.path("category").asText();
                    String fcstValue = item.path("fcstValue").asText();

                    if (!hourlyData.containsKey(fcstTime)) {
                        hourlyData.put(fcstTime, new HashMap<>());
                    }
                    hourlyData.get(fcstTime).put(category, fcstValue);
                }

                // 6시간 데이터 생성
                int count = 0;
                for (Map.Entry<String, Map<String, String>> entry : hourlyData.entrySet()) {
                    if (count >= 6) break;

                    String timeKey = entry.getKey();
                    int hour = Integer.parseInt(timeKey.substring(0, 2));
                    Map<String, String> data = entry.getValue();

                    double temp = parseDoubleSafe(data.get("T1H"), 20.0);
                    String sky = data.getOrDefault("SKY", "1");
                    String pty = data.getOrDefault("PTY", "0");

                    WeatherResponseDTO.HourlyForecast forecast = WeatherResponseDTO.HourlyForecast.builder()
                            .time(formatHourToKorean(hour))
                            .temperature(temp)
                            .weatherCondition(getWeatherConditionFromCode(sky, pty))
                            .weatherIcon(getWeatherIconFromCode(sky, pty, hour))
                            .precipitationProbability(0.0) // 초단기에는 강수확률 없음
                            .humidity(parseDoubleSafe(data.get("REH"), 50.0))
                            .windSpeed(parseDoubleSafe(data.get("WSD"), 2.5))
                            .build();

                    hourlyList.add(forecast);
                    count++;
                }

                return hourlyList;
            }
        } catch (Exception e) {
            log.warn("초단기예보 조회 실패: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 단기예보에서 시간별 데이터 생성 (24시간)
     */
    private List<WeatherResponseDTO.HourlyForecast> getHourlyFromShortTerm(String regionCode, int daysFromNow) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String baseDate = DateUtil.formatDateOnly(now);
            String baseTime = getBaseTimeForShortTerm(now);

            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getVilageFcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1000)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                // 대상 날짜
                LocalDate targetDate = LocalDate.now().plusDays(daysFromNow);
                String targetDateStr = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE);

                // 3시간 간격 데이터 수집
                Map<Integer, WeatherDataPoint> threeHourData = new TreeMap<>();

                for (JsonNode item : items) {
                    String fcstDate = item.path("fcstDate").asText();
                    String fcstTime = item.path("fcstTime").asText();
                    String category = item.path("category").asText();
                    String fcstValue = item.path("fcstValue").asText();

                    // 대상 날짜 데이터만
                    if (!fcstDate.equals(targetDateStr)) continue;

                    int hour = Integer.parseInt(fcstTime.substring(0, 2));

                    if (!threeHourData.containsKey(hour)) {
                        threeHourData.put(hour, new WeatherDataPoint(hour));
                    }

                    WeatherDataPoint point = threeHourData.get(hour);

                    switch (category) {
                        case "TMP":
                            point.temperature = parseDoubleSafe(fcstValue);
                            break;
                        case "SKY":
                            point.sky = fcstValue;
                            break;
                        case "PTY":
                            point.pty = fcstValue;
                            break;
                        case "POP":
                            point.pop = parseDoubleSafe(fcstValue);
                            break;
                        case "REH":
                            point.humidity = parseDoubleSafe(fcstValue);
                            break;
                        case "WSD":
                            point.windSpeed = parseDoubleSafe(fcstValue);
                            break;
                    }
                }

                // 1시간 간격으로 보간
                return interpolateToHourly(threeHourData);
            }
        } catch (Exception e) {
            log.error("단기예보 시간별 파싱 실패: {}", e.getMessage());
        }

        return createDefaultHourlyForecast24h(daysFromNow);
    }

    /**
     * 단기예보에서 주간 데이터 가져오기
     */
    private List<WeatherResponseDTO.DailyForecast> getShortTermWeeklyForecast(String regionCode) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String baseDate = DateUtil.formatDateOnly(now);
            String baseTime = getBaseTimeForShortTerm(now);

            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            URI uri = UriComponentsBuilder.fromHttpUrl(apiConfig.getKmaApiUrl() + "/getVilageFcst")
                    .queryParam("serviceKey", apiConfig.getKmaApiKey())
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1000)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                // 날짜별 데이터 수집
                Map<String, DailyWeatherData> dailyData = new TreeMap<>();

                for (JsonNode item : items) {
                    String fcstDate = item.path("fcstDate").asText();
                    String fcstTime = item.path("fcstTime").asText();
                    String category = item.path("category").asText();
                    String fcstValue = item.path("fcstValue").asText();

                    int hour = Integer.parseInt(fcstTime.substring(0, 2));
                    boolean isAm = hour < 12;

                    if (!dailyData.containsKey(fcstDate)) {
                        dailyData.put(fcstDate, new DailyWeatherData());
                    }

                    DailyWeatherData dayData = dailyData.get(fcstDate);

                    if (isAm) {
                        switch (category) {
                            case "TMP":
                                dayData.amTemp = parseDoubleSafe(fcstValue);
                                break;
                            case "SKY":
                                dayData.amSky = fcstValue;
                                break;
                            case "PTY":
                                dayData.amPty = fcstValue;
                                break;
                            case "TMN":
                                dayData.minTemp = parseDoubleSafe(fcstValue);
                                break;
                        }
                    } else {
                        switch (category) {
                            case "TMP":
                                dayData.pmTemp = parseDoubleSafe(fcstValue);
                                break;
                            case "SKY":
                                dayData.pmSky = fcstValue;
                                break;
                            case "PTY":
                                dayData.pmPty = fcstValue;
                                break;
                            case "TMX":
                                dayData.maxTemp = parseDoubleSafe(fcstValue);
                                break;
                            case "POP":
                                dayData.pop = parseDoubleSafe(fcstValue);
                                break;
                        }
                    }
                }

                // 7일간 데이터 생성
                List<WeatherResponseDTO.DailyForecast> weekly = new ArrayList<>();
                LocalDate today = LocalDate.now();

                for (int i = 0; i < 7; i++) {
                    LocalDate date = today.plusDays(i);
                    String dateKey = date.format(DateTimeFormatter.BASIC_ISO_DATE);

                    if (dailyData.containsKey(dateKey)) {
                        DailyWeatherData dayData = dailyData.get(dateKey);

                        // 기본값 처리
                        if (dayData.maxTemp == null) dayData.maxTemp = 24.0 - i;
                        if (dayData.minTemp == null) dayData.minTemp = 16.0 - i;
                        if (dayData.amTemp == null) dayData.amTemp = dayData.minTemp + 2.0;
                        if (dayData.pmTemp == null) dayData.pmTemp = dayData.maxTemp - 2.0;
                        if (dayData.pop == null) dayData.pop = 0.0;

                        String dayOfWeek = formatDayOfWeek(date);

                        WeatherResponseDTO.DailyForecast forecast = WeatherResponseDTO.DailyForecast.builder()
                                .date(date.format(DateTimeFormatter.ofPattern("MM/dd")))
                                .dayOfWeek(dayOfWeek)
                                .maxTemp(dayData.maxTemp)
                                .minTemp(dayData.minTemp)
                                .amTemp(dayData.amTemp)
                                .pmTemp(dayData.pmTemp)
                                .dayWeather(getWeatherConditionFromCode(dayData.pmSky, dayData.pmPty))
                                .nightWeather(getWeatherConditionFromCode(dayData.amSky, dayData.amPty))
                                .dayIcon(getWeatherIconFromCode(dayData.pmSky, dayData.pmPty, 14))
                                .nightIcon(getWeatherIconFromCode(dayData.amSky, dayData.amPty, 8))
                                .precipitationProbability(dayData.pop)
                                .build();

                        weekly.add(forecast);
                    }
                }

                return weekly;
            }
        } catch (Exception e) {
            log.error("단기 주간 예보 파싱 실패: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 중기예보에서 주간 데이터 가져오기 (폴백용)
     */
    private List<WeatherResponseDTO.DailyForecast> getMidTermWeeklyForecast(String regionCode) {
        // 중기예보 API 구현 (현재는 기본 데이터 반환)
        return createDefaultWeeklyForecastWithAmPm();
    }

    /**
     * 24시간 데이터로 확장
     */
    private List<WeatherResponseDTO.HourlyForecast> extendTo24Hours(List<WeatherResponseDTO.HourlyForecast> baseData) {
        List<WeatherResponseDTO.HourlyForecast> extended = new ArrayList<>();

        if (baseData.isEmpty()) return extended;

        int currentHour = LocalDateTime.now().getHour();

        for (int i = 0; i < 24; i++) {
            int targetHour = (currentHour + i) % 24;
            int baseIndex = Math.min(i, baseData.size() - 1);
            WeatherResponseDTO.HourlyForecast base = baseData.get(baseIndex);

            WeatherResponseDTO.HourlyForecast extendedForecast = WeatherResponseDTO.HourlyForecast.builder()
                    .time(formatHourToKorean(targetHour))
                    .temperature(base.getTemperature() + (i * 0.1))
                    .weatherCondition(base.getWeatherCondition())
                    .weatherIcon(base.getWeatherIcon())
                    .precipitationProbability(base.getPrecipitationProbability())
                    .humidity(base.getHumidity())
                    .windSpeed(base.getWindSpeed())
                    .build();

            extended.add(extendedForecast);
        }

        return extended;
    }

    /**
     * 3시간 데이터를 1시간 간격으로 보간
     */
    private List<WeatherResponseDTO.HourlyForecast> interpolateToHourly(Map<Integer, WeatherDataPoint> threeHourData) {
        List<WeatherResponseDTO.HourlyForecast> hourly = new ArrayList<>();
        List<Integer> hours = new ArrayList<>(threeHourData.keySet());

        if (hours.size() < 2) {
            return hourly;
        }

        for (int i = 0; i < hours.size() - 1; i++) {
            int startHour = hours.get(i);
            int endHour = hours.get(i + 1);
            WeatherDataPoint startPoint = threeHourData.get(startHour);
            WeatherDataPoint endPoint = threeHourData.get(endHour);

            for (int h = startHour; h < endHour; h++) {
                double ratio = (double)(h - startHour) / (endHour - startHour);

                double temp = interpolate(startPoint.temperature, endPoint.temperature, ratio);
                double pop = interpolate(startPoint.pop, endPoint.pop, ratio);
                double humidity = interpolate(startPoint.humidity, endPoint.humidity, ratio);
                double windSpeed = interpolate(startPoint.windSpeed, endPoint.windSpeed, ratio);
                String sky = (ratio < 0.5) ? startPoint.sky : endPoint.sky;
                String pty = (ratio < 0.5) ? startPoint.pty : endPoint.pty;

                WeatherResponseDTO.HourlyForecast forecast = WeatherResponseDTO.HourlyForecast.builder()
                        .time(formatHourToKorean(h))
                        .temperature(temp)
                        .weatherCondition(getWeatherConditionFromCode(sky, pty))
                        .weatherIcon(getWeatherIconFromCode(sky, pty, h))
                        .precipitationProbability(pop)
                        .humidity(humidity)
                        .windSpeed(windSpeed)
                        .build();

                hourly.add(forecast);
            }
        }

        if (!hours.isEmpty()) {
            int lastHour = hours.get(hours.size() - 1);
            WeatherDataPoint lastPoint = threeHourData.get(lastHour);

            WeatherResponseDTO.HourlyForecast forecast = WeatherResponseDTO.HourlyForecast.builder()
                    .time(formatHourToKorean(lastHour))
                    .temperature(lastPoint.temperature)
                    .weatherCondition(getWeatherConditionFromCode(lastPoint.sky, lastPoint.pty))
                    .weatherIcon(getWeatherIconFromCode(lastPoint.sky, lastPoint.pty, lastHour))
                    .precipitationProbability(lastPoint.pop)
                    .humidity(lastPoint.humidity)
                    .windSpeed(lastPoint.windSpeed)
                    .build();

            hourly.add(forecast);
        }

        return hourly;
    }

    // ===== 유틸리티 메서드 =====

    private String getBaseTimeForCurrent(LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();

        if (minute < 40) {
            hour = hour - 1;
            if (hour < 0) hour = 23;
        }

        return String.format("%02d00", hour);
    }

    private String getBaseTimeForUltraShort(LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();

        if (minute < 45) {
            hour = hour - 1;
            if (hour < 0) hour = 23;
        }

        return String.format("%02d30", hour);
    }

    private String getBaseTimeForShortTerm(LocalDateTime now) {
        int hour = now.getHour();
        int[] forecastTimes = {2, 5, 8, 11, 14, 17, 20, 23};

        for (int i = forecastTimes.length - 1; i >= 0; i--) {
            if (hour >= forecastTimes[i]) {
                return String.format("%02d00", forecastTimes[i]);
            }
        }

        return "2300";
    }

    private double interpolate(Double start, Double end, double ratio) {
        if (start == null) start = 20.0;
        if (end == null) end = 20.0;
        return start + (end - start) * ratio;
    }

    private double calculateFeelsLike(double temp, double humidity, double windSpeed) {
        // 체감온도 계산 (간단한 버전)
        if (temp >= 26.0) {
            // 더위 체감온도
            return temp + 0.1 * humidity;
        } else if (temp <= 10.0) {
            // 추위 체감온도
            return 13.12 + 0.6215 * temp - 11.37 * Math.pow(windSpeed, 0.16)
                    + 0.3965 * temp * Math.pow(windSpeed, 0.16);
        }
        return temp;
    }

    private String getWindDirection(String vec) {
        if (vec == null) return "북풍";
        try {
            double degree = Double.parseDouble(vec);
            if (degree >= 337.5 || degree < 22.5) return "북풍";
            if (degree >= 22.5 && degree < 67.5) return "북동풍";
            if (degree >= 67.5 && degree < 112.5) return "동풍";
            if (degree >= 112.5 && degree < 157.5) return "남동풍";
            if (degree >= 157.5 && degree < 202.5) return "남풍";
            if (degree >= 202.5 && degree < 247.5) return "남서풍";
            if (degree >= 247.5 && degree < 292.5) return "서풍";
            return "북서풍";
        } catch (NumberFormatException e) {
            return "북풍";
        }
    }

    private String getWeatherConditionFromCode(String sky, String pty) {
        if ("1".equals(pty)) return "비";
        if ("2".equals(pty)) return "비/눈";
        if ("3".equals(pty)) return "눈";
        if ("4".equals(pty)) return "소나기";
        if ("5".equals(pty)) return "빗방울";
        if ("6".equals(pty)) return "빗방울눈날림";
        if ("7".equals(pty)) return "눈날림";

        if ("1".equals(sky)) return "맑음";
        if ("3".equals(sky)) return "구름많음";
        if ("4".equals(sky)) return "흐림";

        return "보통";
    }

    private String getWeatherIconFromCode(String sky, String pty, int hour) {
        boolean isDay = hour >= 6 && hour <= 18;

        if ("1".equals(pty)) return "fas fa-cloud-rain";
        if ("2".equals(pty)) return "fas fa-cloud-meatball";
        if ("3".equals(pty)) return "fas fa-snowflake";
        if ("4".equals(pty)) return "fas fa-poo-storm";
        if ("5".equals(pty)) return "fas fa-cloud-rain";
        if ("6".equals(pty)) return "fas fa-cloud-meatball";
        if ("7".equals(pty)) return "fas fa-snowflake";

        if ("1".equals(sky)) {
            return isDay ? "fas fa-sun" : "fas fa-moon";
        } else if ("3".equals(sky)) {
            return isDay ? "fas fa-cloud-sun" : "fas fa-cloud-moon";
        } else if ("4".equals(sky)) {
            return "fas fa-cloud";
        }

        return "fas fa-question";
    }

    private String formatHourToKorean(int hour) {
        if (hour == 0) return "자정";
        if (hour == 12) return "정오";
        if (hour < 12) return String.format("오전 %d시", hour);
        if (hour == 24) return "자정";
        return String.format("오후 %d시", hour - 12);
    }

    private String formatDayOfWeek(LocalDate date) {
        String[] days = {"일", "월", "화", "수", "목", "금", "토"};
        return days[date.getDayOfWeek().getValue() % 7];
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, String> getGridCoordinates(String regionCode) {
        Map<String, String> coords = new HashMap<>();
        // 지역코드에 따른 격자 좌표 매핑 (간소화)
        switch (regionCode) {
            case "1100000000": // 서울
                coords.put("nx", "60");
                coords.put("ny", "127");
                break;
            case "2600000000": // 부산
                coords.put("nx", "98");
                coords.put("ny", "76");
                break;
            case "2700000000": // 대구
                coords.put("nx", "89");
                coords.put("ny", "90");
                break;
            case "2800000000": // 인천
                coords.put("nx", "55");
                coords.put("ny", "124");
                break;
            case "2900000000": // 광주
                coords.put("nx", "58");
                coords.put("ny", "74");
                break;
            case "3000000000": // 대전
                coords.put("nx", "67");
                coords.put("ny", "100");
                break;
            case "3100000000": // 울산
                coords.put("nx", "102");
                coords.put("ny", "84");
                break;
            default:
                coords.put("nx", "60");
                coords.put("ny", "127");
        }
        return coords;
    }

    private String getRegionNameFromCode(String regionCode) {
        Map<String, String> regionMap = new HashMap<>();
        regionMap.put("1100000000", "서울특별시");
        regionMap.put("2600000000", "부산광역시");
        regionMap.put("2800000000", "인천광역시");
        regionMap.put("2700000000", "대구광역시");
        regionMap.put("3000000000", "대전광역시");
        regionMap.put("2900000000", "광주광역시");
        regionMap.put("3100000000", "울산광역시");
        regionMap.put("4100000000", "경기도");
        regionMap.put("4200000000", "강원도");
        regionMap.put("4300000000", "충청북도");
        regionMap.put("4400000000", "충청남도");
        regionMap.put("4500000000", "전라북도");
        regionMap.put("4600000000", "전라남도");
        regionMap.put("4700000000", "경상북도");
        regionMap.put("4800000000", "경상남도");
        regionMap.put("5000000000", "제주특별자치도");
        return regionMap.getOrDefault(regionCode, "서울특별시");
    }

    // ===== 기본 데이터 생성 메서드 =====

    private WeatherResponseDTO.WeatherSummary createWeatherSummary(
            List<WeatherResponseDTO.HourlyForecast> hourly,
            List<WeatherResponseDTO.HourlyForecast> tomorrowHourly,
            List<WeatherResponseDTO.DailyForecast> weekly) {

        String ultraShortSummary = generateUltraShortSummary(hourly);
        String shortSummary = generateShortSummary(tomorrowHourly);
        String midSummary = generateMidSummary(weekly);

        return WeatherResponseDTO.WeatherSummary.builder()
                .ultraShortSummary(ultraShortSummary)
                .shortSummary(shortSummary)
                .midSummary(midSummary)
                .build();
    }

    private String generateUltraShortSummary(List<WeatherResponseDTO.HourlyForecast> hourly) {
        if (hourly.isEmpty()) return "초단기 데이터를 불러오는 중입니다.";

        int rainyCount = 0;
        double maxTemp = -100;
        double minTemp = 100;

        for (WeatherResponseDTO.HourlyForecast h : hourly) {
            if (h.getWeatherCondition().contains("비") || h.getWeatherCondition().contains("소나기")) {
                rainyCount++;
            }
            if (h.getTemperature() > maxTemp) maxTemp = h.getTemperature();
            if (h.getTemperature() < minTemp) minTemp = h.getTemperature();
        }

        if (rainyCount > 0) {
            return String.format("오늘 %d시간 동안 비 소식이 있습니다. 기온은 %.1f°C~%.1f°C로 예상됩니다.",
                    rainyCount, minTemp, maxTemp);
        } else {
            return String.format("오늘은 맑은 날씨가 이어집니다. 기온은 %.1f°C~%.1f°C로 예상됩니다.",
                    minTemp, maxTemp);
        }
    }

    private String generateShortSummary(List<WeatherResponseDTO.HourlyForecast> tomorrowHourly) {
        if (tomorrowHourly.isEmpty()) return "내일 날씨 정보를 불러오는 중입니다.";

        int rainyCount = 0;
        double maxTemp = -100;
        double minTemp = 100;

        for (WeatherResponseDTO.HourlyForecast h : tomorrowHourly) {
            if (h.getWeatherCondition().contains("비") || h.getWeatherCondition().contains("소나기")) {
                rainyCount++;
            }
            if (h.getTemperature() > maxTemp) maxTemp = h.getTemperature();
            if (h.getTemperature() < minTemp) minTemp = h.getTemperature();
        }

        if (rainyCount > 0) {
            return String.format("내일 %d시간 동안 비 소식이 있습니다. 기온은 %.1f°C~%.1f°C로 예상됩니다.",
                    rainyCount, minTemp, maxTemp);
        } else {
            return String.format("내일은 대체로 맑은 날씨입니다. 기온은 %.1f°C~%.1f°C로 예상됩니다.",
                    minTemp, maxTemp);
        }
    }

    private String generateMidSummary(List<WeatherResponseDTO.DailyForecast> weekly) {
        if (weekly.isEmpty()) return "주간 예보 정보를 불러오는 중입니다.";

        int rainyDays = 0;
        for (WeatherResponseDTO.DailyForecast d : weekly) {
            if (d.getPrecipitationProbability() > 50) {
                rainyDays++;
            }
        }

        if (rainyDays > 0) {
            return String.format("이번 주 %d일 동안 비 소식이 있습니다. 주중에는 대체로 맑은 날씨가 이어집니다.", rainyDays);
        } else {
            return "이번 주는 대체로 맑은 날씨가 이어지며, 강수 확률이 낮습니다.";
        }
    }

    private WeatherResponseDTO createFallbackWeatherData(String regionCode) {
        String regionName = getRegionNameFromCode(regionCode);

        return WeatherResponseDTO.builder()
                .regionName(regionName)
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .current(createDefaultCurrentWeather())
                .hourly(createDefaultHourlyForecast24h(0))
                .tomorrowHourly(createDefaultHourlyForecast24h(1))
                .daily(createDefaultWeeklyForecastWithAmPm())
                .summary(WeatherResponseDTO.WeatherSummary.builder()
                        .ultraShortSummary("데이터를 불러오는 중입니다.")
                        .shortSummary("기본 날씨 정보를 표시합니다.")
                        .midSummary("API 연결을 확인해주세요.")
                        .build())
                .build();
    }

    private WeatherResponseDTO.CurrentWeather createDefaultCurrentWeather() {
        return WeatherResponseDTO.CurrentWeather.builder()
                .temperature(22.0)
                .feelsLike(23.0)
                .humidity(45.0)
                .windSpeed(2.5)
                .windDirection("남서풍")
                .precipitation(0.0)
                .weatherCondition("맑음")
                .weatherIcon("fas fa-sun")
                .updateTime(LocalDateTime.now())
                .build();
    }

    private List<WeatherResponseDTO.HourlyForecast> createDefaultHourlyForecast24h(int daysFromNow) {
        List<WeatherResponseDTO.HourlyForecast> hourly = new ArrayList<>();
        int startHour = LocalDateTime.now().getHour();

        for (int i = 0; i < 24; i++) {
            int hour = (startHour + i) % 24;
            double temp = 20.0 + Math.sin((hour - 6) * Math.PI / 12) * 5.0;

            String condition;
            if (hour < 6 || hour > 20) {
                condition = "맑음";
            } else if (hour < 12) {
                condition = "구름조금";
            } else {
                condition = "구름많음";
            }

            hourly.add(WeatherResponseDTO.HourlyForecast.builder()
                    .time(formatHourToKorean(hour))
                    .temperature(temp)
                    .weatherCondition(condition)
                    .weatherIcon(getWeatherIconFromCode("1", "0", hour))
                    .precipitationProbability(0.0)
                    .humidity(45.0 + Math.sin(hour * Math.PI / 12) * 10.0)
                    .windSpeed(2.0 + Math.random() * 2.0)
                    .build());
        }
        return hourly;
    }

    private List<WeatherResponseDTO.DailyForecast> createDefaultWeeklyForecastWithAmPm() {
        List<WeatherResponseDTO.DailyForecast> weekly = new ArrayList<>();
        String[] days = {"일", "월", "화", "수", "목", "금", "토"};
        String[] amWeathers = {"맑음", "구름조금", "맑음", "흐림", "구름많음", "맑음", "맑음"};
        String[] pmWeathers = {"구름많음", "맑음", "구름조금", "비", "흐림", "구름많음", "맑음"};

        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            double maxTemp = 24.0 - i + Math.random() * 2.0;
            double minTemp = 16.0 - i - Math.random() * 2.0;
            double amTemp = minTemp + 3.0;
            double pmTemp = maxTemp - 2.0;

            weekly.add(WeatherResponseDTO.DailyForecast.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("MM/dd")))
                    .dayOfWeek(days[i])
                    .maxTemp(maxTemp)
                    .minTemp(minTemp)
                    .amTemp(amTemp)
                    .pmTemp(pmTemp)
                    .dayWeather(pmWeathers[i])
                    .nightWeather(amWeathers[i])
                    .dayIcon(getWeatherIconFromCondition(pmWeathers[i], 14))
                    .nightIcon(getWeatherIconFromCondition(amWeathers[i], 8))
                    .precipitationProbability(i == 3 ? 60.0 : i == 4 ? 30.0 : 0.0)
                    .build());
        }
        return weekly;
    }

    private String getWeatherIconFromCondition(String condition, int hour) {
        if (condition.contains("맑음")) {
            return hour >= 6 && hour <= 18 ? "fas fa-sun" : "fas fa-moon";
        } else if (condition.contains("비")) {
            return "fas fa-cloud-rain";
        } else if (condition.contains("구름")) {
            return hour >= 6 && hour <= 18 ? "fas fa-cloud-sun" : "fas fa-cloud-moon";
        } else if (condition.contains("흐림")) {
            return "fas fa-cloud";
        }
        return "fas fa-question";
    }

    // ===== 도우미 클래스 =====

    private static class WeatherDataPoint {
        int hour;
        Double temperature = 20.0;
        String sky = "1";
        String pty = "0";
        Double pop = 0.0;
        Double humidity = 50.0;
        Double windSpeed = 2.5;

        WeatherDataPoint(int hour) {
            this.hour = hour;
        }
    }

    private static class DailyWeatherData {
        Double maxTemp;
        Double minTemp;
        Double amTemp;
        Double pmTemp;
        String amSky = "1";
        String pmSky = "1";
        String amPty = "0";
        String pmPty = "0";
        Double pop = 0.0;
    }
    // parseDoubleSafe 메서드 오버로딩 (기본값 0.0)
    private double parseDoubleSafe(String value) {
        return parseDoubleSafe(value, 0.0);
    }
}