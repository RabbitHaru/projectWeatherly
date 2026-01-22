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

        private String dataTime; // 발표 시각

        // ⭐ [추가] 서비스에서 사용하려면 이 필드가 꼭 있어야 해!
        private String informCode;

        private String date;
        private String overallGrade;
        private String pm10Grade;
        private String pm25Grade;
        private String advice;
        private String cause;
    }
}