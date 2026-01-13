package me.shinsunyoung.projectweatherly.member.domain.entity;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String region;
    private Double temperature;
    private Double humidity;
    private String condition;
    private String icon;
    private LocalDateTime timestamp;
    private LocalDateTime createdDate;
}
