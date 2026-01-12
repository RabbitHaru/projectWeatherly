package me.shinsunyoung.projectweatherly.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes",
        indexes = {
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_expires_at", columnList = "expiresAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 인증번호 사용 처리
     */
    public void markAsUsed() {
        this.used = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 시도 횟수 증가
     */
    public void incrementAttempts() {
        this.attempts++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 인증번호가 유효한지 확인
     */
    public boolean isValid() {
        return !used &&
                attempts < 5 &&
                expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * 남은 시간(분) 계산
     */
    public long getRemainingMinutes() {
        return java.time.Duration.between(
                LocalDateTime.now(), expiresAt
        ).toMinutes();
    }
}