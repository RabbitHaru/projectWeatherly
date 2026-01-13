package me.shinsunyoung.projectweatherly.clothing.service;

import me.shinsunyoung.projectweatherly.clothing.enums.TemperatureRange;
import me.shinsunyoung.projectweatherly.clothing.model.ClothingSet;
import me.shinsunyoung.projectweatherly.clothing.policy.ClothingPolicy;
import org.springframework.stereotype.Service;

@Service
public class ClothingRecommendationService {

    public ClothingSet recommend(double temperature) {

        TemperatureRange range =
                TemperatureRangeResolver.resolve(temperature);

        ClothingSet clothingSet =
                ClothingPolicy.POLICY.get(range);

        if (clothingSet == null) {
            throw new IllegalStateException("옷 추천 정책이 없습니다.");
        }

        return clothingSet;
    }
}
