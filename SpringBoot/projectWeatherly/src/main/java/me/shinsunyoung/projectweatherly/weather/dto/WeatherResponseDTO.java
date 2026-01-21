package me.shinsunyoung.projectweatherly.weather.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherResponseDTO {

    // 기본 정보
    private String regionName;
    private String regionCode;
    private String currentTime;

    @Builder.Default
    private boolean isMock = false;

    // [추가됨] 기상특보 정보
    private WeatherWarning warning;

    private CurrentWeather current;
    private List<HourlyForecast> hourly;
    private List<HourlyForecast> tomorrowHourly;
    private List<DailyForecast> daily;
    private WeatherSummary summary;

    public static WeatherResponseDTO createLiteVersion(WeatherResponseDTO full) {
        return WeatherResponseDTO.builder()
                .regionName(full.getRegionName())
                .regionCode(full.getRegionCode())
                .currentTime(full.getCurrentTime())
                .current(full.getCurrent())
                .summary(full.getSummary())
                .isMock(full.isMock())
                .warning(full.getWarning()) // 특보 정보도 복사
                .build();
    }

    // [추가됨] 특보 정보 클래스
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherWarning {
        private boolean active;      // 특보 발효 여부
        private String title;        // 제목 (예: 호우주의보)
        private String description;  // 설명 (예: 시간당 30mm 이상의 강한 비)
        private String level;        // 등급 (safe, caution, danger)
    }

    // ... (CurrentWeather, HourlyForecast, DailyForecast, WeatherSummary는 기존 그대로 유지)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HourlyForecast {
        private String time;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DailyForecast {
        private String date;
        private String dayOfWeek;
        private Double maxTemp;
        private Double minTemp;
        private Double amTemp;
        private Double pmTemp;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WeatherSummary {
        private String ultraShortSummary;
        private String shortSummary;
        private String midSummary;
    }
}