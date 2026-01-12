package me.shinsunyoung.projectweatherly.member.domain.member;

import jakarta.persistence.*;
import lombok.*;
import me.shinsunyoung.projectweatherly.member.domain.BaseTimeEntity;



import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "user_email"),
                @Index(name = "idx_auth_provider", columnList = "auth_provider"),
                @Index(name = "idx_user_role", columnList = "user_role"),
                @Index(name = "idx_is_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", nullable = false, unique = true, length = 100)
    private String userEmail;

    @Column(name = "user_password", length = 255)
    private String userPassword;  // BCrypt로 암호화, 소셜 로그인 사용자는 null 가능

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 20)
    @Builder.Default
    private MemberRole userRole = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    @PreUpdate
    public void prePersistAndUpdate() {
        if (this.userRole == null) {
            this.userRole = MemberRole.USER;
        }
        if (this.authProvider == null) {
            this.authProvider = AuthProvider.LOCAL;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    // 비밀번호 변경 메서드
    public void changePassword(String encryptedPassword) {
        this.userPassword = encryptedPassword;
    }

    // 프로필 업데이트 메서드
    public void updateProfile(String userName, String profileImage) {
        this.userName = userName;
        if (profileImage != null) {
            this.profileImage = profileImage;
        }
    }

    // 마지막 로그인 시간 업데이트
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // 계정 활성화/비활성화
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // 소셜 로그인 사용자 확인
    public boolean isSocialUser() {
        return this.authProvider != AuthProvider.LOCAL;
    }
}