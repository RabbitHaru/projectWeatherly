package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.exception.MemberException;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    /**
     * 이메일로 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageInfo(String email) {
        Long memberId = getMemberIdByEmail(email);
        return memberService.getMyPageInfo(memberId);
    }

    /**
     * 이메일로 회원 정보 수정
     */
    @Transactional
    public MyPageResponse updateMemberForMyPage(String email, UpdateMemberRequest request) {
        Long memberId = getMemberIdByEmail(email);
        return memberService.updateMemberForMyPage(memberId, request);
    }

    /**
     * 이메일로 비밀번호 변경
     */
    @Transactional
    public void updatePassword(String email, UpdatePasswordRequest request) {
        Long memberId = getMemberIdByEmail(email);
        memberService.updatePassword(memberId, request.getCurrentPassword(), request.getNewPassword());
    }

    /**
     * 이메일로 알림 설정 업데이트
     */
    @Transactional
    public void updateNotificationSettings(String email, UpdateNotificationRequest request) {
        Long memberId = getMemberIdByEmail(email);

        // UpdateNotificationRequest를 사용하여 알림 설정 업데이트
        memberService.updateNotificationForMyPage(memberId, request);
    }

    /**
     * 이메일로 약관 동의 업데이트
     */
    @Transactional
    public MyPageResponse updateAgreementForMyPage(String email, UpdateAgreementRequest request) {
        Long memberId = getMemberIdByEmail(email);
        return memberService.updateAgreementForMyPage(memberId, request);
    }

    /**
     * 이메일로 내 게시물 삭제
     */
    @Transactional
    public void deleteMyPost(String email, Long postId) {
        Long memberId = getMemberIdByEmail(email);

        // TODO: 게시물 서비스와 연동 필요
        // boardService.deletePostByMemberId(postId, memberId);
        log.info("회원 {}의 게시물 {} 삭제 요청", email, postId);
        // 임시 구현
        throw new UnsupportedOperationException("게시물 삭제 기능은 구현 중입니다.");
    }

    /**
     * 이메일로 신고 취소
     */
    @Transactional
    public void cancelMyReport(String email, Long reportId) {
        Long memberId = getMemberIdByEmail(email);

        // TODO: 신고 서비스와 연동 필요
        // reportService.cancelReportByMemberId(reportId, memberId);
        log.info("회원 {}의 신고 {} 취소 요청", email, reportId);
        // 임시 구현
        throw new UnsupportedOperationException("신고 취소 기능은 구현 중입니다.");
    }

    /**
     * 이메일로 MemberId 조회 (공통 메서드)
     */
    private Long getMemberIdByEmail(String email) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + email));
        return member.getId();
    }
}