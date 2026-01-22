package me.shinsunyoung.projectweatherly.airquality.repository;

import me.shinsunyoung.projectweatherly.airquality.entity.AirQualityForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirQualityForecastRepository extends JpaRepository<AirQualityForecastEntity, Long> {

    // 1. 예보 조회용: 최신순 2개 (오늘, 내일)
    List<AirQualityForecastEntity> findTop2ByOrderByRecordedAtDesc();

    // 2. [수정] 중복 확인용: 시간(dataTime)으로만 찾기
    Optional<AirQualityForecastEntity> findByDataTime(String dataTime);
}