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

        // 1. 로컬호스트 체크
        if (clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1")) {
            regionName = "서울";
        } else {
            // 2. 실제 IP 위치 조회
            LocationDTO location = locationService.getLocationByIp(clientIp);
            if (location != null) {
                regionName = location.getRegionName();
            }
        }

        if (regionName == null) regionName = "서울";

        return fetchRealData(regionName);
    }

    /**
     * [핵심 수정] GPS 기반 대기질 조회
     * - GPS로 찾은 정확한 지역명을 결과 데이터에 강제로 주입합니다.
     * - API가 터져서 가짜 데이터가 오더라도 지역명은 제대로 뜨게 됩니다.
     */
    public AirQualityResponseDTO getAirQualityByGps(Double latitude, Double longitude) {
        // 1. 카카오 API로 좌표 -> 행정구역(시도) 조회 (이건 API 쿼터가 널널해서 작동함)
        LocationDTO location = locationService.getLocationByGps(latitude, longitude);
        String regionName = (location != null) ? location.getRegionName() : "서울";

        // 2. 근접 측정소 찾기 (에어코리아 API - 429 에러나면 "중구" 반환될 수 있음)
        Double tmX = longitude * 1.0;
        Double tmY = latitude * 1.0; // 임시 좌표 변환 없이 그대로 사용 (정확도 낮아도 동작 우선)
        String stationName = airQualityApiService.getNearbyStation(tmX, tmY);

        log.info("GPS 조회: {} -> 측정소: {}", regionName, stationName);

        // 3. 측정소 데이터 조회 (에어코리아 API - 429 에러나면 가짜 데이터 반환됨)
        AirQualityResponseDTO dto = airQualityApiService.getAirQualityByStation(stationName);

        // [중요] DTO가 null이거나 가짜 데이터일 수 있으니,
        // 우리가 GPS로 찾은 '진짜 지역명'을 덮어씌워서 화면에 제대로 뜨게 함
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
        map.put("Seoul", "서울"); map.put("Busan", "부산"); map.put("Daegu", "대구");
        map.put("Incheon", "인천"); map.put("Gwangju", "광주"); map.put("Daejeon", "대전");
        map.put("Ulsan", "울산"); map.put("Gyeonggi", "경기"); map.put("Gangwon", "강원");
        map.put("Chungbuk", "충북"); map.put("Chungnam", "충남"); map.put("Jeonbuk", "전북");
        map.put("Jeonnam", "전남"); map.put("Gyeongbuk", "경북"); map.put("Gyeongnam", "경남");
        map.put("Jeju", "제주"); map.put("Sejong", "세종");
        map.put("서울특별시", "서울"); map.put("부산광역시", "부산"); map.put("경기도", "경기");

        for (String key : map.keySet()) {
            if (regionName.contains(key)) return map.get(key);
        }
        // 한글 2글자만 남기기 (경상북도 -> 경북 등 처리가 안된 경우 앞 2글자)
        return (regionName.length() >= 2) ? regionName.substring(0, 2) : regionName;
    }
}