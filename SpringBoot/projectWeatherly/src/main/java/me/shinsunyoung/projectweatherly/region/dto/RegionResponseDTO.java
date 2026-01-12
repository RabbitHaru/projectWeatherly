package me.shinsunyoung.projectweatherly.region.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import me.shinsunyoung.projectweatherly.region.domain.Region;

@Getter
@Builder
public class RegionResponseDTO {
    private String regionCode;
    private String regionName;
    private boolean active;

    public static RegionResponseDTO from(Region region) {
        return RegionResponseDTO.builder()
                .regionCode(region.getRegionCode())
                .regionName(region.getRegionName())
                .active(region.isActive())
                .build();
    }
}
