package me.shinsunyoung.projectweatherly.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.util.DateUtil;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.kma.key}")
    private String apiKey;

    @Value("${weatherly.api.kma.url}")
    private String kmaApiUrl;

    /**
     * 초단기예보 조회 (6시간)
     */
    @Cacheable(value = "ultraShortForecast", key = "#regionCode + '_' + #baseDate + '_' + #baseTime")
    public WeatherResponseDto getUltraShortForecast(String regionCode, String baseDate, String baseTime) {
        try {
            // 격자 좌표 조회
            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            // API 호출 URL 생성
            URI uri = UriComponentsBuilder.fromHttpUrl(kmaApiUrl + "/getUltraSrtFcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUri();

            log.info("초단기예보 API 호출: {}, {}, {}", regionCode, baseDate, baseTime);
            log.info("격자 좌표: nx={}, ny={}", nx, ny);

            // 실제 API 호출
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("API 호출 실패");
            }

            String responseBody = response.getBody();
            log.info("초단기예보 API 응답 수신: {} bytes", responseBody.length());

            // 응답 파싱
            return parseUltraShortResponse(responseBody, regionCode, baseDate, baseTime);

        } catch (Exception e) {
            log.error("초단기예보 처리 중 오류", e);
            throw new RuntimeException("초단기예보 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 단기예보 조회 (3일)
     */
    @Cacheable(value = "shortTermForecast", key = "#regionCode")
    public WeatherResponseDto getShortTermForecast(String regionCode) {
        try {
            String baseDate = DateUtil.formatDateOnly(LocalDateTime.now());
            String baseTime = "0500"; // 기상청 기준 시간

            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            // API 호출 URL 생성
            URI uri = UriComponentsBuilder.fromHttpUrl(kmaApiUrl + "/getVilageFcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 300)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
                    .toUri();

            log.info("단기예보 API 호출: {}", regionCode);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API 호출 실패");
            }

            return parseShortTermResponse(response.getBody(), regionCode);

        } catch (Exception e) {
            log.error("단기예보 처리 중 오류", e);
            throw new RuntimeException("단기예보 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 현재 날씨 정보 조회
     */
    @Cacheable(value = "currentWeather", key = "#regionCode")
    public WeatherResponseDto.CurrentWeather getCurrentWeather(String regionCode) {
        try {
            String baseDate = DateUtil.formatDateOnly(LocalDateTime.now());
            String baseTime = DateUtil.getBaseTime();

            Map<String, String> gridCoords = getGridCoordinates(regionCode);
            String nx = gridCoords.get("nx");
            String ny = gridCoords.get("ny");

            // API 호출 URL 생성
            URI uri = UriComponentsBuilder.fromHttpUrl(kmaApiUrl + "/getUltraSrtNcst")
                    .queryParam("serviceKey", apiKey)
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

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API 호출 실패");
            }

            return parseCurrentWeatherResponse(response.getBody());

        } catch (Exception e) {
            log.error("현재 날씨 처리 중 오류", e);
            throw new RuntimeException("현재 날씨 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 초단기예보 응답 파싱
     */
    private WeatherResponseDto parseUltraShortResponse(String jsonResponse, String regionCode, String baseDate, String baseTime) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items").path("item");

        if (!items.isArray() || items.size() == 0) {
            throw new RuntimeException("예보 데이터가 없습니다.");
        }

        // 시간별 데이터를 그룹화
        Map<String, Map<String, String>> hourlyData = new HashMap<>();

        for (JsonNode item : items) {
            String fcstTime = item.path("fcstTime").asText();
            String category = item.path("category").asText();
            String fcstValue = item.path("fcstValue").asText();

            if (!hourlyData.containsKey(fcstTime)) {
                hourlyData.put(fcstTime, new HashMap<>());
            }

            hourlyData.get(fcstTime).put(category, fcstValue);
        }

        // 시간별 예보 생성
        List<WeatherResponseDto.HourlyForecast> hourly = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : hourlyData.entrySet()) {
            String time = entry.getKey().substring(0, 2) + "시";
            Map<String, String> data = entry.getValue();

            String temperature = data.getOrDefault("T1H", "0");
            String sky = data.getOrDefault("SKY", "1");
            String precipitation = data.getOrDefault("PTY", "0");

            WeatherResponseDto.HourlyForecast forecast = WeatherResponseDto.HourlyForecast.builder()
                    .time(time)
                    .temperature(Double.parseDouble(temperature))
                    .weatherCondition(getWeatherCondition(sky, precipitation))
                    .weatherIcon(getWeatherIcon(sky, precipitation, time))
                    .precipitationProbability(0.0) // 초단기예보에는 강수확률 없음
                    .humidity(50.0) // 기본값
                    .build();

            hourly.add(forecast);
        }

        // 현재 날씨 생성
        WeatherResponseDto.CurrentWeather current = createCurrentWeatherFromHourly(hourly);

        return WeatherResponseDto.builder()
                .regionName(getRegionNameFromCode(regionCode))
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .current(current)
                .hourly(hourly)
                .summary(WeatherResponseDto.WeatherSummary.builder()
                        .ultraShortSummary("초단기예보: " + hourly.size() + "시간 동안 " +
                                getWeatherTrend(hourly) + " 날씨가 예상됩니다.")
                        .build())
                .build();
    }

    /**
     * 단기예보 응답 파싱
     */
    private WeatherResponseDto parseShortTermResponse(String jsonResponse, String regionCode) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items").path("item");

        if (!items.isArray() || items.size() == 0) {
            throw new RuntimeException("예보 데이터가 없습니다.");
        }

        // 날짜별 데이터 그룹화
        Map<String, Map<String, String>> dailyData = new HashMap<>();

        for (JsonNode item : items) {
            String fcstDate = item.path("fcstDate").asText();
            String category = item.path("category").asText();
            String fcstValue = item.path("fcstValue").asText();

            String key = fcstDate;
            if (!dailyData.containsKey(key)) {
                dailyData.put(key, new HashMap<>());
            }

            dailyData.get(key).put(category, fcstValue);
        }

        // 일별 예보 생성
        List<WeatherResponseDto.DailyForecast> daily = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : dailyData.entrySet()) {
            String dateStr = entry.getKey();
            Map<String, String> data = entry.getValue();

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
            String dayOfWeek = date.format(DateTimeFormatter.ofPattern("E"));

            // 최고/최저 기온 추출
            Double maxTemp = parseDoubleSafe(data.get("TMX"));
            Double minTemp = parseDoubleSafe(data.get("TMN"));

            // 하늘 상태 (평균값 사용)
            String sky = data.getOrDefault("SKY", "1");
            String pty = data.getOrDefault("PTY", "0");

            WeatherResponseDto.DailyForecast forecast = WeatherResponseDto.DailyForecast.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("MM/dd")))
                    .dayOfWeek(dayOfWeek)
                    .maxTemp(maxTemp != null ? maxTemp : 20.0)
                    .minTemp(minTemp != null ? minTemp : 10.0)
                    .dayWeather(getWeatherCondition(sky, pty))
                    .nightWeather(getWeatherCondition(sky, pty))
                    .dayIcon(getWeatherIcon(sky, pty, "1200"))
                    .nightIcon(getWeatherIcon(sky, pty, "0000"))
                    .precipitationProbability(parseDoubleSafe(data.get("POP")))
                    .build();

            daily.add(forecast);
        }

        // 요약 생성
        String summary = generateDailySummary(daily);

        return WeatherResponseDto.builder()
                .regionName(getRegionNameFromCode(regionCode))
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .daily(daily)
                .summary(WeatherResponseDto.WeatherSummary.builder()
                        .shortSummary(summary)
                        .midSummary("중기예보는 별도 API를 호출해야 합니다.")
                        .build())
                .build();
    }

    /**
     * 현재 날씨 응답 파싱
     */
    private WeatherResponseDto.CurrentWeather parseCurrentWeatherResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items").path("item");

        double temperature = 20.0;
        double humidity = 50.0;
        double precipitation = 0.0;

        if (items.isArray()) {
            for (JsonNode item : items) {
                String category = item.path("category").asText();
                String obsrValue = item.path("obsrValue").asText();

                switch (category) {
                    case "T1H": // 기온
                        temperature = parseDoubleSafe(obsrValue, 20.0);
                        break;
                    case "REH": // 습도
                        humidity = parseDoubleSafe(obsrValue, 50.0);
                        break;
                    case "RN1": // 1시간 강수량
                        precipitation = parseDoubleSafe(obsrValue, 0.0);
                        break;
                }
            }
        }

        return WeatherResponseDto.CurrentWeather.builder()
                .temperature(temperature)
                .feelsLike(calculateFeelsLike(temperature, humidity))
                .humidity(humidity)
                .windSpeed(2.5) // 기본값
                .windDirection("남서풍") // 기본값
                .precipitation(precipitation)
                .weatherCondition(getCurrentWeatherCondition(temperature, humidity))
                .weatherIcon(getCurrentWeatherIcon(temperature, humidity))
                .updateTime(LocalDateTime.now())
                .build();
    }

    // ===== 유틸리티 메서드 =====

    private Map<String, String> getGridCoordinates(String regionCode) {
        // 지역코드 → 격자 좌표 매핑 (더 정확한 매핑 필요시 확장)
        Map<String, Map<String, String>> gridMap = new HashMap<>();

        gridMap.put("1100000000", Map.of("nx", "60", "ny", "127"));  // 서울
        gridMap.put("2600000000", Map.of("nx", "98", "ny", "76"));   // 부산
        gridMap.put("2800000000", Map.of("nx", "55", "ny", "124"));  // 인천
        gridMap.put("2700000000", Map.of("nx", "89", "ny", "90"));   // 대구
        gridMap.put("3000000000", Map.of("nx", "67", "ny", "100"));  // 대전
        gridMap.put("2900000000", Map.of("nx", "58", "ny", "74"));   // 광주
        gridMap.put("3100000000", Map.of("nx", "102", "ny", "84"));  // 울산
        gridMap.put("4100000000", Map.of("nx", "60", "ny", "120"));  // 경기
        gridMap.put("4200000000", Map.of("nx", "73", "ny", "134"));  // 강원
        gridMap.put("4300000000", Map.of("nx", "69", "ny", "107"));  // 충북
        gridMap.put("4400000000", Map.of("nx", "68", "ny", "100"));  // 충남
        gridMap.put("4500000000", Map.of("nx", "63", "ny", "89"));   // 전북
        gridMap.put("4600000000", Map.of("nx", "51", "ny", "67"));   // 전남
        gridMap.put("4700000000", Map.of("nx", "89", "ny", "91"));   // 경북
        gridMap.put("4800000000", Map.of("nx", "91", "ny", "77"));   // 경남
        gridMap.put("5000000000", Map.of("nx", "52", "ny", "38"));   // 제주

        return gridMap.getOrDefault(regionCode, Map.of("nx", "60", "ny", "127"));
    }

    private String getRegionNameFromCode(String regionCode) {
        return switch (regionCode) {
            case "1100000000" -> "서울특별시";
            case "2600000000" -> "부산광역시";
            case "2800000000" -> "인천광역시";
            case "2700000000" -> "대구광역시";
            case "3000000000" -> "대전광역시";
            case "2900000000" -> "광주광역시";
            case "3100000000" -> "울산광역시";
            case "4100000000" -> "경기도";
            case "4200000000" -> "강원도";
            case "4300000000" -> "충청북도";
            case "4400000000" -> "충청남도";
            case "4500000000" -> "전라북도";
            case "4600000000" -> "전라남도";
            case "4700000000" -> "경상북도";
            case "4800000000" -> "경상남도";
            case "5000000000" -> "제주특별자치도";
            default -> "서울특별시";
        };
    }

    private String getWeatherCondition(String skyCode, String ptyCode) {
        int sky = Integer.parseInt(skyCode);
        int pty = Integer.parseInt(ptyCode);

        if (pty > 0) {
            return switch (pty) {
                case 1 -> "비";
                case 2 -> "비/눈";
                case 3 -> "눈";
                case 4 -> "소나기";
                default -> "비";
            };
        }

        return switch (sky) {
            case 1 -> "맑음";
            case 3 -> "구름많음";
            case 4 -> "흐림";
            default -> "맑음";
        };
    }

    private String getWeatherIcon(String skyCode, String ptyCode, String timeStr) {
        int sky = Integer.parseInt(skyCode);
        int pty = Integer.parseInt(ptyCode);
        int hour = Integer.parseInt(timeStr.substring(0, 2));

        boolean isDay = hour >= 6 && hour < 18;

        if (pty > 0) {
            return switch (pty) {
                case 1 -> "fas fa-cloud-rain";
                case 2 -> "fas fa-cloud-meatball";
                case 3 -> "fas fa-snowflake";
                case 4 -> "fas fa-cloud-showers-heavy";
                default -> "fas fa-cloud-rain";
            };
        }

        if (sky == 1) {
            return isDay ? "fas fa-sun" : "fas fa-moon";
        } else if (sky == 3) {
            return isDay ? "fas fa-cloud-sun" : "fas fa-cloud-moon";
        } else {
            return "fas fa-cloud";
        }
    }

    private String getCurrentWeatherCondition(double temp, double humidity) {
        if (temp >= 30) return "더움";
        if (temp >= 25) return "따뜻함";
        if (temp >= 15) return "선선함";
        if (temp >= 5) return "쌀쌀함";
        return "춥다";
    }

    private String getCurrentWeatherIcon(double temp, double humidity) {
        if (temp >= 25) return "fas fa-sun";
        if (temp >= 15) return "fas fa-cloud-sun";
        if (temp >= 5) return "fas fa-cloud";
        return "fas fa-snowflake";
    }

    private Double parseDoubleSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
                return null;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        Double result = parseDoubleSafe(value);
        return result != null ? result : defaultValue;
    }

    private double calculateFeelsLike(double temp, double humidity) {
        // 체감온도 계산 (간단한 공식)
        return temp + (humidity - 50) * 0.1;
    }

    private WeatherResponseDto.CurrentWeather createCurrentWeatherFromHourly(List<WeatherResponseDto.HourlyForecast> hourly) {
        if (hourly.isEmpty()) {
            return WeatherResponseDto.CurrentWeather.builder()
                    .temperature(20.0)
                    .feelsLike(21.0)
                    .humidity(50.0)
                    .windSpeed(2.5)
                    .windDirection("남서풍")
                    .precipitation(0.0)
                    .weatherCondition("맑음")
                    .weatherIcon("fas fa-sun")
                    .updateTime(LocalDateTime.now())
                    .build();
        }

        WeatherResponseDto.HourlyForecast first = hourly.get(0);
        return WeatherResponseDto.CurrentWeather.builder()
                .temperature(first.getTemperature())
                .feelsLike(first.getTemperature() + 1.0)
                .humidity(first.getHumidity())
                .windSpeed(2.5)
                .windDirection("남서풍")
                .precipitation(0.0)
                .weatherCondition(first.getWeatherCondition())
                .weatherIcon(first.getWeatherIcon())
                .updateTime(LocalDateTime.now())
                .build();
    }

    private String getWeatherTrend(List<WeatherResponseDto.HourlyForecast> hourly) {
        if (hourly.size() < 2) return "변화 없는";

        String first = hourly.get(0).getWeatherCondition();
        String last = hourly.get(hourly.size() - 1).getWeatherCondition();

        if (first.equals(last)) {
            return first + " 유지되는";
        } else {
            return first + "에서 " + last + "로 변화하는";
        }
    }

    private String generateDailySummary(List<WeatherResponseDto.DailyForecast> daily) {
        if (daily.isEmpty()) return "예보 정보 없음";

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < Math.min(3, daily.size()); i++) {
            WeatherResponseDto.DailyForecast day = daily.get(i);
            summary.append(day.getDate())
                    .append("(").append(day.getDayOfWeek()).append(") ")
                    .append(day.getDayWeather())
                    .append(", ");
        }

        return summary.substring(0, summary.length() - 2);
    }
}