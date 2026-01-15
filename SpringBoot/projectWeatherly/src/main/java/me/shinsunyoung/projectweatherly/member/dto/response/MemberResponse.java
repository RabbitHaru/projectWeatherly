package me.shinsunyoung.projectweatherly.member.dto.response;



import lombok.Builder;
import lombok.Data;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;


import java.time.LocalDateTime;

@Data
@Builder
public class MemberResponse {

    private Long memberId;
    private String email;
    private String nickname;
    private String profileImage;
    private MemberRole role;
    private AuthProvider authProvider;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 약관 동의 정보
    private Boolean termsOfServiceAgree;
    private Boolean privacyPolicyAgree;
    private Boolean marketingAgree;

    // 알림 설정 정보
    private Boolean boardNotificationAgree;
    private Boolean weatherAlertAgree;
}