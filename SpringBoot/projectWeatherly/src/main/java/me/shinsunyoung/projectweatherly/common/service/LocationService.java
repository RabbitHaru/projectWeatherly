package me.shinsunyoung.projectweatherly.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${weatherly.api.ipinfo.url}")
    private String ipInfoUrl;

    @Value("${weatherly.service.default-region}")
    private String defaultRegion;

    @Value("${weatherly.service.default-region-code}")
    private String defaultRegionCode;

    /**
     * IP 주소로부터 위치 정보 조회
     */
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
            location.setCity(node.get("city").asText());
            location.setCountry(node.get("country").asText());
            location.setRegionName(node.get("regionName").asText());

            // 도시명을 기반으로 지역 코드 매핑 (간단한 예시)
            location.setRegionCode(mapCityToRegionCode(location.getCity()));

            log.info("IP 기반 위치 정보 조회 성공: {}", location);
            return location;

        } catch (Exception e) {
            log.warn("IP 기반 위치 정보 조회 실패, 기본 위치 사용: {}", e.getMessage());
            return getDefaultLocation();
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
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

        // 로컬호스트 IP 변환
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                log.error("로컬 IP 조회 실패", e);
            }
        }

        return ip.split(",")[0].trim(); // 첫 번째 IP 반환
    }

    /**
     * GPS 좌표로부터 위치 정보 조회 (카카오 API 사용)
     */
    @Cacheable(value = "gpsLocationCache", key = "#latitude + ',' + #longitude")
    public LocationDTO getLocationByGps(Double latitude, Double longitude) {
        try {
            // 카카오 로컬 API로 좌표 → 주소 변환
            String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json" +
                    "?x=" + longitude + "&y=" + latitude + "&input_coord=WGS84";

            // 실제 구현 시에는 API 키 헤더 추가 필요
            // String response = restTemplate.exchange(url, HttpMethod.GET,
            //     new HttpEntity<>(createHeaders(apiKey)), String.class).getBody();

            // 임시 더미 데이터 반환 (실제 구현 시 API 연동)
            LocationDTO location = new LocationDTO();
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setRegionName("서울특별시");
            location.setRegionCode("1100000000");
            location.setAddress("서울특별시 중구");

            log.info("GPS 기반 위치 정보 조회 성공: {}", location);
            return location;

        } catch (Exception e) {
            log.error("GPS 기반 위치 정보 조회 실패", e);
            throw new RuntimeException("위치 정보를 가져올 수 없습니다.");
        }
    }

    /**
     * 기본 위치 정보 반환
     */
    private LocationDTO getDefaultLocation() {
        LocationDTO location = new LocationDTO();
        location.setRegionName(defaultRegion);
        location.setRegionCode(defaultRegionCode);
        location.setCity(defaultRegion);
        location.setCountry("South Korea");
        return location;
    }

    /**
     * 도시명을 지역 코드로 매핑 (간단한 예시)
     */
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
}