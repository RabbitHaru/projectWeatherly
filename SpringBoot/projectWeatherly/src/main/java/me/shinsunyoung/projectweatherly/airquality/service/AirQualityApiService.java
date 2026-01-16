package me.shinsunyoung.projectweatherly.airquality.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${weatherly.api.airkorea.url}")
    private String airKoreaApiUrl;

    @Value("${api.airkorea.key}")
    private String apiKey;

    /**
     * 시도별 실시간 측정정보 조회
     */
    @Cacheable(value = "airQualityBySido", key = "#sidoName")
    public List<AirQualityResponseDTO> getAirQualityBySido(String sidoName) {
        try {
            // API 호출 URL 생성
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getCtprvnRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", 100)
                    .queryParam("pageNo", 1)
                    .queryParam("sidoName", sidoName)
                    .queryParam("ver", "1.3")
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            log.info("대기질 API 호출: {}", sidoName);
            log.info("API URL: {}", uri.toString());

            // 실제 API 호출
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("API 호출 실패");
            }

            String responseBody = response.getBody();
            log.info("API 응답 수신: {} bytes", responseBody.length());

            // 실제 응답 파싱
            return parseAirQualityResponse(responseBody, sidoName);

        } catch (Exception e) {
            log.error("대기질 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("대기질 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 측정소별 실시간 측정정보 조회
     */
    @Cacheable(value = "airQualityByStation", key = "#stationName")
    public AirQualityResponseDTO getAirQualityByStation(String stationName) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMsrstnAcctoRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("stationName", stationName)
                    .queryParam("dataTerm", "DAILY")
                    .queryParam("ver", "1.3")
                    .build()
                    .toUri();

            log.info("측정소별 대기질 API 호출: {}", stationName);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API 호출 실패");
            }

            return parseStationAirQualityResponse(response.getBody(), stationName);

        } catch (Exception e) {
            log.error("측정소별 대기질 API 호출 실패", e);
            throw new RuntimeException("대기질 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 근접 측정소 조회 (TM 좌표 기준)
     */
    @Cacheable(value = "nearbyStations", key = "#tmX + '_' + #tmY")
    public String getNearbyStation(Double tmX, Double tmY) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getNearbyMsrstnList")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("tmX", tmX)
                    .queryParam("tmY", tmY)
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("response").path("body").path("items");

                if (items.isArray() && items.size() > 0) {
                    String stationName = items.get(0).path("stationName").asText();
                    log.info("근접 측정소 조회 결과: {}", stationName);
                    return stationName;
                }
            }

        } catch (Exception e) {
            log.error("근접 측정소 API 호출 실패", e);
        }

        // 기본값
        return "중구";
    }

    /**
     * 대기질 예보 정보 조회
     */
//    @Cacheable(value = "airQualityForecast", key = "#sidoName")
    public List<AirQualityResponseDTO.AirQualityForecast> getAirQualityForecast(String sidoName) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMinuDustFrcstDspth")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("searchDate", today)
                    .queryParam("informCode", "PM10")
                    .build()
                    .toUri();

            log.info("대기질 예보 API 호출: {}", sidoName);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API 호출 실패");
            }

            return parseForecastResponse(response.getBody());

        } catch (Exception e) {
            log.error("대기질 예보 API 호출 실패", e);
            throw new RuntimeException("대기질 예보 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 대기질 응답 파싱
     */
    private List<AirQualityResponseDTO> parseAirQualityResponse(String jsonResponse, String sidoName) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items");

        List<AirQualityResponseDTO> result = new ArrayList<>();

        if (items.isArray()) {
            for (JsonNode item : items) {
                try {
                    AirQualityResponseDTO dto = parseAirQualityItem(item, sidoName);
                    if (dto != null) {
                        result.add(dto);
                    }
                } catch (Exception e) {
                    log.warn("대기질 데이터 파싱 중 오류: {}", e.getMessage());
                }
            }
        }

        log.info("파싱된 대기질 데이터 수: {}건", result.size());
        return result;
    }

    /**
     * 개별 대기질 항목 파싱
     */
    private AirQualityResponseDTO parseAirQualityItem(JsonNode item, String sidoName) {
        try {
            // 필수 필드 확인
            String stationName = item.path("stationName").asText(null);
            String dataTimeStr = item.path("dataTime").asText(null);

            if (stationName == null || dataTimeStr == null) {
                return null;
            }

            // 데이터 시간 파싱
            LocalDateTime dataTime = LocalDateTime.parse(
                    dataTimeStr.replace(" ", "T"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );

            // 각 항목 파싱
            Integer pm10Value = parseIntegerSafe(item.path("pm10Value").asText());
            String pm10Grade = item.path("pm10Grade1h").asText("2");

            Integer pm25Value = parseIntegerSafe(item.path("pm25Value").asText());
            String pm25Grade = item.path("pm25Grade1h").asText("2");

            Double o3Value = parseDoubleSafe(item.path("o3Value").asText());
            String o3Grade = item.path("o3Grade").asText("2");

            Double no2Value = parseDoubleSafe(item.path("no2Value").asText());
            String no2Grade = item.path("no2Grade").asText("2");

            Double coValue = parseDoubleSafe(item.path("coValue").asText());
            String coGrade = item.path("coGrade").asText("2");

            Double so2Value = parseDoubleSafe(item.path("so2Value").asText());
            String so2Grade = item.path("so2Grade").asText("2");

            // 통합대기환경지수
            Integer khaiValue = parseIntegerSafe(item.path("khaiValue").asText());
            String khaiGrade = item.path("khaiGrade").asText("2");

            // 전체 등급 결정 (가장 나쁜 등급으로)
            String overallGrade = determineOverallGrade(pm10Grade, pm25Grade, o3Grade, no2Grade, coGrade, so2Grade);

            return AirQualityResponseDTO.builder()
                    .stationName(stationName)
                    .sidoName(sidoName)
                    .dataTime(dataTime)
                    .khai(createAirQualityIndex(khaiValue, khaiGrade, convertGradeToStatus(khaiGrade), ""))
                    .pm10(createAirQualityIndex(pm10Value, pm10Grade, convertGradeToStatus(pm10Grade), "㎍/㎥"))
                    .pm25(createAirQualityIndex(pm25Value, pm25Grade, convertGradeToStatus(pm25Grade), "㎍/㎥"))
                    .o3(createAirQualityIndex(o3Value, o3Grade, convertGradeToStatus(o3Grade), "ppm"))
                    .no2(createAirQualityIndex(no2Value, no2Grade, convertGradeToStatus(no2Grade), "ppm"))
                    .co(createAirQualityIndex(coValue, coGrade, convertGradeToStatus(coGrade), "ppm"))
                    .so2(createAirQualityIndex(so2Value, so2Grade, convertGradeToStatus(so2Grade), "ppm"))
                    .overallGrade(overallGrade)
                    .overallStatus(convertGradeToStatus(overallGrade))
                    .healthAdvice(generateHealthAdvice(overallGrade))
                    .build();

        } catch (Exception e) {
            log.error("대기질 항목 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 측정소별 응답 파싱
     */
    private AirQualityResponseDTO parseStationAirQualityResponse(String jsonResponse, String stationName) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items");

        if (items.isArray() && items.size() > 0) {
            JsonNode firstItem = items.get(0);
            return parseAirQualityItem(firstItem, getSidoNameFromStation(stationName));
        }

        throw new RuntimeException("측정소 데이터를 찾을 수 없습니다.");
    }

    /**
     * 예보 응답 파싱
     */
    private List<AirQualityResponseDTO.AirQualityForecast> parseForecastResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items");

        List<AirQualityResponseDTO.AirQualityForecast> forecasts = new ArrayList<>();

        if (items.isArray()) {
            for (JsonNode item : items) {
                String informData = item.path("informData").asText();
                String informCode = item.path("informCode").asText();
                String informOverall = item.path("informOverall").asText();

                // 예보 내용에서 등급 추출
                String overallGrade = extractGradeFromForecast(informOverall);
                String pm10Grade = extractGradeFromForecast(informOverall);
                String pm25Grade = extractGradeFromForecast(informOverall);

                forecasts.add(AirQualityResponseDTO.AirQualityForecast.builder()
                        .date(informData)
                        .overallGrade(overallGrade)
                        .pm10Grade(pm10Grade)
                        .pm25Grade(pm25Grade)
                        .advice(generateForecastAdvice(informOverall))
                        .build());
            }
        }

        return forecasts;
    }

    // ===== 유틸리티 메서드 =====

    private Integer parseIntegerSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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

    private AirQualityResponseDTO.AirQualityIndex createAirQualityIndex(Number value, String grade, String status, String unit) {
        Integer intValue = (value != null) ? value.intValue() : null;

        return AirQualityResponseDTO.AirQualityIndex.builder()
                .value(intValue)
                .grade(grade)
                .status(status)
                .unit(unit)
                .build();
    }

    private String convertGradeToStatus(String grade) {
        if (grade == null) return "보통";

        return switch (grade.trim()) {
            case "1" -> "좋음";
            case "2" -> "보통";
            case "3" -> "나쁨";
            case "4" -> "매우나쁨";
            default -> "보통";
        };
    }

    private String determineOverallGrade(String... grades) {
        int worstGrade = 1;

        for (String grade : grades) {
            if (grade != null) {
                try {
                    int gradeNum = Integer.parseInt(grade.trim());
                    if (gradeNum > worstGrade) {
                        worstGrade = gradeNum;
                    }
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        }

        return String.valueOf(worstGrade);
    }

    private String getSidoNameFromStation(String stationName) {
        // 간단한 매핑 (실제 구현시 더 정확한 매핑 필요)
        if (stationName.contains("서울") || stationName.contains("중구") || stationName.contains("강남")) {
            return "서울";
        } else if (stationName.contains("부산")) {
            return "부산";
        } else if (stationName.contains("인천")) {
            return "인천";
        } else if (stationName.contains("대구")) {
            return "대구";
        } else if (stationName.contains("대전")) {
            return "대전";
        } else if (stationName.contains("광주")) {
            return "광주";
        } else if (stationName.contains("울산")) {
            return "울산";
        } else if (stationName.contains("경기")) {
            return "경기";
        } else if (stationName.contains("강원")) {
            return "강원";
        } else if (stationName.contains("충북")) {
            return "충북";
        } else if (stationName.contains("충남")) {
            return "충남";
        } else if (stationName.contains("전북")) {
            return "전북";
        } else if (stationName.contains("전남")) {
            return "전남";
        } else if (stationName.contains("경북")) {
            return "경북";
        } else if (stationName.contains("경남")) {
            return "경남";
        } else if (stationName.contains("제주")) {
            return "제주";
        }

        return "서울"; // 기본값
    }

    private String extractGradeFromForecast(String forecastText) {
        if (forecastText == null) return "2";

        if (forecastText.contains("좋음")) return "1";
        if (forecastText.contains("보통")) return "2";
        if (forecastText.contains("나쁨")) return "3";
        if (forecastText.contains("매우나쁨")) return "4";

        return "2";
    }

    private String generateHealthAdvice(String grade) {
        return switch (grade) {
            case "1" -> "대기질이 양호합니다. 실외 활동에 문제 없습니다.";
            case "2" -> "대기질이 보통입니다. 민감한 분들은 주의가 필요합니다.";
            case "3" -> "대기질이 나쁩니다. 장시간 실외 활동을 자제하세요.";
            case "4" -> "대기질이 매우 나쁩니다. 되도록 실외 활동을 피하세요.";
            default -> "대기질 정보를 확인해주세요.";
        };
    }

    private String generateForecastAdvice(String forecastText) {
        if (forecastText == null) return "예보 정보를 확인해주세요.";
        return forecastText;
    }
}