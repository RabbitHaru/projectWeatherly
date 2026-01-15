package me.shinsunyoung.projectweatherly.region.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegionResponseDTO {
    private String regionCode; // 법정동 코드
    private String regionName; // 법정동 이름
    private boolean active; // 폐지유무 확인
    private int nx; // x좌표 값
    private int ny; // y좌표 값

    public static RegionResponseDTO from(javax.swing.plaf.synth.Region region) {
        return RegionResponseDTO.builder()
                .regionCode(region.getRegionCode())
                .regionName(region.getRegionName())
                .active(region.isActive())
                .build();
    }
}
