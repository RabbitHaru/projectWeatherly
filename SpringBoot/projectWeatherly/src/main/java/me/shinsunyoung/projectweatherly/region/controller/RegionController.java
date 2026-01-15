package me.shinsunyoung.projectweatherly.region.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.region.service.RegionService;
import me.shinsunyoung.projectweatherly.region.dto.RegionResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // 전체 지역 조회
    @GetMapping
    public List<RegionResponseDTO> getRegions() {
        return regionService.findAll();
    }

    // 활성 지역만 조회
    @GetMapping("/active")
    public List<RegionResponseDTO> getActiveRegions() {
        return regionService.findActiveRegions();
    }

    // 지역 코드로 단건 조회
    @GetMapping("/{code}")
    public RegionResponseDTO getRegion(@PathVariable String code) {
        return regionService.findByCode(code);
    }
}
