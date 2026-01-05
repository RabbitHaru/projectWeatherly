package me.shinsunyoung.cloude.dto;

import java.util.List;

public class WeatherDto {
    private String region;
    private Integer temp;
    private String condition;
    private Integer feelsLike;
    private Integer humidity;
    private Integer precipitation;
    private List<HourlyForecast> hourly;  // hourly 추가
    private List<WeeklyForecast> weekly;

    // Getter, Setter 추가
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Integer getTemp() {
        return temp;
    }

    public void setTemp(Integer temp) {
        this.temp = temp;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Integer getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(Integer feelsLike) {
        this.feelsLike = feelsLike;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Integer getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(Integer precipitation) {
        this.precipitation = precipitation;
    }

    public List<HourlyForecast> getHourly() {
        return hourly;
    }

    public void setHourly(List<HourlyForecast> hourly) {
        this.hourly = hourly;
    }

    public List<WeeklyForecast> getWeekly() {
        return weekly;
    }

    public void setWeekly(List<WeeklyForecast> weekly) {
        this.weekly = weekly;
    }
}
