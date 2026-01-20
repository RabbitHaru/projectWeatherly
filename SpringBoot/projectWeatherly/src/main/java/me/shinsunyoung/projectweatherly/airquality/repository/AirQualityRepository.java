package me.shinsunyoung.projectweatherly.airquality.repository;

import me.shinsunyoung.projectweatherly.airquality.entity.AirQualityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirQualityRepository extends JpaRepository<AirQualityEntity, Long> {
    // 특정 시도의 데이터를 측정시간 내림차순(최신순)으로 조회
    List<AirQualityEntity> findBySidoNameOrderByDataTimeDesc(String sidoName);
}