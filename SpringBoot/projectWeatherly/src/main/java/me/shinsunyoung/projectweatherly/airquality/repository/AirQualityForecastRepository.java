package me.shinsunyoung.projectweatherly.airquality.repository;

import me.shinsunyoung.projectweatherly.airquality.entity.AirQualityForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirQualityForecastRepository extends JpaRepository<AirQualityForecastEntity, Long> {
    // 가장 최근에 저장된 예보 데이터 2개(오늘/내일) 조회
    List<AirQualityForecastEntity> findTop2ByOrderByRecordedAtDesc();
}