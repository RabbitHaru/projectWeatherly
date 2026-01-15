package me.shinsunyoung.projectweatherly.clothing.policy;

import me.shinsunyoung.projectweatherly.clothing.enums.TemperatureRange;
import me.shinsunyoung.projectweatherly.clothing.model.ClothingSet;

import java.util.List;
import java.util.Map;

public class ClothingPolicy {

    public static final Map<TemperatureRange, ClothingSet> POLICY = Map.of(

            TemperatureRange.HOT,
            new ClothingSet(
                    List.of(),
                    List.of("민소매", "반팔 티셔츠"),
                    List.of("반바지", "짧은 치마")
            ),

            TemperatureRange.WARM,
            new ClothingSet(
                    List.of(),
                    List.of("반팔 티셔츠", "얇은 셔츠"),
                    List.of("반바지", "면바지")
            ),

            TemperatureRange.MILD_WARM,
            new ClothingSet(
                    List.of("얇은 가디건", "셔츠", "블라우스"),
                    List.of("긴팔 티셔츠"),
                    List.of("면바지", "슬랙스", "청바지")
            ),

            TemperatureRange.MILD,
            new ClothingSet(
                    List.of("얇은 니트", "가디건", "재킷", "바람막이"),
                    List.of("후드티", "맨투맨"),
                    List.of("긴바지", "청바지", "슬랙스", "스키니진")
            ),

            TemperatureRange.COOL,
            new ClothingSet(
                    List.of("재킷", "가디건"),
                    List.of("맨투맨", "셔츠"),
                    List.of("청바지", "면바지")
            ),

            TemperatureRange.COLD_COOL,
            new ClothingSet(
                    List.of("재킷", "야상", "점퍼", "트렌치 코트"),
                    List.of("맨투맨", "두꺼운 긴팔", "얇은 니트"),
                    List.of("청바지", "면바지", "기모 바지")
            ),

            TemperatureRange.COLD,
            new ClothingSet(
                    List.of("코트", "가죽 재킷"),
                    List.of("니트", "두꺼운 맨투맨", "기모 셔츠"),
                    List.of("청바지", "두꺼운 바지", "기모 바지", "레깅스")
            ),

            TemperatureRange.FREEZING,
            new ClothingSet(
                    List.of("패딩", "두꺼운 코트"),
                    List.of("히트텍", "두꺼운 니트", "기모 맨투맨"),
                    List.of("기모 바지", "기모 청바지", "두꺼운 슬랙스")
            )
    );

    private ClothingPolicy() {
    }
}

