package me.shinsunyoung.projectweatherly.region.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    // 법정동 코드 (PK)

    @Id
    private String regionCode;

    // 지역명
    private String regionName;

    // 사용 여부 (폐지되지 않은 지역)
    private boolean active;

    // 기상청 격자 좌표
    private int nx;
    private int ny;

    // Builder 생성자
    // - JPA 기본 생성자와 분리

    @Builder
    public Region(String regionCode,
                  String regionName,
                  boolean active,
                  int nx,
                  int ny) {

        this.regionCode = regionCode;
        this.regionName = regionName;
        this.active = active;
        this.nx = nx;
        this.ny = ny;
    }
}
