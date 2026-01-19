package me.shinsunyoung.projectweatherly.outfit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OutfitConditionDTO {

    private Double feelsLike;        // 체감온도
    private double windSpeed;     // 풍속 (m/s)
    private double humidity;      // 습도 (%)
    private double precipitation; // 강수량 (mm)
    private String condition;     // 날씨 상태 (맑음, 비, 눈 등)
}
