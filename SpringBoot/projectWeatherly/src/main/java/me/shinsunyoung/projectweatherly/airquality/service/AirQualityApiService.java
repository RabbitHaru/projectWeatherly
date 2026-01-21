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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private final Random random = new Random();

    // [1] 시도별 조회 (컴파일 에러 해결됨)
    public List<AirQualityResponseDTO> getAirQualityBySido(String sidoName) {
        try {
            String encodedSidoName = URLEncoder.encode(sidoName, StandardCharsets.UTF_8);
            int numOfRows = "전국".equals(sidoName) ? 1000 : 100;

            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getCtprvnRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey).queryParam("returnType", "json")
                    .queryParam("numOfRows", numOfRows).queryParam("pageNo", 1)
                    .queryParam("sidoName", encodedSidoName).queryParam("ver", "1.3")
                    .build(true).toUri();

            log.info("API 호출: {} (요청 수: {})", sidoName, numOfRows);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (isErrorResponse(response.getBody())) return getMockDataList(sidoName);

            List<AirQualityResponseDTO> list = parseAirQualityResponse(response.getBody(), sidoName);
            if (list == null || list.isEmpty()) return getMockDataList(sidoName);
            return list;

        } catch (Exception e) {
            log.error("API 호출 실패: {}", e.getMessage());
            return getMockDataList(sidoName);
        }
    }

    // [2] 측정소별 조회
    @Cacheable(value = "airQualityByStation", key = "#stationName")
    public AirQualityResponseDTO getAirQualityByStation(String stationName) {
        try {
            String encodedStationName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMsrstnAcctoRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey).queryParam("returnType", "json").queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1).queryParam("stationName", encodedStationName).queryParam("dataTerm", "DAILY")
                    .queryParam("ver", "1.3").build(true).toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (isErrorResponse(response.getBody())) return getSingleMockData(stationName);

            AirQualityResponseDTO dto = parseStationAirQualityResponse(response.getBody(), stationName);
            return (dto != null) ? dto : getSingleMockData(stationName);
        } catch (Exception e) {
            return getSingleMockData(stationName);
        }
    }

    // [3] 예보 조회
    public List<AirQualityResponseDTO.AirQualityForecast> getAirQualityForecast(String sidoName) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMinuDustFrcstDspth")
                    .queryParam("serviceKey", apiKey).queryParam("returnType", "json")
                    .queryParam("searchDate", today).queryParam("informCode", "PM10").build(true).toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (isErrorResponse(response.getBody())) return getMockForecasts();

            List<AirQualityResponseDTO.AirQualityForecast> list = parseForecastResponse(response.getBody());
            return (list != null && !list.isEmpty()) ? list : getMockForecasts();
        } catch (Exception e) {
            log.error("예보 API 실패", e);
            return getMockForecasts();
        }
    }

    // 헬퍼들
    private boolean isErrorResponse(String body) {
        if (body == null) return true;
        return body.contains("quota exceeded") || body.contains("SERVICE ERROR") || body.trim().startsWith("<");
    }

    private List<AirQualityResponseDTO> parseAirQualityResponse(String jsonResponse, String requestSidoName) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items");
        List<AirQualityResponseDTO> result = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                try {
                    AirQualityResponseDTO dto = parseAirQualityItem(item, requestSidoName);
                    if (dto != null) result.add(dto);
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    private AirQualityResponseDTO parseStationAirQualityResponse(String jsonResponse, String stationName) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items");
        if (items.isArray() && items.size() > 0) return parseAirQualityItem(items.get(0), "대한민국");
        return null;
    }

    private List<AirQualityResponseDTO.AirQualityForecast> parseForecastResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items");
        List<AirQualityResponseDTO.AirQualityForecast> forecasts = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                String dataTime = item.path("dataTime").asText();
                String informData = item.path("informData").asText();
                String informOverall = item.path("informOverall").asText();
                String informCause = item.path("informCause").asText();
                String informGrade = item.path("informGrade").asText();
                String overallGrade = extractGradeFromForecast(informGrade, "서울");

                forecasts.add(AirQualityResponseDTO.AirQualityForecast.builder()
                        .dataTime(dataTime).date(informData)
                        .overallGrade(overallGrade).pm10Grade(overallGrade).pm25Grade(overallGrade)
                        .advice(informOverall).cause(informCause).build());
            }
        }
        return forecasts;
    }

    private AirQualityResponseDTO parseAirQualityItem(JsonNode item, String defaultSidoName) {
        try {
            String itemSidoName = item.path("sidoName").asText(null);
            String finalSidoName = (itemSidoName != null && !itemSidoName.isEmpty()) ? itemSidoName : defaultSidoName;
            String stationName = item.path("stationName").asText(null);
            String dataTimeStr = item.path("dataTime").asText(null);
            if (stationName == null || dataTimeStr == null) return null;

            LocalDateTime dataTime;
            try {
                dataTime = LocalDateTime.parse(dataTimeStr.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                dataTime = LocalDateTime.now();
            }

            Integer pm10Value = parseIntegerSafe(item.path("pm10Value").asText());
            String pm10Grade = item.path("pm10Grade").asText("2");
            Integer pm25Value = parseIntegerSafe(item.path("pm25Value").asText());
            String pm25Grade = item.path("pm25Grade").asText("2");
            Integer khaiValue = parseIntegerSafe(item.path("khaiValue").asText());
            String khaiGrade = item.path("khaiGrade").asText("2");
            String overallGrade = pm10Grade;

            return AirQualityResponseDTO.builder()
                    .stationName(stationName).sidoName(finalSidoName).dataTime(dataTime)
                    .khai(createIndex(khaiValue, khaiGrade, "")).pm10(createIndex(pm10Value, pm10Grade, "㎍/㎥"))
                    .pm25(createIndex(pm25Value, pm25Grade, "㎍/㎥")).overallGrade(overallGrade)
                    .overallStatus(convertGradeToStatus(overallGrade)).healthAdvice(generateHealthAdvice(overallGrade))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private List<AirQualityResponseDTO> getMockDataList(String sidoName) {
        List<AirQualityResponseDTO> list = new ArrayList<>();
        if ("전국".equals(sidoName)) {
            list.add(createMockDTO("서울", "서울본청(가상)"));
            list.add(createMockDTO("부산", "부산본청(가상)"));
        } else {
            list.add(createMockDTO(sidoName, sidoName + " 측정소(가상)"));
        }
        return list;
    }

    private AirQualityResponseDTO getSingleMockData(String stationName) {
        return createMockDTO("가상지역", stationName + "(가상)");
    }

    private AirQualityResponseDTO createMockDTO(String sidoName, String stationName) {
        String grade = String.valueOf(random.nextInt(4) + 1);
        return AirQualityResponseDTO.builder().sidoName(sidoName).stationName(stationName).dataTime(LocalDateTime.now())
                .pm10(createIndex(50, grade, "㎍/㎥")).pm25(createIndex(20, grade, "㎍/㎥"))
                .overallGrade(grade).overallStatus(convertGradeToStatus(grade)).healthAdvice("📢 [테스트 모드]").build();
    }

    private List<AirQualityResponseDTO.AirQualityForecast> getMockForecasts() {
        List<AirQualityResponseDTO.AirQualityForecast> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 2; i++) {
            list.add(AirQualityResponseDTO.AirQualityForecast.builder().date(today.plusDays(i).toString()).overallGrade("2").advice("📢 [가상 예보]").cause("API 상태 확인 필요").build());
        }
        return list;
    }

    private Integer parseIntegerSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private AirQualityResponseDTO.AirQualityIndex createIndex(Number v, String g, String u) {
        return AirQualityResponseDTO.AirQualityIndex.builder().value(v != null ? v.intValue() : 0).grade(g).status(convertGradeToStatus(g)).unit(u).build();
    }

    private String convertGradeToStatus(String g) {
        if ("1".equals(g)) return "좋음";
        if ("2".equals(g)) return "보통";
        if ("3".equals(g)) return "나쁨";
        if ("4".equals(g)) return "매우나쁨";
        return "보통";
    }

    private String generateHealthAdvice(String g) {
        return "확인 중...";
    }

    private String extractGradeFromForecast(String t, String r) {
        return "2";
    }
}