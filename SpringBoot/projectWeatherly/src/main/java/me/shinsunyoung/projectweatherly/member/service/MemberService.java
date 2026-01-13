package me.shinsunyoung.projectweatherly.member.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.entity.NotificationSetting;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.LoginResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.exception.MemberException;
import me.shinsunyoung.projectweatherly.member.repository.AgreementRepository;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import me.shinsunyoung.projectweatherly.member.repository.NotificationSettingRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final AgreementRepository agreementRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Long signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new MemberException("이미 사용 중인 이메일입니다.");
        }

        // 필수 약관 동의 체크
        if (!request.getTermsOfServiceAgree() || !request.getPrivacyPolicyAgree()) {
            throw new MemberException("필수 약관에 동의해주세요.");
        }

        // 회원 생성
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .profileImage(request.getProfileImage())
                .role(MemberRole.USER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        Member savedMember = memberRepository.save(member);

        // 약관 동의 생성
        Agreement agreement = Agreement.builder()
                .member(savedMember)
                .termsOfServiceAgree(request.getTermsOfServiceAgree())
                .privacyPolicyAgree(request.getPrivacyPolicyAgree())
                .marketingAgree(request.getMarketingAgree())
                .build();

        agreementRepository.save(agreement);

        // 알림 설정 생성
        NotificationSetting notificationSetting = NotificationSetting.builder()
                .member(savedMember)
                .boardNotificationAgree(request.getBoardNotificationAgree())
                .weatherAlertAgree(request.getWeatherAlertAgree())
                .build();

        notificationSettingRepository.save(notificationSetting);

        log.info("회원가입 성공: {}", savedMember.getEmail());
        return savedMember.getId();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new MemberException("이메일 또는 비밀번호를 확인해주세요."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new MemberException("이메일 또는 비밀번호를 확인해주세요.");
        }


        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(
                member.getEmail(),
                member.getRole()
        );

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole().name())
                .expiresIn(86400000L) // 24시간
                .build();
    }

    public MemberResponse getMemberById(Long memberId) {
        Member member = memberRepository.findByIdWithAgreementAndNotification(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        return convertToResponse(member);
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        if (request.getNickname() != null) {
            member.setNickname(request.getNickname());
        }

        if (request.getProfileImage() != null) {
            member.setProfileImage(request.getProfileImage());
        }

        memberRepository.save(member);

        return convertToResponse(member);
    }

    @Transactional
    public void updatePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new MemberException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 설정
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Transactional
    public MemberResponse updateAgreement(Long memberId, UpdateAgreementRequest request) {
        Agreement agreement = agreementRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MemberException("약관 정보를 찾을 수 없습니다."));

        Optional.ofNullable(request.getTermsOfServiceAgree())
                .ifPresent(agreement::setTermsOfServiceAgree);

        Optional.ofNullable(request.getPrivacyPolicyAgree())
                .ifPresent(agreement::setPrivacyPolicyAgree);

        Optional.ofNullable(request.getMarketingAgree())
                .ifPresent(agreement::setMarketingAgree);

        agreementRepository.save(agreement);

        return getMemberById(memberId);
    }

    @Transactional
    public MemberResponse updateNotification(Long memberId, UpdateNotificationRequest request) {
        NotificationSetting setting = notificationSettingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MemberException("알림 설정을 찾을 수 없습니다."));

        Optional.ofNullable(request.getBoardNotificationAgree())
                .ifPresent(setting::setBoardNotificationAgree);

        Optional.ofNullable(request.getWeatherAlertAgree())
                .ifPresent(setting::setWeatherAlertAgree);

        notificationSettingRepository.save(setting);

        return getMemberById(memberId);
    }

    @Transactional
    public void deactivateMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        member.setIsActive(false);
        memberRepository.save(member);
    }

    private MemberResponse convertToResponse(Member member) {
        Agreement agreement = member.getAgreement();
        NotificationSetting notificationSetting = member.getNotificationSetting();

        return MemberResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .role(member.getRole())
                .authProvider(member.getAuthProvider())
                .isActive(member.getIsActive())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .termsOfServiceAgree(agreement != null ? agreement.getTermsOfServiceAgree() : null)
                .privacyPolicyAgree(agreement != null ? agreement.getPrivacyPolicyAgree() : null)
                .marketingAgree(agreement != null ? agreement.getMarketingAgree() : null)
                .boardNotificationAgree(notificationSetting != null ? notificationSetting.getBoardNotificationAgree() : null)
                .weatherAlertAgree(notificationSetting != null ? notificationSetting.getWeatherAlertAgree() : null)
                .build();
    }
    // MemberService.java에 다음 메서드 추가
    public MyPageResponse getMyPageInfo(Long memberId) {
        MemberResponse memberResponse = getMemberById(memberId);
        MyPageResponse response = MyPageResponse.fromMemberResponse(memberResponse);

        // 추가 통계 정보 설정 (실제로는 다른 서비스나 레포지토리에서 가져옴)
        response.setPostCount(0);
        response.setCommentCount(0);
        response.setLikeCount(0);

        return response;
    }


    @Transactional
    public MyPageResponse updateMemberForMyPage(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            member.setNickname(request.getNickname());
        }

        if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
            member.setProfileImage(request.getProfileImage());
        }

        memberRepository.save(member);

        // MyPageResponse로 반환
        return getMyPageInfo(memberId);
    }

    @Transactional
    public MyPageResponse updateAgreementForMyPage(Long memberId, UpdateAgreementRequest request) {
        updateAgreement(memberId, request);
        return getMyPageInfo(memberId);
    }

    @Transactional
    public MyPageResponse updateNotificationForMyPage(Long memberId, UpdateNotificationRequest request) {
        updateNotification(memberId, request);
        return getMyPageInfo(memberId);
    }
}