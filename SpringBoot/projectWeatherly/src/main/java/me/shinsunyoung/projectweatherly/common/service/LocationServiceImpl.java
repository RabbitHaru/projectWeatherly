package me.shinsunyoung.projectweatherly.common.service;

import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.common.dto.LocationRequest;
import me.shinsunyoung.projectweatherly.common.dto.LocationResponse;
import me.shinsunyoung.projectweatherly.common.entity.Region;
import me.shinsunyoung.projectweatherly.common.repository.RegionRepository;
import me.shinsunyoung.projectweatherly.common.util.ApiKeyManager;
import me.shinsunyoung.projectweatherly.common.util.LocationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final RegionRepository regionRepository;
    private final RestTemplate restTemplate;
    private final ApiKeyManager apiKeyManager;

    @Value("${location.default.region-code:1100000000}")
    private String defaultRegionCode;

    @Value("${location.ip-api.url:http://ip-api.com/json/}")
    private String ipApiUrl;

    @Value("${location.kakao.api.url:https://dapi.kakao.com/v2/local}")
    private String kakaoApiUrl;

    @Override
    public LocationResponse getLocationByIp(String ipAddress) {
        try {
            if (ipAddress == null || ipAddress.isEmpty() || ipAddress.equals("127.0.0.1")) {
                return getDefaultLocation();
            }

            // IP-API.com 호출
            String url = ipApiUrl + ipAddress + "?fields=status,message,country,regionName,city,lat,lon,timezone";
            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);

            if (response != null && "success".equals(response.getStatus())) {
                log.info("IP 기반 위치 조회 성공: {}, {}, {}",
                        response.getCountry(), response.getRegionName(), response.getCity());

                // 가장 가까운 지역 찾기
                return findNearestRegion(response.getLat(), response.getLon());
            }
        } catch (Exception e) {
            log.warn("IP 기반 위치 조회 실패: {}", e.getMessage());
        }

        return getDefaultLocation();
    }

    @Override
    public LocationResponse getLocationByGps(Double latitude, Double longitude) {
        try {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("위도와 경도가 필요합니다.");
            }

            // 카카오 API로 주소 변환
            if (apiKeyManager.hasKakaoApiKey()) {
                return getLocationByKakaoApi(latitude, longitude);
            }

            // 카카오 API가 없으면 DB에서 가장 가까운 지역 찾기
            return findNearestRegion(latitude, longitude);

        } catch (Exception e) {
            log.warn("GPS 기반 위치 조회 실패: {}", e.getMessage());
            return findNearestRegion(latitude, longitude);
        }
    }

    private LocationResponse getLocationByKakaoApi(Double latitude, Double longitude) {
        String url = kakaoApiUrl + "/geo/coord2address.json?x=" + longitude + "&y=" + latitude;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKeyManager.getKakaoApiKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<KakaoResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, KakaoResponse.class);

        if (response.getBody() != null && !response.getBody().getDocuments().isEmpty()) {
            KakaoDocument doc = response.getBody().getDocuments().get(0);

            // 주소 파싱
            String sido = doc.getRegion1DepthName();
            String sigungu = doc.getRegion2DepthName();
            String dong = doc.getRegion3DepthName();

            // DB에서 지역 검색
            Optional<Region> regionOpt = regionRepository.findBySidoAndSigunguAndDong(sido, sigungu, dong);

            if (regionOpt.isPresent()) {
                return convertToResponse(regionOpt.get());
            } else {
                // 유사한 지역 검색
                return searchSimilarRegion(sido, sigungu, dong);
            }
        }

        return getDefaultLocation();
    }

    @Override
    public LocationResponse getLocationByAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return getDefaultLocation();
        }

        try {
            // 주소 파싱 (시/도/구/동)
            String[] parts = address.split(" ");
            if (parts.length >= 4) {
                return getLocationByAddress(parts[0], parts[1], parts[2] + " " + parts[3]);
            } else if (parts.length == 3) {
                return getLocationByAddress(parts[0], parts[1], parts[2]);
            } else if (parts.length == 2) {
                return getLocationByAddress(parts[0], parts[1], "");
            } else {
                return searchRegionByName(address);
            }
        } catch (Exception e) {
            log.warn("주소 기반 위치 조회 실패: {}", e.getMessage());
            return getDefaultLocation();
        }
    }

    @Override
    public LocationResponse getLocationByAddress(String sido, String sigungu, String dong) {
        try {
            Optional<Region> regionOpt;

            if (dong != null && !dong.isEmpty()) {
                // 시/도/구/동으로 정확히 검색
                regionOpt = regionRepository.findBySidoAndSigunguAndDong(sido, sigungu, dong);
            } else {
                // 시/도/구로 검색
                List<Region> regions = regionRepository.findBySidoAndSigungu(sido, sigungu);
                if (!regions.isEmpty()) {
                    regionOpt = Optional.of(regions.get(0));
                } else {
                    regionOpt = Optional.empty();
                }
            }

            if (regionOpt.isPresent()) {
                return convertToResponse(regionOpt.get());
            } else {
                return searchSimilarRegion(sido, sigungu, dong);
            }
        } catch (Exception e) {
            log.warn("주소 상세 기반 위치 조회 실패: {}/{}/{}", sido, sigungu, dong);
            return getDefaultLocation();
        }
    }

    @Override
    public LocationResponse getLocationByRegionCode(String regionCode) {
        return regionRepository.findByRegionCode(regionCode)
                .map(this::convertToResponse)
                .orElseGet(this::getDefaultLocation);
    }

    @Override
    public LocationResponse determineUserLocation(LocationRequest request) {
        // 우선순위: GPS > 주소 > IP > 기본
        if (request.getLatitude() != null && request.getLongitude() != null) {
            log.info("Using GPS coordinates for location: {}, {}",
                    request.getLatitude(), request.getLongitude());
            return getLocationByGps(request.getLatitude(), request.getLongitude());
        }

        if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            log.info("Using address for location: {}", request.getAddress());
            return getLocationByAddress(request.getAddress());
        }

        if (request.getIpAddress() != null && !request.getIpAddress().isEmpty()) {
            log.info("Using IP address for location: {}", request.getIpAddress());
            return getLocationByIp(request.getIpAddress());
        }

        log.info("Using default location");
        return getDefaultLocation();
    }

    @Override
    public LocationResponse findNearestRegion(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return getDefaultLocation();
        }

        List<Region> allRegions = regionRepository.findAll();

        return allRegions.stream()
                .min(Comparator.comparingDouble(r ->
                        LocationUtils.calculateDistance(
                                latitude, longitude,
                                r.getLatitude(), r.getLongitude())))
                .map(this::convertToResponse)
                .orElseGet(this::getDefaultLocation);
    }

    @Override
    public List<LocationResponse> getMajorCities() {
        return regionRepository.findAll().stream()
                .filter(Region::getIsMajorCity)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private LocationResponse searchSimilarRegion(String sido, String sigungu, String dong) {
        // 유사한 지역 검색
        List<Region> similarRegions = regionRepository.searchRegions(sido, sigungu, dong);

        if (!similarRegions.isEmpty()) {
            return convertToResponse(similarRegions.get(0));
        }

        // 시/도만으로 검색
        List<Region> sidoRegions = regionRepository.findBySido(sido);
        if (!sidoRegions.isEmpty()) {
            return convertToResponse(sidoRegions.get(0));
        }

        return getDefaultLocation();
    }

    private LocationResponse searchRegionByName(String name) {
        // 지역명으로 검색
        List<Region> regions = regionRepository.findBySigunguContaining(name);
        if (!regions.isEmpty()) {
            return convertToResponse(regions.get(0));
        }

        return getDefaultLocation();
    }

    private LocationResponse getDefaultLocation() {
        return regionRepository.findByRegionCode(defaultRegionCode)
                .map(this::convertToResponse)
                .orElseThrow(() -> new RuntimeException("기본 지역 정보를 찾을 수 없습니다."));
    }

    private LocationResponse convertToResponse(Region region) {
        return LocationResponse.builder()
                .regionCode(region.getRegionCode())
                .fullAddress(region.getSido() + " " + region.getSigungu() + " " +
                        (region.getDong() != null ? region.getDong() : ""))
                .sido(region.getSido())
                .sigungu(region.getSigungu())
                .dong(region.getDong())
                .latitude(region.getLatitude())
                .longitude(region.getLongitude())
                .nx(region.getNx())
                .ny(region.getNy())
                .isMajorCity(region.getIsMajorCity())
                .source("DATABASE")
                .build();
    }

    // Response classes
    @lombok.Data
    private static class IpApiResponse {
        private String status;
        private String message;
        private String country;
        private String regionName;
        private String city;
        private Double lat;
        private Double lon;
        private String timezone;
    }

    @lombok.Data
    private static class KakaoResponse {
        private List<KakaoDocument> documents;

        @lombok.Data
        public static class KakaoDocument {
            private String region1DepthName; // 시/도
            private String region2DepthName; // 시/군/구
            private String region3DepthName; // 동/읍/면
            private String addressName;
            private Double x; // 경도
            private Double y; // 위도
        }
    }
}