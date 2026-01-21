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

    // [추가됨] 발표 시각 (이걸로 중복 체크 할 거야!)
    private String dataTime;

    private String informData;    // 예보 대상 날짜
    private String informCode;    // PM10 or PM25

    @Column(length = 2000)
    private String informOverall; // 개황

    @Column(length = 2000)
    private String informCause;   // 발생 원인

    @Column(length = 2000)
    private String informGrade;   // 등급

    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}