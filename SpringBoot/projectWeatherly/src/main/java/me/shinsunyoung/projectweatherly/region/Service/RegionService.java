package me.shinsunyoung.projectweatherly.region.Service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.region.domain.Region;
import me.shinsunyoung.projectweatherly.region.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    public Region findByCode(String regionCode) {
        return regionRepository.findById(regionCode)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 지역 코드입니다: " + regionCode)
                );

    }
    public List<Region> findActiveRegions() {
        return regionRepository.findByIsActiveTrue();
    }

}

