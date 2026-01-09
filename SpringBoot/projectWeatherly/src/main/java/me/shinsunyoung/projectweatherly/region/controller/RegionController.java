package me.shinsunyoung.projectweatherly.region.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.region.Service.RegionService;
import me.shinsunyoung.projectweatherly.region.domain.Region;
import me.shinsunyoung.projectweatherly.region.repository.RegionRepository;
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

    @GetMapping("/{regionCode}")
    public Region getRegion(@PathVariable String regionCode) {
        return regionService.findByCode(regionCode);
    }
    @GetMapping("/active")
    public List<Region> getActiveRegions() {
        return regionService.findActiveRegions();
    }

}
