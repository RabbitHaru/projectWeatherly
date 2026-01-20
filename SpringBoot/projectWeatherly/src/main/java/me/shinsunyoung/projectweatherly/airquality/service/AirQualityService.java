package me.shinsunyoung.projectweatherly.airquality.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityRequestDTO;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private final AirQualityApiService airQualityApiService;
    private final LocationService locationService;

    /**
     * IP 기반 대기질 조회
     */
    public AirQualityResponseDTO getAirQualityByIp(HttpServletRequest request) {
        String clientIp = locationService.getClientIp(request);
        String regionName = null;

        if (clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1")) {
            regionName = "서울특별시"; // [수정] 기본값 서울특별시로 통일
        } else {
            LocationDTO location = locationService.getLocationByIp(clientIp);
            if (location != null) {
                regionName = location.getRegionName();
            }
        }

        if (regionName == null) regionName = "서울특별시"; // [수정]

        return fetchRealData(regionName);
    }

    /**
     * GPS 기반 대기질 조회
     */
    public AirQualityResponseDTO getAirQualityByGps(Double latitude, Double longitude) {
        LocationDTO location = locationService.getLocationByGps(latitude, longitude);
        // [수정] GPS 위치 못 찾았을 때 기본값 서울특별시로 통일
        String regionName = (location != null) ? location.getRegionName() : "서울특별시";

        Double tmX = longitude * 1.0;
        Double tmY = latitude * 1.0;
        String stationName = airQualityApiService.getNearbyStation(tmX, tmY);

        log.info("GPS 조회: {} -> 측정소: {}", regionName, stationName);

        AirQualityResponseDTO dto = airQualityApiService.getAirQualityByStation(stationName);

        if (dto != null) {
            dto.setSidoName(extractSidoName(regionName));
        }

        return dto;
    }

    private AirQualityResponseDTO fetchRealData(String regionName) {
        String sidoName = extractSidoName(regionName);
        List<AirQualityResponseDTO> list = airQualityApiService.getAirQualityBySido(sidoName);

        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public List<AirQualityResponseDTO> getAirQualityBySido(String sidoName) {
        return airQualityApiService.getAirQualityBySido(sidoName);
    }

    public AirQualityResponseDTO getAirQualityByStation(String stationName) {
        return airQualityApiService.getAirQualityByStation(stationName);
    }

    public List<AirQualityResponseDTO.AirQualityForecast> getAirQualityForecast(String sidoName) {
        return airQualityApiService.getAirQualityForecast(sidoName);
    }

    public AirQualityResponseDTO getAirQuality(AirQualityRequestDTO requestDto) {
        if (requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            return getAirQualityByGps(requestDto.getLatitude(), requestDto.getLongitude());
        } else if (requestDto.getStationName() != null) {
            return getAirQualityByStation(requestDto.getStationName());
        } else if (requestDto.getSidoName() != null) {
            return fetchRealData(requestDto.getSidoName());
        }
        throw new IllegalArgumentException("필수 파라미터 누락");
    }

    private String extractSidoName(String regionName) {
        if (regionName == null) return "서울";
        Map<String, String> map = new HashMap<>();
        map.put("Seoul", "서울");
        map.put("Busan", "부산");
        map.put("Daegu", "대구");
        map.put("Incheon", "인천");
        map.put("Gwangju", "광주");
        map.put("Daejeon", "대전");
        map.put("Ulsan", "울산");
        map.put("Gyeonggi", "경기");
        map.put("Gangwon", "강원");
        map.put("Chungbuk", "충북");
        map.put("Chungnam", "충남");
        map.put("Jeonbuk", "전북");
        map.put("Jeonnam", "전남");
        map.put("Gyeongbuk", "경북");
        map.put("Gyeongnam", "경남");
        map.put("Jeju", "제주");
        map.put("Sejong", "세종");
        map.put("서울특별시", "서울");
        map.put("부산광역시", "부산");
        map.put("경기도", "경기");

        for (String key : map.keySet()) {
            if (regionName.contains(key)) return map.get(key);
        }
        return (regionName.length() >= 2) ? regionName.substring(0, 2) : regionName;
    }
}