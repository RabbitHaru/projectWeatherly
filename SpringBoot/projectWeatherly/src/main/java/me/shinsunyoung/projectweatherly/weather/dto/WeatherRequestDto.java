package me.shinsunyoung.projectweatherly.weather.dto;

import lombok.Data;

@Data
public class WeatherRequestDto {
    private String regionCode;      // 지역 코드
    private String regionName;      // 지역 이름
    private Double latitude;        // 위도 (GPS용)
    private Double longitude;       // 경도 (GPS용)
    private String forecastType;    // 예보 타입 (current, hourly, daily)
}