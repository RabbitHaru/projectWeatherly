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

    // 오늘 시간별 예보 (1시간 간격)
    private List<HourlyForecast> hourly;

    // 내일 시간별 예보 (1시간 간격)
    private List<HourlyForecast> tomorrowHourly;

    // 주간 예보 (오전/오후 구분)
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
        private String time;                    // "오전 8시", "오후 2시"
        private Double temperature;
        private String weatherCondition;
        private String weatherIcon;
        private Double precipitationProbability;
        private Double humidity;
        private Double windSpeed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private String date;                    // "MM/dd"
        private String dayOfWeek;               // "월", "화"
        private Double maxTemp;                 // 최고기온
        private Double minTemp;                 // 최저기온
        private Double amTemp;                  // 오전 기온
        private Double pmTemp;                  // 오후 기온
        private String dayWeather;              // 오후 날씨
        private String nightWeather;            // 오전 날씨
        private String dayIcon;                 // 오후 아이콘
        private String nightIcon;               // 오전 아이콘
        private Double precipitationProbability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherSummary {
        private String ultraShortSummary;       // 오늘 날씨 추이
        private String shortSummary;            // 내일 예보
        private String midSummary;              // 이번 주 예보
    }
}