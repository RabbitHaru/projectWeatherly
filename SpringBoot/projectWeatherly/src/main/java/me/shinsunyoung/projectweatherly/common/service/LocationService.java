package me.shinsunyoung.projectweatherly.common.service;

import com.weatherly.common.dto.LocationRequest;
import com.weatherly.common.dto.LocationResponse;

public interface LocationService {

    /**
     * IP 주소로 위치 조회
     */
    LocationResponse getLocationByIp(String ipAddress);

    /**
     * GPS 좌표로 위치 조회
     */
    LocationResponse getLocationByGps(Double latitude, Double longitude);

    /**
     * 주소로 위치 조회
     */
    LocationResponse getLocationByAddress(String address);

    /**
     * 지역 코드로 위치 조회
     */
    LocationResponse getLocationByRegionCode(String regionCode);

    /**
     * 시/도/구/동으로 위치 조회
     */
    LocationResponse getLocationByAddress(String sido, String sigungu, String dong);

    /**
     * 사용자 위치 자동 결정 (IP -> GPS -> 기본)
     */
    LocationResponse determineUserLocation(LocationRequest request);

    /**
     * 가장 가까운 지역 찾기
     */
    LocationResponse findNearestRegion(Double latitude, Double longitude);

    /**
     * 대도시 목록 조회
     */
    java.util.List<LocationResponse> getMajorCities();
}