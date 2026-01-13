package me.shinsunyoung.projectweatherly.region.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder

public class Region {
    @Id
    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "region_name", nullable = false)
    private String regionName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int nx;

    @Column(nullable = false)
    private int ny;

    public Region(String regionCode, String regionName, boolean active) {
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.active = active;
    }
}
