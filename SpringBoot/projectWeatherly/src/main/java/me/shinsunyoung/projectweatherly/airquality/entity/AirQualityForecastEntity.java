package me.shinsunyoung.projectweatherly.airquality.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "air_quality_forecast",
        uniqueConstraints = {
                // ⭐ [설정] 오직 발표 시간(dataTime)만으로 중복 체크!
                @UniqueConstraint(
                        name = "uk_forecast_data_time",
                        columnNames = {"dataTime"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AirQualityForecastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dataTime;      // 발표 시각 (이게 같으면 중복!)

    private String informData;    // 예보 대상 날짜

    private String informCode;    // (참고용으로 저장만 함)

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