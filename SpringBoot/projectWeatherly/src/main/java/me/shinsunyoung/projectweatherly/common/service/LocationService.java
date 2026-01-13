package me.shinsunyoung.projectweatherly.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetAddress;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient kakaoWebClient;

    @Value("${weatherly.api.ipinfo.url}")
    private String ipInfoUrl;

    @Value("${weatherly.service.default-region}")
    private String defaultRegion;

    @Value("${weatherly.service.default-region-code}")
    private String defaultRegionCode;

    @Cacheable(value = "locationCache", key = "#ipAddress")
    public LocationDTO getLocationByIp(String ipAddress) {
        try {
            String url = ipInfoUrl;
            if (!ipAddress.equals("127.0.0.1") && !ipAddress.equals("0:0:0:0:0:0:0:1")) {
                url = ipInfoUrl + "/" + ipAddress;
            }

            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            LocationDTO location = new LocationDTO();
            location.setIpAddress(ipAddress);
            location.setCity(node.get("city") != null ? node.get("city").asText() : defaultRegion);
            location.setCountry(node.get("country") != null ? node.get("country").asText() : "South Korea");
            location.setRegionName(node.get("regionName") != null ? node.get("regionName").asText() : defaultRegion);
            location.setRegionCode(mapCityToRegionCode(location.getCity()));

            log.info("IP 기반 위치 정보 조회 성공: {}", location);
            return location;

        } catch (Exception e) {
            log.warn("IP 기반 위치 정보 조회 실패, 기본 위치 사용: {}", e.getMessage());
            return getDefaultLocation();
        }
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
            String response = kakaoWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geo/coord2address.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("input_coord", "WGS84")
                            .build())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                JsonNode address = documents.get(0).path("address");

                LocationDTO location = new LocationDTO();
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                location.setRegionName(address.path("region_1depth_name").asText());
                location.setRegionCode(mapRegionToRegionCode(location.getRegionName()));
                location.setAddress(address.path("address_name").asText());

                log.info("GPS 기반 위치 정보 조회 성공: {}", location);
                return location;
            } else {
                throw new RuntimeException("주소 정보를 찾을 수 없습니다.");
            }

        } catch (Exception e) {
            log.error("GPS 기반 위치 정보 조회 실패, 기본 위치 사용: {}", e.getMessage());
            LocationDTO defaultLoc = getDefaultLocation();
            defaultLoc.setLatitude(latitude);
            defaultLoc.setLongitude(longitude);
            return defaultLoc;
        }
    }

    private LocationDTO getDefaultLocation() {
        LocationDTO location = new LocationDTO();
        location.setRegionName(defaultRegion);
        location.setRegionCode(defaultRegionCode);
        location.setCity(defaultRegion);
        location.setCountry("South Korea");
        return location;
    }

    private String mapCityToRegionCode(String city) {
        return switch (city) {
            case "Seoul" -> "1100000000";
            case "Busan" -> "2600000000";
            case "Incheon" -> "2800000000";
            case "Daegu" -> "2700000000";
            case "Daejeon" -> "3000000000";
            case "Gwangju" -> "2900000000";
            case "Ulsan" -> "3100000000";
            default -> defaultRegionCode;
        };
    }

    private String mapRegionToRegionCode(String regionName) {
        return switch (regionName) {
            case "서울특별시" -> "1100000000";
            case "부산광역시" -> "2600000000";
            case "인천광역시" -> "2800000000";
            case "대구광역시" -> "2700000000";
            case "대전광역시" -> "3000000000";
            case "광주광역시" -> "2900000000";
            case "울산광역시" -> "3100000000";
            case "경기도" -> "4100000000";
            case "강원도" -> "4200000000";
            case "충청북도" -> "4300000000";
            case "충청남도" -> "4400000000";
            case "전라북도" -> "4500000000";
            case "전라남도" -> "4600000000";
            case "경상북도" -> "4700000000";
            case "경상남도" -> "4800000000";
            case "제주특별자치도" -> "5000000000";
            default -> defaultRegionCode;
        };
    }
}