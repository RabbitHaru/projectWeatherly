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

    // [추가됨] 가상 데이터 여부 (true면 화면에 표시)
    @Builder.Default
    private boolean isMock = false;

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

    // 간략 모드용
    public static WeatherResponseDTO createLiteVersion(WeatherResponseDTO full) {
        return WeatherResponseDTO.builder()
                .regionName(full.getRegionName())
                .regionCode(full.getRegionCode())
                .currentTime(full.getCurrentTime())
                .current(full.getCurrent())
                .summary(full.getSummary())
                .isMock(full.isMock()) // 상태 복사
                .build();
    }

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