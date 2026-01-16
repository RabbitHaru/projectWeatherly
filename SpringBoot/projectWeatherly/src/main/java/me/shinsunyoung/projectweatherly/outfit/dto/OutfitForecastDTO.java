package me.shinsunyoung.projectweatherly.outfit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OutfitForecastDTO {

    private String date;        // "10/20"
    private String dayOfWeek;   // "금"
    private Double minTemp;
    private Double maxTemp;

    // 옷차림 결과
    private int level;
    private String levelText;
    private String mainName;
    private String tip;         // "가벼운 자켓 준비"
}
