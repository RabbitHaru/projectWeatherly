package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.board.service.CommunityService;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MemberRepository memberRepository;
    private final AgreementService agreementService;
    private final CommunityService communityService;
    private final ReportService reportService;
    private final PasswordEncoder passwordEncoder;

    // 이메일로 마이페이지 정보 조회
    public MyPageResponse getMyPageInfo(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 약관 동의 정보 조회
        var agreement = agreementService.getAgreementByMemberId(member.getId());

        // 내가 작성한 커뮤니티 게시물 조회
        var myPosts = communityService.getMyPosts(member.getId());

        // 내가 신고한 게시물 조회
        var reportedPosts = reportService.getMyReports(member.getId());

        // 게시물 수와 신고 수
        int postCount = communityService.getPostCountByMemberId(member.getId());
        int reportCount = reportService.getReportCountByMemberId(member.getId());

        return MyPageResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .profileImage(member.getProfileImage())
                .termsOfServiceAgree(agreement.getTermsOfServiceAgree())
                .privacyPolicyAgree(agreement.getPrivacyPolicyAgree())
                .boardNotificationAgree(agreement.getBoardNotificationAgree())
                .weatherAlertAgree(agreement.getWeatherAlertAgree())
                .reportedPosts(reportedPosts)
                .myCommunityPosts(myPosts)
                .postCount(postCount)
                .reportCount(reportCount)
                .build();
    }

    @Transactional
    public MyPageResponse updateMemberForMyPage(String email, UpdateMemberRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 닉네임 업데이트
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            // 닉네임 중복 체크 (자신의 기존 닉네임과 같으면 체크 안함)
            if (!member.getNickname().equals(request.getNickname())) {
                boolean nicknameExists = memberRepository.existsByNickname(request.getNickname());
                if (nicknameExists) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
            }
            member.setNickname(request.getNickname().trim());
        }

        // 프로필 이미지 업데이트
        if (request.getProfileImage() != null) {
            member.setProfileImage(request.getProfileImage());
        }

        memberRepository.save(member);
        return getMyPageInfo(email);
    }

    @Transactional
    public MyPageResponse updatePassword(String email, UpdatePasswordRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (passwordEncoder.matches(request.getNewPassword(), member.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 비밀번호 업데이트
        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        memberRepository.save(member);

        return getMyPageInfo(email);
    }

    @Transactional
    public MyPageResponse updateNotificationSettings(String email, UpdateNotificationRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // AgreementService를 통해 알림 설정 업데이트
        agreementService.updateNotificationSettings(member.getId(), request);
        return getMyPageInfo(email);
    }

    @Transactional
    public MyPageResponse updateAgreementForMyPage(String email, UpdateAgreementRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // AgreementService를 통해 약관 동의 업데이트
        agreementService.updateAgreement(member.getId(), request);
        return getMyPageInfo(email);
    }

    // 내 게시물 삭제
    @Transactional
    public void deleteMyPost(String email, Long boardId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        communityService.deleteMyPost(member.getId(), boardId);
    }

    // 신고 취소
    @Transactional
    public void cancelMyReport(String email, Long reportId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        reportService.cancelReport(reportId, member.getId());
    }
}