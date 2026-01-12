package me.shinsunyoung.projectweatherly.region.Service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.region.domain.Region;
import me.shinsunyoung.projectweatherly.region.dto.RegionResponseDTO;
import me.shinsunyoung.projectweatherly.region.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        return regionRepository.findById(regionCode)
                .map(RegionResponseDTO::from)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 지역 코드입니다"));
    }

    // 활성 지역만 조회
    public List<RegionResponseDTO> findActiveRegions() {
        return regionRepository.findByActiveTrue()
                .stream()
                .map(RegionResponseDTO::from)
                .toList();
    }
}

