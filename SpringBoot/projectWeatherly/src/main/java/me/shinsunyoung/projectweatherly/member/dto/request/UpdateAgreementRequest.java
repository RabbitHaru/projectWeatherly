package me.shinsunyoung.projectweatherly.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgreementRequest {
    private Boolean termsOfServiceAgree;      // 이용약관 동의
    private Boolean privacyPolicyAgree;       // 개인정보 처리방침 동의
}