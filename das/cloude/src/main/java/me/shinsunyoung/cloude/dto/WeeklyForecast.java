package me.shinsunyoung.cloude.dto;

public class WeeklyForecast {

    private String day;        // 요일
    private String condition;  // 날씨 상태 (예: 맑음, 비, 구름 등)
    private Integer high;      // 최고 기온
    private Integer low;       // 최저 기온

    // Getter, Setter
    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Integer getHigh() {
        return high;
    }

    public void setHigh(Integer high) {
        this.high = high;
    }

    public Integer getLow() {
        return low;
    }

    public void setLow(Integer low) {
        this.low = low;
    }
}
