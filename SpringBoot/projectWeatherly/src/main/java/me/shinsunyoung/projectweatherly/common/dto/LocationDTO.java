package me.shinsunyoung.projectweatherly.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private Double latitude;    // 위도
    private Double longitude;   // 경도
    private String regionName;  // 지역명 (예: 서울특별시)
    private String regionCode;  // 지역코드 (예: 1100000000)
    private String address;     // 상세 주소

    // IP 기반 위치 정보용
    private String ipAddress;
    private String city;
    private String country;
}