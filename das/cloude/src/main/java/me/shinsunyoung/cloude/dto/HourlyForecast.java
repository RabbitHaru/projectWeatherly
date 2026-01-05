package me.shinsunyoung.cloude.dto;

public class HourlyForecast {
    private String time;  // 시간
    private Integer temp;  // 기온

    // Getter, Setter
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getTemp() {
        return temp;
    }

    public void setTemp(Integer temp) {
        this.temp = temp;
    }
}
