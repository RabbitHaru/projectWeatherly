package me.shinsunyoung.projectweatherly.airquality.dto;

import lombok.Data;

@Data
public class AirQualityRequestDTO {
    private String sidoName;        // 시도명 (예: 서울, 부산)
    private String stationName;     // 측정소명 (옵션)
    private Double latitude;        // 위도 (GPS용)
    private Double longitude;       // 경도 (GPS용)
}