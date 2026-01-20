package me.shinsunyoung.projectweatherly.airquality.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "air_quality_realtime")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AirQualityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sidoName;        // 시도 (서울, 부산 등)
    private String stationName;     // 측정소
    private LocalDateTime dataTime; // 측정 시간

    // 수치 및 등급
    private Integer pm10Value;
    private String pm10Grade;
    private Integer pm25Value;
    private String pm25Grade;

    private Double o3Value;
    private String o3Grade;
    private Double no2Value;
    private String no2Grade;
    private Double coValue;
    private String coGrade;
    private Double so2Value;
    private String so2Grade;

    private Integer khaiValue;      // 통합대기환경지수
    private String khaiGrade;

    private LocalDateTime recordedAt; // DB 저장 시각 (통계용)

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}