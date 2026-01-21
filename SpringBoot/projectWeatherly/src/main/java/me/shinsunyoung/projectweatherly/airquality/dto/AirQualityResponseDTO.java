package me.shinsunyoung.projectweatherly.airquality.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AirQualityResponseDTO {

    private String sidoName;
    private String stationName;
    private Object dataTime; // String or LocalDateTime

    private AirQualityIndex pm10;
    private AirQualityIndex pm25;
    private AirQualityIndex o3;
    private AirQualityIndex no2;
    private AirQualityIndex co;
    private AirQualityIndex so2;
    private AirQualityIndex khai;

    private String overallGrade;
    private String overallStatus;
    private String healthAdvice;

    @Getter
    @Builder
    public static class AirQualityIndex {
        private int value;
        private String grade;
        private String status;
        private String unit;
    }

    @Getter
    @Builder
    public static class AirQualityForecast {

        // [추가됨] 발표 시각
        private String dataTime;

        private String date;
        private String overallGrade;
        private String pm10Grade;
        private String pm25Grade;
        private String advice;
        private String cause;
    }
}