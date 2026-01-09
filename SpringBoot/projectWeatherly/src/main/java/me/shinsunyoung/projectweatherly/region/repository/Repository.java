package me.shinsunyoung.projectweatherly.region.repository;

import me.shinsunyoung.projectweatherly.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public class Repository extends JpaRepository {

    List<Region> findAllByIsActiveTrue();
}
