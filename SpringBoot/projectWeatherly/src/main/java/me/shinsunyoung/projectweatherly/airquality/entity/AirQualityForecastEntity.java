package me.shinsunyoung.projectweatherly.airquality.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "air_quality_forecast")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AirQualityForecastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String informData;    // 예보 날짜 (YYYY-MM-DD)
    private String informCode;    // PM10 or PM25

    @Column(length = 2000)        // 내용이 길 수 있음
    private String informOverall; // 전체 개황

    @Column(length = 2000)
    private String informGrade;   // 지역별 등급 (서울 : 보통, ...)

    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}