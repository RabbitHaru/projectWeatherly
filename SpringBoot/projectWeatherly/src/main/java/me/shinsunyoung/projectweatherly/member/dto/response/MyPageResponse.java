package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageResponse {
    private Long memberId;
    private String nickname;
    private String email;
    private String profileImage;

    // ✅ createdAt 필드 (Thymeleaf 템플릿에서 사용됨)
    private LocalDateTime createdAt;

    // 약관 동의 정보
    private Boolean termsOfServiceAgree;
    private Boolean privacyPolicyAgree;

    // 알림 설정 정보
    private Boolean boardNotificationAgree;
    private Boolean weatherAlertAgree;

    // 게시물 목록
    private List<ReportedPostResponse> reportedPosts;
    private List<CommunityPostResponse> myCommunityPosts;

    // 통계
    private Integer postCount;
    private Integer reportCount;
    private Integer commentCount;
    private Integer likeCount;

    // ✅ MemberResponse를 받는 메서드 (완전한 버전으로 수정)
    public static MyPageResponse fromMemberResponse(MemberResponse memberResponse) {
        return MyPageResponse.builder()
                .memberId(memberResponse.getMemberId())
                .nickname(memberResponse.getNickname())
                .email(memberResponse.getEmail())
                .profileImage(memberResponse.getProfileImage())
                .createdAt(memberResponse.getCreatedAt()) // ✅ createdAt 설정
                .termsOfServiceAgree(memberResponse.getTermsOfServiceAgree()) // ✅ 약관 동의 정보
                .privacyPolicyAgree(memberResponse.getPrivacyPolicyAgree()) // ✅ 개인정보 동의
                .boardNotificationAgree(memberResponse.getBoardNotificationAgree()) // ✅ 게시물 알림 설정
                .weatherAlertAgree(memberResponse.getWeatherAlertAgree()) // ✅ 날씨 알림 설정

                .commentCount(0)
                .likeCount(0)
                .postCount(0)
                .reportCount(0)
                .build();
    }

    // ✅ Member 엔티티를 받는 메서드 (완전한 버전으로 수정)
    public static MyPageResponse fromMember(me.shinsunyoung.projectweatherly.member.domain.entity.Member member) {
        return MyPageResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .profileImage(member.getProfileImage())
                .createdAt(member.getCreatedAt()) // ✅ createdAt 설정
                .commentCount(0)
                .likeCount(0)
                .postCount(0)
                .reportCount(0)
                .build();
    }
}