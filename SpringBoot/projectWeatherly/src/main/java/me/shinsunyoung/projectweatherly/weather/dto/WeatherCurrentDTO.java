package me.shinsunyoung.projectweatherly.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeatherCurrentDTO {
    private int temperature;
    private int feelsLike;
    private Double windSpeed;
    private Integer humidity;
    private String weatherCondition;
}
