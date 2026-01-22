package me.shinsunyoung.projectweatherly.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.config.ApiConfig;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiConfig apiConfig;

    @Value("${weatherly.service.default-region}")
    private String defaultRegion;

    @Value("${weatherly.service.default-region-code}")
    private String defaultRegionCode;

    @Cacheable(value = "locationCache", key = "#ipAddress")
    public LocationDTO getLocationByIp(String ipAddress) {
        try {
            String url = apiConfig.getIpInfoUrl();
            if (!ipAddress.equals("127.0.0.1") && !ipAddress.equals("0:0:0:0:0:0:0:1")) {
                url = apiConfig.getIpInfoUrl() + "/" + ipAddress;
            }

            log.info("IP 기반 위치 조회 URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());

                LocationDTO location = new LocationDTO();
                location.setIpAddress(ipAddress);

                if (node.has("city") && !node.get("city").isNull()) {
                    String city = node.get("city").asText();
                    location.setCity(city);
                    location.setRegionName(node.get("regionName").asText());
                } else if (node.has("region") && !node.get("region").isNull()) {
                    location.setRegionName(node.get("region").asText());
                } else {
                    location.setRegionName(defaultRegion);
                }

                if (node.has("loc") && !node.get("loc").isNull()) {
                    String[] loc = node.get("loc").asText().split(",");
                    if (loc.length == 2) {
                        try {
                            location.setLatitude(Double.parseDouble(loc[0]));
                            location.setLongitude(Double.parseDouble(loc[1]));
                        } catch (NumberFormatException e) {
                            log.warn("위치 좌표 파싱 실패: {}", e.getMessage());
                        }
                    }
                }

                location.setCountry(node.has("country") ? node.get("country").asText() : "South Korea");
                location.setRegionCode(mapCityToRegionCode(location.getRegionName()));

                log.info("IP 기반 위치 정보 조회 성공: {}, {}", location.getRegionName(), location.getCountry());
                return location;
            }

        } catch (Exception e) {
            log.warn("IP 기반 위치 정보 조회 실패, 기본 위치 사용: {}", e.getMessage());
        }

        return getDefaultLocation();
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                log.error("로컬 IP 조회 실패", e);
            }
        }

        return ip != null ? ip.split(",")[0].trim() : "127.0.0.1";
    }

    @Cacheable(value = "gpsLocationCache", key = "#latitude + ',' + #longitude")
    public LocationDTO getLocationByGps(Double latitude, Double longitude) {
        try {
            String url = apiConfig.getKakaoApiUrl() + "/v2/local/geo/coord2address.json" +
                    "?x=" + longitude +
                    "&y=" + latitude +
                    "&input_coord=WGS84";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiConfig.getKakaoApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            log.info("GPS 기반 위치 조회 응답 상태: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode documents = root.path("documents");

                if (documents.isArray() && documents.size() > 0) {
                    JsonNode firstDoc = documents.get(0);
                    JsonNode address = firstDoc.path("address");

                    LocationDTO location = new LocationDTO();
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);

                    String region1 = address.path("region_1depth_name").asText();
                    String region2 = address.path("region_2depth_name").asText();
                    String region3 = address.path("region_3depth_name").asText();

                    location.setRegionName(region1);
                    location.setCity(region2);

                    // 상세 주소 구성
                    StringBuilder addressBuilder = new StringBuilder();
                    if (region3 != null && !region3.isEmpty()) {
                        addressBuilder.append(region3);
                    }
                    if (firstDoc.has("road_address")) {
                        JsonNode roadAddress = firstDoc.path("road_address");
                        if (roadAddress.has("building_name") && !roadAddress.path("building_name").isNull()) {
                            if (addressBuilder.length() > 0) addressBuilder.append(" ");
                            addressBuilder.append(roadAddress.path("building_name").asText());
                        }
                    }

                    location.setAddress(addressBuilder.length() > 0 ? addressBuilder.toString() : region2);
                    location.setRegionCode(mapRegionToRegionCode(region1, region2));

                    log.info("GPS 기반 위치 정보 조회 성공: {} {}", location.getRegionName(), location.getAddress());
                    return location;
                }
            }
        } catch (Exception e) {
            log.warn("GPS 기반 위치 정보 조회 실패: {}", e.getMessage());
        }

        return getDefaultLocation();
    }

    private LocationDTO getDefaultLocation() {
        LocationDTO location = new LocationDTO();
        location.setRegionName(defaultRegion);
        location.setRegionCode(defaultRegionCode);
        location.setCity(defaultRegion);
        location.setCountry("South Korea");
        location.setAddress("기본 위치");
        return location;
    }

    private String mapCityToRegionCode(String regionName) {
        if (regionName == null) return defaultRegionCode;

        if (regionName.contains("서울")) return "1100000000";
        if (regionName.contains("부산")) return "2600000000";
        if (regionName.contains("인천")) return "2800000000";
        if (regionName.contains("대구")) return "2700000000";
        if (regionName.contains("대전")) return "3000000000";
        if (regionName.contains("광주")) return "2900000000";
        if (regionName.contains("울산")) return "3100000000";
        if (regionName.contains("경기")) return "4100000000";
        if (regionName.contains("강원")) return "4200000000";
        if (regionName.contains("충북")) return "4300000000";
        if (regionName.contains("충남")) return "4400000000";
        if (regionName.contains("전북")) return "4500000000";
        if (regionName.contains("전남")) return "4600000000";
        if (regionName.contains("경북")) return "4700000000";
        if (regionName.contains("경남")) return "4800000000";
        if (regionName.contains("제주")) return "5000000000";

        return defaultRegionCode;
    }

    private String mapRegionToRegionCode(String regionName, String region2depthName) {
        if (regionName == null) return defaultRegionCode;

        // "서울특별시", "서울" 모두 "서울"을 포함하므로 OK!
        if (regionName.contains("서울")) return "1100000000";
        if (regionName.contains("부산")) return "2600000000";
        if (regionName.contains("대구")) return "2700000000";
        if (regionName.contains("인천")) return "2800000000";
        if (regionName.contains("광주")) return "2900000000";
        if (regionName.contains("대전")) return "3000000000";
        if (regionName.contains("울산")) return "3100000000";
        if (regionName.contains("세종")) return "3600000000"; // 세종 추가!
        if (regionName.contains("경기")) return "4100000000";
        if (regionName.contains("강원")) return "4200000000";
        if (regionName.contains("충북") || regionName.contains("충청북도")) return "4300000000";
        if (regionName.contains("충남") || regionName.contains("충청남도")) return "4400000000";
        if (regionName.contains("전북") || regionName.contains("전라북도")) return "4500000000";
        if (regionName.contains("전남") || regionName.contains("전라남도")) return "4600000000";
        if (regionName.contains("경북") || regionName.contains("경상북도")) return "4700000000";
        if (regionName.contains("경남") || regionName.contains("경상남도")) return "4800000000";
        if (regionName.contains("제주")) return "5000000000";

        return defaultRegionCode;
    }
}