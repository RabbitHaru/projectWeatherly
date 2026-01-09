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
@AllArgsConstructor
@Builder

public class Region {
    @Id
    @Column(name = "legal_code", length = 10)
    private String legalCode;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
