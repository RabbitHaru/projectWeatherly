package me.shinsunyoung.projectweatherly.outfit.service;

import me.shinsunyoung.projectweatherly.outfit.enums.TemperatureRange;
import me.shinsunyoung.projectweatherly.outfit.model.OutfitSet;
import me.shinsunyoung.projectweatherly.outfit.policy.OutfitPolicy;
import org.springframework.stereotype.Service;

@Service
public class OutfitRecommendationService {

    public OutfitSet recommend(double temperature) {

        TemperatureRange range =
                TemperatureRangeResolver.resolve(temperature);

        OutfitSet outfitSet =
                OutfitPolicy.POLICY.get(range);

        if (outfitSet == null) {
            throw new IllegalStateException("옷 추천 정책이 없습니다.");
        }

        return outfitSet;
    }
}
