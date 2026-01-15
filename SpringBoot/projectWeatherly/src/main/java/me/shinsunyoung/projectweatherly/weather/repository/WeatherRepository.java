package me.shinsunyoung.projectweatherly.weather.repository;

import me.shinsunyoung.projectweatherly.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, Long> {

    Optional<Weather> findTopByRegionCodeOrderByForecastDateTimeDesc(String regionCode);

    List<Weather> findByRegionCodeAndForecastDateTimeBetween(
            String regionCode,
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT w FROM Weather w WHERE w.regionCode = :regionCode " +
            "AND w.forecastDateTime >= :dateTime " +
            "ORDER BY w.forecastDateTime ASC")
    List<Weather> findFutureForecasts(
            @Param("regionCode") String regionCode,
            @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT w FROM Weather w WHERE w.regionCode = :regionCode " +
            "AND w.createdAt >= :dateTime " +
            "ORDER BY w.createdAt DESC")
    List<Weather> findRecentWeather(
            @Param("regionCode") String regionCode,
            @Param("dateTime") LocalDateTime dateTime);

    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}