package me.shinsunyoung.projectweatherly.weather.dto;

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
public class WeatherResponseDTO {

    // 기본 정보
    private String regionName;
    private String regionCode;
    private String currentTime;

    // 현재 날씨
    private CurrentWeather current;

    // 시간별 예보
    private List<HourlyForecast> hourly;

    // 일별 예보
    private List<DailyForecast> daily;

    // 요약 정보
    private WeatherSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeather {
        private Double temperature;
        private Double feelsLike;
        private Double humidity;
        private Double windSpeed;
        private String windDirection;
        private Double precipitation;
        private String weatherCondition;
        private String weatherIcon;
        private LocalDateTime updateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyForecast {
        private String time;
        private Double temperature;
        private String weatherCondition;
        private String weatherIcon;
        private Double precipitationProbability;
        private Double humidity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private String date;
        private String dayOfWeek;
        private Double maxTemp;
        private Double minTemp;
        private String dayWeather;
        private String nightWeather;
        private String dayIcon;
        private String nightIcon;
        private Double precipitationProbability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherSummary {
        private String ultraShortSummary;   // 초단기예보 요약
        private String shortSummary;        // 단기예보 요약
        private String midSummary;          // 중기예보 요약
    }
}