package me.shinsunyoung.projectweatherly.region.repository;

import me.shinsunyoung.projectweatherly.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, String> {

    List<Region> findByActiveTrue();

    Optional<Region> findByRegionCodeAndActiveTrue(String regionCode);
}

