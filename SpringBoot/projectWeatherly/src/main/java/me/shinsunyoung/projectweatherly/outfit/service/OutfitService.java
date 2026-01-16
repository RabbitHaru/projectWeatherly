package me.shinsunyoung.projectweatherly.outfit.service;

import me.shinsunyoung.projectweatherly.outfit.dto.OutfitConditionDTO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OutfitService {

    public Map<String, Object> decide(OutfitConditionDTO condition) {

        int feelsLike = condition.getFeelsLike().intValue();

        int level;
        String levelText;
        String mainName;
        String summary;

        if (feelsLike >= 28) {
            level = 1;
            levelText = "반팔, 민소매 추천";
            mainName = "반팔 티셔츠";
            summary = "더운 날씨로 가볍게 입는 것이 좋아요.";
        } else if (feelsLike >= 23) {
            level = 2;
            levelText = "가벼운 겉옷 필요";
            mainName = "반팔 티셔츠";
            summary = "일교차를 대비해 얇은 겉옷을 준비하세요.";
        } else if (feelsLike >= 18) {
            level = 3;
            levelText = "긴팔 추천";
            mainName = "긴팔 티셔츠";
            summary = "선선한 날씨로 긴팔이 적당해요.";
        } else if (feelsLike >= 13) {
            level = 4;
            levelText = "자켓, 니트";
            mainName = "자켓";
            summary = "쌀쌀하니 겉옷이 필요해요.";
        } else {
            level = 5;
            levelText = "코트, 패딩";
            mainName = "코트";
            summary = "추운 날씨로 보온에 신경 쓰세요.";
        }

        return Map.of(
                "level", level,
                "levelText", levelText,
                "mainName", mainName,
                "summary", summary
        );
    }
}

