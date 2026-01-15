package me.shinsunyoung.projectweatherly.region.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.region.domain.Region;
import me.shinsunyoung.projectweatherly.region.dto.RegionResponseDTO;
import me.shinsunyoung.projectweatherly.region.repository.RegionRepository;
import me.shinsunyoung.projectweatherly.common.error.CustomException;
import me.shinsunyoung.projectweatherly.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    // 전체 조회
    public List<RegionResponseDTO> findAll() {
        return regionRepository.findAll()
                .stream()
                .map(RegionResponseDTO::from)
                .toList();
    }

    // 단건 조회
    public RegionResponseDTO findByCode(String regionCode) {
        return regionRepository.findByRegionCodeAndActiveTrue(regionCode)
                .map(RegionResponseDTO::from)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND));
    }

    // 활성 지역 목록
    public List<RegionResponseDTO> findActiveRegions() {
        return regionRepository.findByActiveTrue()
                .stream()
                .map(RegionResponseDTO::from)
                .toList();
    }

    // WeatherQuery용
    public Region getActiveRegion(String regionCode) {
        return regionRepository.findByRegionCodeAndActiveTrue(regionCode)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND));
    }
}
