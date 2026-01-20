package me.shinsunyoung.projectweatherly.airquality.repository;

import me.shinsunyoung.projectweatherly.airquality.entity.AirQualityForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirQualityForecastRepository extends JpaRepository<AirQualityForecastEntity, Long> {

    // 1. 예보 조회용: 최신순으로 2개 가져오기 (오늘, 내일)
    List<AirQualityForecastEntity> findTop2ByOrderByRecordedAtDesc();

    // 2. [추가됨] 중복 체크용: 가장 최신 1개만 가져오기
    AirQualityForecastEntity findTopByOrderByRecordedAtDesc();
}