package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // ✅ 추가된 필드들 (에러 해결용)
    private Integer commentCount;
    private Integer likeCount;

    // ✅ fromMemberResponse 메서드 추가
    public static MyPageResponse fromMemberResponse(MemberResponse memberResponse) {
        return MyPageResponse.builder()
                .memberId(memberResponse.getMemberId())
                .nickname(memberResponse.getNickname())
                .email(memberResponse.getEmail())
                .profileImage(memberResponse.getProfileImage())
                .commentCount(0)
                .likeCount(0)
                .postCount(0)
                .reportCount(0)
                .build();
    }
}