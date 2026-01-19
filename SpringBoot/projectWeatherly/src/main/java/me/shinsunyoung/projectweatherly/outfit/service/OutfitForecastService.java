package me.shinsunyoung.projectweatherly.outfit.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.outfit.dto.OutfitConditionDTO;
import me.shinsunyoung.projectweatherly.outfit.dto.OutfitForecastDTO;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutfitForecastService {

    private final OutfitService outfitService;

    public List<OutfitForecastDTO> createForecasts(
            List<WeatherResponseDTO.DailyForecast> dailyList
    ) {

        if (dailyList == null || dailyList.isEmpty()) {
            return List.of();
        }

        return dailyList.stream()
                .skip(1)  // [수정] 오늘(첫번째) 데이터 건너뛰기
                .limit(3) // [수정] 내일, 모레, 글피 (딱 3일치만)
                .map(this::convert)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ... (아래 convert 메서드 등은 기존과 동일) ...
    private OutfitForecastDTO convert(WeatherResponseDTO.DailyForecast daily) {
        if (daily.getMinTemp() == null || daily.getMaxTemp() == null) {
            return null;
        }

        double avgTemp = (daily.getMinTemp() + daily.getMaxTemp()) / 2.0;

        String weather = daily.getDayWeather() != null
                ? daily.getDayWeather()
                : "맑음";

        OutfitConditionDTO condition = new OutfitConditionDTO(
                avgTemp,
                0.0,
                0.0,
                0.0,
                weather
        );

        Map<String, Object> result = outfitService.decide(condition);
        int level = ((Number) result.get("level")).intValue();

        return new OutfitForecastDTO(
                daily.getDate(),
                daily.getDayOfWeek(),
                daily.getMinTemp(),
                daily.getMaxTemp(),
                level,
                (String) result.get("levelText"),
                (String) result.get("mainName"),
                createTip(level)
        );
    }

    private String createTip(int level) {
        return switch (level) {
            case 1 -> "시원한 옷차림 추천";
            case 2 -> "가벼운 겉옷 준비";
            case 3 -> "일교차 주의";
            case 4 -> "보온이 필요해요";
            default -> "두꺼운 옷 필수";
        };
    }
}