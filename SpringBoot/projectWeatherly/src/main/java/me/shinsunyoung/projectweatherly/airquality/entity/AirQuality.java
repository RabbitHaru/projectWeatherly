package me.shinsunyoung.projectweatherly.airquality.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.time.LocalDateTime;

@Entity
@Table(name = "air_quality")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AirQuality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stationName;         // 측정소명

    @Column(nullable = false)
    private String stationCode;         // 측정소 코드

    @Column(nullable = false)
    private String sidoName;            // 시도명

    private Integer pm10Value;          // 미세먼지(PM10) 농도
    private String pm10Grade;           // PM10 등급 (1: 좋음, 2: 보통, 3: 나쁨, 4: 매우나쁨)

    private Integer pm25Value;          // 초미세먼지(PM2.5) 농도
    private String pm25Grade;           // PM2.5 등급

    private Integer o3Value;            // 오존 농도
    private String o3Grade;             // 오존 등급

    private Integer no2Value;           // 이산화질소 농도
    private String no2Grade;            // 이산화질소 등급

    private Integer coValue;            // 일산화탄소 농도
    private String coGrade;             // 일산화탄소 등급

    private Integer so2Value;           // 아황산가스 농도
    private String so2Grade;            // 아황산가스 등급

    @Column(nullable = false)
    private LocalDateTime dataTime;     // 측정 시간

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;    // 생성 시간
}