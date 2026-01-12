package me.shinsunyoung.projectweatherly.airquality.repository;

import me.shinsunyoung.projectweatherly.airquality.entity.AirQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AirQualityRepository extends JpaRepository<AirQuality, Long> {

    Optional<AirQuality> findTopByStationNameOrderByDataTimeDesc(String stationName);

    List<AirQuality> findBySidoNameAndDataTimeBetween(
            String sidoName,
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT aq FROM AirQuality aq WHERE aq.sidoName = :sidoName " +
            "AND aq.dataTime >= :dateTime " +
            "ORDER BY aq.dataTime DESC")
    List<AirQuality> findRecentBySido(
            @Param("sidoName") String sidoName,
            @Param("dateTime") LocalDateTime dateTime);

    List<AirQuality> findByStationNameIn(List<String> stationNames);

    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}