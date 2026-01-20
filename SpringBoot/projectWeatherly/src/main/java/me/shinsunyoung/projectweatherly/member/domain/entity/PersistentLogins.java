package me.shinsunyoung.projectweatherly.member.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "persistent_logins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersistentLogins {

    @Id
    @Column(name = "series", length = 64, nullable = false)
    private String series;

    @Column(name = "username", length = 64, nullable = false)
    private String username;

    @Column(name = "token", length = 64, nullable = false)
    private String token;

    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}