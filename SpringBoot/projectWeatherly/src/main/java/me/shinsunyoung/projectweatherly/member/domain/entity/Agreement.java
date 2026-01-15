package me.shinsunyoung.projectweatherly.member.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "terms_of_service_agree")
    private Boolean termsOfServiceAgree = false;

    @Column(name = "privacy_policy_agree")
    private Boolean privacyPolicyAgree = false;

    @Column(name = "board_notification_agree")
    private Boolean boardNotificationAgree = false;

    @Column(name = "weather_alert_agree")
    private Boolean weatherAlertAgree = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}