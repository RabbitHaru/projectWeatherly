package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.*;
import me.shinsunyoung.projectweatherly.board.dto.MyCommentResponse;
import org.springframework.data.domain.Page;

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

    // 가입일
    private LocalDateTime createdAt;

    // 약관 및 알림 동의 정보
    private Boolean termsOfServiceAgree;
    private Boolean privacyPolicyAgree;
    private Boolean boardNotificationAgree;
    private Boolean weatherAlertAgree;

    // [목록 데이터]
    private Page<CommunityPostResponse> myCommunityPosts; // 내가 쓴 글
    private Page<ReportResponse> myReports;               // 신고 내역
    private Page<MyCommentResponse> myComments;           // [★추가] 내가 쓴 댓글

    // [통계]
    private Integer postCount;
    private Integer reportCount;
    private Integer commentCount;
    private Integer likeCount;

    public static MyPageResponse fromMemberResponse(MemberResponse memberResponse) {
        return MyPageResponse.builder()
                .memberId(memberResponse.getMemberId())
                .nickname(memberResponse.getNickname())
                .email(memberResponse.getEmail())
                .profileImage(memberResponse.getProfileImage())
                .createdAt(memberResponse.getCreatedAt())
                .termsOfServiceAgree(memberResponse.getTermsOfServiceAgree())
                .privacyPolicyAgree(memberResponse.getPrivacyPolicyAgree())
                .boardNotificationAgree(memberResponse.getBoardNotificationAgree())
                .weatherAlertAgree(memberResponse.getWeatherAlertAgree())
                .commentCount(0)
                .likeCount(0)
                .postCount(0)
                .reportCount(0)
                .build();
    }
}