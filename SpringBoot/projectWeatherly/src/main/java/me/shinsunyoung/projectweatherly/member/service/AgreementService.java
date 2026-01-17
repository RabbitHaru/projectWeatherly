package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateAgreementRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateNotificationRequest;
import me.shinsunyoung.projectweatherly.member.repository.AgreementRepository;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final MemberRepository memberRepository;

    // 회원의 약관 동의 정보 조회
    public Agreement getAgreementByMemberId(Long memberId) {
        return agreementRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("약관 동의 정보를 찾을 수 없습니다."));
    }

    // 회원의 약관 동의 정보 조회 (이메일로)
    public Agreement getAgreementByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return getAgreementByMemberId(member.getId());
    }

    // 약관 동의 업데이트
    @Transactional
    public Agreement updateAgreement(Long memberId, UpdateAgreementRequest request) {
        Agreement agreement = getAgreementByMemberId(memberId);

        if (request.getTermsOfServiceAgree() != null) {
            agreement.setTermsOfServiceAgree(request.getTermsOfServiceAgree());
        }

        if (request.getPrivacyPolicyAgree() != null) {
            agreement.setPrivacyPolicyAgree(request.getPrivacyPolicyAgree());
        }

        agreement.setUpdatedAt(LocalDateTime.now());
        return agreementRepository.save(agreement);
    }

    // 알림 설정 업데이트
    @Transactional
    public Agreement updateNotificationSettings(Long memberId, UpdateNotificationRequest request) {
        Agreement agreement = getAgreementByMemberId(memberId);

        if (request.getBoardNotificationAgree() != null) {
            agreement.setBoardNotificationAgree(request.getBoardNotificationAgree());
        }

        if (request.getWeatherAlertAgree() != null) {
            agreement.setWeatherAlertAgree(request.getWeatherAlertAgree());
        }

        agreement.setUpdatedAt(LocalDateTime.now());
        return agreementRepository.save(agreement);
    }

    // 약관 동의 생성 (회원가입 시)
    @Transactional
    public Agreement createAgreementForMember(Member member) {
        Agreement agreement = Agreement.builder()
                .member(member)
                .termsOfServiceAgree(false)
                .privacyPolicyAgree(false)
                .boardNotificationAgree(true)  // 기본값: 게시물 알림 동의
                .weatherAlertAgree(true)       // 기본값: 기상특보 알림 동의
                .build();

        return agreementRepository.save(agreement);
    }

    // 모든 약관 동의 여부 확인
    public boolean checkAllAgreements(Long memberId) {
        Agreement agreement = getAgreementByMemberId(memberId);
        return agreement.getTermsOfServiceAgree() && agreement.getPrivacyPolicyAgree();
    }
}