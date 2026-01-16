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
     * IP 기반 현재 위치의 대기질 정보 조회
     */
    public AirQualityResponseDTO getAirQualityByIp(HttpServletRequest request) {
        String clientIp = locationService.getClientIp(request);
        LocationDTO location = locationService.getLocationByIp(clientIp);

        log.info("IP 기반 대기질 조회: {} -> {}", clientIp, location.getRegionName());

        // 시도명 추출 (예: "서울특별시" → "서울")
        String sidoName = extractSidoName(location.getRegionName());
        List<AirQualityResponseDTO> sidoData = airQualityApiService.getAirQualityBySido(sidoName);

        // 첫 번째 측정소 데이터 반환 (실제로는 근접 측정소 계산 필요)
        return sidoData.isEmpty() ? null : sidoData.get(0);
    }

    /**
     * GPS 좌표 기반 대기질 정보 조회
     */
    public AirQualityResponseDTO getAirQualityByGps(Double latitude, Double longitude) {
        LocationDTO location = locationService.getLocationByGps(latitude, longitude);

        log.info("GPS 기반 대기질 조회: ({}, {}) -> {}",
                latitude, longitude, location.getRegionName());

        String sidoName = extractSidoName(location.getRegionName());

        // TM 좌표로 변환 (간단한 예시, 실제로는 좌표 변환 API 필요)
        Double tmX = longitude * 1.0;
        Double tmY = latitude * 1.0;

        // 근접 측정소 찾기
        String stationName = airQualityApiService.getNearbyStation(tmX, tmY);

        return airQualityApiService.getAirQualityByStation(stationName);
    }

    /**
     * 시도명으로 대기질 정보 조회
     */
    public List<AirQualityResponseDTO> getAirQualityBySido(String sidoName) {
        log.info("시도별 대기질 조회: {}", sidoName);
        return airQualityApiService.getAirQualityBySido(sidoName);
    }

    /**
     * 측정소명으로 대기질 정보 조회
     */
    public AirQualityResponseDTO getAirQualityByStation(String stationName) {
        log.info("측정소별 대기질 조회: {}", stationName);
        return airQualityApiService.getAirQualityByStation(stationName);
    }

    /**
     * 대기질 예보 정보 조회
     */
    public List<AirQualityResponseDTO.AirQualityForecast> getAirQualityForecast(String sidoName) {
        log.info("대기질 예보 조회: {}", sidoName);
        return airQualityApiService.getAirQualityForecast(sidoName);
    }

    /**
     * 다양한 파라미터로 대기질 정보 조회
     */
    public AirQualityResponseDTO getAirQuality(AirQualityRequestDTO requestDto) {
        if (requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            return getAirQualityByGps(requestDto.getLatitude(), requestDto.getLongitude());
        } else if (requestDto.getStationName() != null) {
            return getAirQualityByStation(requestDto.getStationName());
        } else if (requestDto.getSidoName() != null) {
            List<AirQualityResponseDTO> sidoData = getAirQualityBySido(requestDto.getSidoName());
            return sidoData.isEmpty() ? null : sidoData.get(0);
        } else {
            throw new IllegalArgumentException("시도명, 측정소명 또는 좌표 정보가 필요합니다.");
        }
    }

    /**
     * 지역명에서 시도명 추출
     */
    private String extractSidoName(String regionName) {
        if (regionName == null || regionName.contains("?")) return "서울";
        Map<String, String> sidoMap = new HashMap<>();
        sidoMap.put("Seoul","서울");
        sidoMap.put("Busan","부산");
        sidoMap.put("Daegu","대구");
        sidoMap.put("Incheon","인천");
        sidoMap.put("Gwangju","광주");
        sidoMap.put("Daejeon","대전");
        sidoMap.put("Ulsan","울산");
        sidoMap.put("Gyeonggi","경기");
        sidoMap.put("Gangwon","강원");
        sidoMap.put("Chungbuk","충북");
        sidoMap.put("Chungnam","충남");
        sidoMap.put("Jeonbuk","전북");
        sidoMap.put("Jeonnam","전남");
        sidoMap.put("Gyeongbuk","경북");
        sidoMap.put("Gyeongnam","경남");
        sidoMap.put("Jeju","제주");
        sidoMap.put("Sejong","세종");
        return sidoMap.getOrDefault(regionName, "서울");
    }
}

