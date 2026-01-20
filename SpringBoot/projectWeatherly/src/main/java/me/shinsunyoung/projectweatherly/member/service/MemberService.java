package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.*;
import me.shinsunyoung.projectweatherly.member.exception.MemberException;
import me.shinsunyoung.projectweatherly.member.repository.AgreementRepository;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final AgreementRepository agreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;

    // ==================== UserDetailsService 구현 ====================
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .roles(member.getRole().name())
                .build();
    }

    // ==================== 이메일 기반 메서드들 ====================

    /**
     * 이메일로 회원 정보 조회
     */
    public MemberResponse getMemberByEmail(String email) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        return convertToResponse(member);
    }

    /**
     * 이메일로 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageInfoByEmail(String email) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        return getMyPageInfo(member.getId(),1);
    }

    /**
     * 이메일 중복 체크
     */
    public boolean checkEmailExists(String email) {
        return memberRepository.existsByEmail(email);
    }

    /**
     * 닉네임 중복 체크
     */
    public boolean checkNicknameExists(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    // ==================== 기존 메서드들 ====================

    @Transactional
    public Long signup(SignupRequest request, String profileImg) {
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
                .profileImage(profileImg)
                .role(MemberRole.USER)
                .authProvider(AuthProvider.local)
                .isActive(true)
                .build();

        Member savedMember = memberRepository.save(member);

        // 약관 동의 생성
        Agreement agreement = Agreement.builder()
                .member(savedMember)
                .termsOfServiceAgree(request.getTermsOfServiceAgree())
                .privacyPolicyAgree(request.getPrivacyPolicyAgree())
                .boardNotificationAgree(request.getBoardNotificationAgree())
                .weatherAlertAgree(request.getWeatherAlertAgree())
                .build();
        agreementRepository.save(agreement);

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

        // Spring Security Session 인증 방식 사용
        return LoginResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .role(member.getRole().name())
                .authProvider(member.getAuthProvider().name())
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

        Optional.ofNullable(request.getBoardNotificationAgree())
                .ifPresent(agreement::setBoardNotificationAgree);

        Optional.ofNullable(request.getWeatherAlertAgree())
                .ifPresent(agreement::setWeatherAlertAgree);



        agreementRepository.save(agreement);

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
                .boardNotificationAgree(agreement != null ? agreement.getBoardNotificationAgree() : null)
                .weatherAlertAgree(agreement != null ? agreement.getWeatherAlertAgree() : null)
                .build();
    }

    public MyPageResponse getMyPageInfo(Long memberId,int page) {
        MemberResponse memberResponse = getMemberById(memberId);
        MyPageResponse response = MyPageResponse.fromMemberResponse(memberResponse);
        Member member = memberRepository.findById(memberId).get();
        Pageable pageable = PageRequest.of(page-1, 10).withSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<CommunityPostResponse>  boardList= boardRepository.findByMember(member,pageable).stream().map(CommunityPostResponse::new).toList();

        List<ReportedPostResponse>  reportList= reportRepository.findByReporterId(member.getId(),pageable)
                .stream().map(ReportedPostResponse::new)
                .toList();
        response.setMyCommunityPosts(boardList);
        response.setReportedPosts(reportList);
        // TODO: 실제 게시물, 댓글, 좋아요 수 조회 로직 추가
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

        return getMyPageInfo(memberId,1);
    }

    @Transactional
    public MyPageResponse updateAgreementForMyPage(Long memberId, UpdateAgreementRequest request) {
        updateAgreement(memberId, request);
        return getMyPageInfo(memberId,1);
    }

    /**
     * 알림 설정 업데이트 (별도 메서드 추가)
     */
    @Transactional
    public MyPageResponse updateNotificationForMyPage(Long memberId, UpdateNotificationRequest request) {
        Agreement agreement = agreementRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MemberException("약관 정보를 찾을 수 없습니다."));

        Optional.ofNullable(request.getBoardNotificationAgree())
                .ifPresent(agreement::setBoardNotificationAgree);

        Optional.ofNullable(request.getWeatherAlertAgree())
                .ifPresent(agreement::setWeatherAlertAgree);

        agreementRepository.save(agreement);

        return getMyPageInfo(memberId,1);
    }
}