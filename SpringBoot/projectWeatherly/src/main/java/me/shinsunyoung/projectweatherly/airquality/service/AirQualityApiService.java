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

    /**
     * 시도별 조회 (전국 포함)
     * [수정] 스케줄러가 호출 시 항상 최신 데이터를 위해 @Cacheable 제거 (또는 별도 메서드 분리 권장)
     * 여기서는 단순화를 위해 캐시 제거함.
     */
    // @Cacheable(value = "airQualityBySido", key = "#sidoName") // 👈 제거: 항상 최신 데이터 요청
    public List<AirQualityResponseDTO> getAirQualityBySido(String sidoName) {
        try {
            String encodedSidoName = URLEncoder.encode(sidoName, StandardCharsets.UTF_8);

            // "전국"이면 1000개, 아니면 100개
            int numOfRows = "전국".equals(sidoName) ? 1000 : 100;

            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getCtprvnRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("pageNo", 1)
                    .queryParam("sidoName", encodedSidoName)
                    .queryParam("ver", "1.3")
                    .build(true)
                    .toUri();

            log.info("대기질 API 호출: {} (요청 수: {})", sidoName, numOfRows);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String responseBody = response.getBody();

            if (isErrorResponse(responseBody)) {
                log.warn("🚨 API 에러 발생 -> 더미 데이터 반환");
                return getMockDataList(sidoName);
            }

            // [중요] 파싱 시 요청한 sidoName("전국")이 아니라 실제 데이터의 sidoName을 쓰도록 유도
            List<AirQualityResponseDTO> list = parseAirQualityResponse(responseBody, sidoName);

            if (list == null || list.isEmpty()) {
                return getMockDataList(sidoName);
            }
            return list;

        } catch (Exception e) {
            log.error("API 호출 중 예외: {}", e.getMessage());
            return getMockDataList(sidoName);
        }
    }

    // ... (나머지 메서드는 기존 유지) ...
    @Cacheable(value = "airQualityByStation", key = "#stationName")
    public AirQualityResponseDTO getAirQualityByStation(String stationName) {
        try {
            String encodedStationName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMsrstnAcctoRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey).queryParam("returnType", "json").queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1).queryParam("stationName", encodedStationName).queryParam("dataTerm", "DAILY")
                    .queryParam("ver", "1.3").build(true).toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String body = response.getBody();
            if (isErrorResponse(body)) return getSingleMockData(stationName);
            AirQualityResponseDTO dto = parseStationAirQualityResponse(body, stationName);
            if (dto == null) return getSingleMockData(stationName);
            return dto;
        } catch (Exception e) {
            return getSingleMockData(stationName);
        }
    }

    @Cacheable(value = "nearbyStations", key = "#tmX + '_' + #tmY")
    public String getNearbyStation(Double tmX, Double tmY) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getNearbyMsrstnList")
                    .queryParam("serviceKey", apiKey).queryParam("returnType", "json")
                    .queryParam("tmX", tmX).queryParam("tmY", tmY).build(true).toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("response").path("body").path("items");
                if (items.isArray() && items.size() > 0) return items.get(0).path("stationName").asText();
            }
        } catch (Exception e) {
            log.error("근접 측정소 조회 실패", e);
        }
        return "가상 측정소";
    }

    public List<AirQualityResponseDTO.AirQualityForecast> getAirQualityForecast(String sidoName) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMinuDustFrcstDspth")
                    .queryParam("serviceKey", apiKey).queryParam("returnType", "json")
                    .queryParam("searchDate", today).queryParam("informCode", "PM10").build(true).toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String body = response.getBody();
            if (isErrorResponse(body)) return getMockForecasts();
            List<AirQualityResponseDTO.AirQualityForecast> list = parseForecastResponse(body);
            if (list == null || list.isEmpty()) return getMockForecasts();
            return list;
        } catch (Exception e) {
            return getMockForecasts();
        }
    }

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
                    // 여기서 requestSidoName("전국")을 그대로 넘기면 안 됨!
                    // 각 아이템 내부의 실제 sidoName을 사용하도록 parseAirQualityItem 내부 로직이 중요함
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
                String dataTime = item.path("dataTime").asText(); // [핵심] 발표 시간 추출
                String informData = item.path("informData").asText();
                String informOverall = item.path("informOverall").asText();
                String informCause = item.path("informCause").asText();
                String informGrade = item.path("informGrade").asText();
                String overallGrade = extractGradeFromForecast(informGrade, "서울");

                forecasts.add(AirQualityResponseDTO.AirQualityForecast.builder()
                        .dataTime(dataTime) // [추가]
                        .date(informData)
                        .overallGrade(overallGrade)
                        .pm10Grade(overallGrade)
                        .pm25Grade(overallGrade)
                        .advice(informOverall)
                        .cause(informCause)
                        .build());
            }
        }
        return forecasts;
    }

    // [핵심 수정] JSON Item 내부의 sidoName을 우선 사용
    private AirQualityResponseDTO parseAirQualityItem(JsonNode item, String defaultSidoName) {
        try {
            // 1. JSON에서 sidoName 추출 시도
            String itemSidoName = item.path("sidoName").asText(null);

            // 2. 없으면 요청 시 사용한 defaultSidoName("전국" 등) 사용
            // 하지만 "전국"일 경우엔 반드시 item 내부 값이 있어야 정상적인 저장이 가능함
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
            Double o3Value = parseDoubleSafe(item.path("o3Value").asText());
            String o3Grade = item.path("o3Grade").asText("2");
            Double no2Value = parseDoubleSafe(item.path("no2Value").asText());
            String no2Grade = item.path("no2Grade").asText("2");
            Double coValue = parseDoubleSafe(item.path("coValue").asText());
            String coGrade = item.path("coGrade").asText("2");
            Double so2Value = parseDoubleSafe(item.path("so2Value").asText());
            String so2Grade = item.path("so2Grade").asText("2");
            Integer khaiValue = parseIntegerSafe(item.path("khaiValue").asText());
            String khaiGrade = item.path("khaiGrade").asText("2");
            String overallGrade = determineOverallGrade(pm10Grade, pm25Grade, o3Grade, no2Grade);

            return AirQualityResponseDTO.builder()
                    .stationName(stationName)
                    .sidoName(finalSidoName) // [수정됨] 실제 지역명 저장
                    .dataTime(dataTime)
                    .khai(createIndex(khaiValue, khaiGrade, ""))
                    .pm10(createIndex(pm10Value, pm10Grade, "㎍/㎥"))
                    .pm25(createIndex(pm25Value, pm25Grade, "㎍/㎥"))
                    .o3(createIndex(o3Value, o3Grade, "ppm"))
                    .no2(createIndex(no2Value, no2Grade, "ppm"))
                    .co(createIndex(coValue, coGrade, "ppm"))
                    .so2(createIndex(so2Value, so2Grade, "ppm"))
                    .overallGrade(overallGrade)
                    .overallStatus(convertGradeToStatus(overallGrade))
                    .healthAdvice(generateHealthAdvice(overallGrade))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private List<AirQualityResponseDTO> getMockDataList(String sidoName) {
        List<AirQualityResponseDTO> list = new ArrayList<>();
        // 모의 데이터 생성 시에도 "전국"이면 가상의 여러 지역을 만들어줌
        if ("전국".equals(sidoName)) {
            list.add(createMockDTO("서울", "서울본청(가상)"));
            list.add(createMockDTO("부산", "부산본청(가상)"));
            list.add(createMockDTO("대구", "대구본청(가상)"));
        } else {
            String[] fakeStations = {sidoName + " 본청(가상)", sidoName + " 동부(가상)", "테스트측정소"};
            for (String station : fakeStations) list.add(createMockDTO(sidoName, station));
        }
        return list;
    }

    private AirQualityResponseDTO getSingleMockData(String stationName) {
        if ("가상 측정소".equals(stationName) || "중구".equals(stationName)) return createMockDTO("위치확인불가", "가상 측정소");
        return createMockDTO("가상지역", stationName + "(가상)");
    }

    private AirQualityResponseDTO createMockDTO(String sidoName, String stationName) {
        String grade = String.valueOf(random.nextInt(4) + 1);
        return AirQualityResponseDTO.builder().sidoName(sidoName).stationName(stationName).dataTime(LocalDateTime.now()).khai(createIndex(random.nextInt(200), grade, "점")).pm10(createIndex(random.nextInt(150), grade, "㎍/㎥")).pm25(createIndex(random.nextInt(100), grade, "㎍/㎥")).o3(createIndex(0.035, "2", "ppm")).no2(createIndex(0.021, "1", "ppm")).co(createIndex(0.4, "1", "ppm")).so2(createIndex(0.003, "1", "ppm")).overallGrade(grade).overallStatus(convertGradeToStatus(grade)).healthAdvice("📢 [테스트 모드] API 호출 한도 초과").build();
    }

    private List<AirQualityResponseDTO.AirQualityForecast> getMockForecasts() {
        List<AirQualityResponseDTO.AirQualityForecast> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 2; i++) {
            String grade = String.valueOf(random.nextInt(3) + 1);
            list.add(AirQualityResponseDTO.AirQualityForecast.builder().date(today.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).overallGrade(grade).pm10Grade(grade).pm25Grade(grade).advice("📢 [가상 예보] 현재 API 상태가 원활하지 않습니다.").cause("대기질 정보 통신 상태를 확인해주세요.").build());
        }
        return list;
    }

    // 유틸리티 메서드들 (기존 유지)
    private Integer parseIntegerSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) return null;
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDoubleSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) return null;
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private AirQualityResponseDTO.AirQualityIndex createIndex(Number val, String grade, String unit) {
        return AirQualityResponseDTO.AirQualityIndex.builder().value(val != null ? val.intValue() : 0).grade(grade).status(convertGradeToStatus(grade)).unit(unit).build();
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
        int worst = 1;
        for (String g : grades) {
            try {
                if (g != null && !g.isEmpty()) worst = Math.max(worst, Integer.parseInt(g.trim()));
            } catch (Exception e) {
            }
        }
        return String.valueOf(worst);
    }

    private String generateHealthAdvice(String grade) {
        return switch (grade) {
            case "1" -> "대기질이 상쾌합니다! 환기하기 좋아요.";
            case "2" -> "대기질이 무난합니다. 평범한 하루네요.";
            case "3" -> "공기가 탁해요. 마스크를 챙기세요.";
            case "4" -> "매우 나쁩니다! 가급적 외출을 삼가세요.";
            default -> "대기질 정보를 확인해주세요.";
        };
    }

    private String extractGradeFromForecast(String informGrade, String region) {
        if (informGrade == null) return "2";
        try {
            String[] parts = informGrade.split(",");
            for (String part : parts) {
                if (part.contains(region)) {
                    if (part.contains("좋음")) return "1";
                    if (part.contains("보통")) return "2";
                    if (part.contains("나쁨")) return "3";
                    if (part.contains("매우나쁨")) return "4";
                }
            }
        } catch (Exception e) {
        }
        return "2";
    }
}