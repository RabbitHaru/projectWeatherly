package me.shinsunyoung.projectweatherly.member.dto.response;



import lombok.Builder;
import lombok.Data;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;


import java.time.LocalDateTime;

@Data
@Builder
public class MyPageResponse {

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

    // 통계 정보 (추후 확장)
    private Integer postCount;
    private Integer commentCount;
    private Integer likeCount;

    // 날씨 관련 정보 (추후 확장)
    private String recentWeatherRegion;
    private String recentWeatherCondition;

    public static MyPageResponse fromMemberResponse(MemberResponse memberResponse) {
        return MyPageResponse.builder()
                .memberId(memberResponse.getMemberId())
                .email(memberResponse.getEmail())
                .nickname(memberResponse.getNickname())
                .profileImage(memberResponse.getProfileImage())
                .role(memberResponse.getRole())
                .authProvider(memberResponse.getAuthProvider())
                .isActive(memberResponse.getIsActive())
                .lastLoginAt(memberResponse.getLastLoginAt())
                .createdAt(memberResponse.getCreatedAt())
                .updatedAt(memberResponse.getUpdatedAt())
                .termsOfServiceAgree(memberResponse.getTermsOfServiceAgree())
                .privacyPolicyAgree(memberResponse.getPrivacyPolicyAgree())
                .marketingAgree(memberResponse.getMarketingAgree())
                .boardNotificationAgree(memberResponse.getBoardNotificationAgree())
                .weatherAlertAgree(memberResponse.getWeatherAlertAgree())
                .build();
    }
}