package me.shinsunyoung.projectweatherly.member.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgreementRequest {
    private Boolean termsOfServiceAgree;      // 이용약관 동의
    private Boolean privacyPolicyAgree;       // 개인정보 처리방침 동의
    private Boolean boardNotificationAgree;   // 게시판 알림 동의 (추가)
    private Boolean weatherAlertAgree;        // 날씨 알림 동의 (추가)

}