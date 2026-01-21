package me.shinsunyoung.projectweatherly.member.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.local;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ★ [추가됨] 정지 해제 날짜 (null이면 영구 정지 혹은 정지 아님 상태)
    @Column(name = "ban_expires_at")
    private LocalDateTime banExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Agreement agreement;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 역할 이름을 문자열로 반환하는 헬퍼 메서드
    public String getRoleName() {
        return this.role != null ? this.role.name() : MemberRole.USER.name();
    }

    // 역할을 문자열로 설정하는 헬퍼 메서드
    public void setRoleFromString(String name) {
        if (name == null) {
            this.role = MemberRole.USER;
            return;
        }

        try {
            this.role = MemberRole.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.role = MemberRole.USER; // 기본값
        }
    }
}