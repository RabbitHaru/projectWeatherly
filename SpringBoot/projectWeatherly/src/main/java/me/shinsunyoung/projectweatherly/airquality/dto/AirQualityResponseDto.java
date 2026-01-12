package me.shinsunyoung.projectweatherly.airquality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityResponseDto {

    // 기본 정보
    private String stationName;
    private String sidoName;
    private LocalDateTime dataTime;

    // 대기질 지수
    private AirQualityIndex khai;          // 통합대기환경지수
    private AirQualityIndex pm10;          // 미세먼지
    private AirQualityIndex pm25;          // 초미세먼지
    private AirQualityIndex o3;            // 오존
    private AirQualityIndex no2;           // 이산화질소
    private AirQualityIndex co;            // 일산화탄소
    private AirQualityIndex so2;           // 아황산가스

    // 요약 정보
    private String overallGrade;           // 전체 등급
    private String overallStatus;          // 전체 상태 (좋음, 보통 등)
    private String healthAdvice;           // 건강 조언

    // 예보 정보
    private List<AirQualityForecast> forecasts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirQualityIndex {
        private Integer value;             // 측정값
        private String grade;              // 등급 (1~4)
        private String status;             // 상태 (좋음, 보통 등)
        private String unit;               // 단위
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirQualityForecast {
        private String date;               // 예보 날짜
        private String overallGrade;       // 전체 등급
        private String pm10Grade;          // PM10 등급
        private String pm25Grade;          // PM2.5 등급
        private String advice;             // 생활지수 조언
    }
}