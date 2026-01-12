package me.shinsunyoung.projectweatherly.weather.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Weather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String regionCode;          // 지역 코드

    @Column(nullable = false)
    private String regionName;          // 지역 이름

    @Column(nullable = false)
    private Double temperature;         // 현재 기온 (°C)

    @Column(nullable = false)
    private Double feelsLike;           // 체감 온도 (°C)

    private Double humidity;            // 습도 (%)

    private Double precipitation;       // 강수량 (mm)

    private Double windSpeed;           // 풍속 (m/s)

    private String windDirection;       // 풍향

    private Integer precipitationType;  // 강수형태 (0: 없음, 1: 비, 2: 비/눈, 3: 눈)

    private Integer skyCondition;       // 하늘상태 (1: 맑음, 3: 구름많음, 4: 흐림)

    @Column(length = 50)
    private String weatherCondition;    // 날씨 상태 (맑음, 구름조금 등)

    @Column(nullable = false)
    private LocalDateTime baseDateTime; // 기준 시간

    @Column(nullable = false)
    private LocalDateTime forecastDateTime; // 예보 시간

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;    // 생성 시간
}