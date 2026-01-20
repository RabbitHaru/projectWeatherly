package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateAgreementRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateMemberRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateNotificationRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdatePasswordRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.CommunityPostResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.ReportResponse;
import me.shinsunyoung.projectweatherly.member.exception.MemberException;
import me.shinsunyoung.projectweatherly.member.repository.AgreementRepository;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final AgreementRepository agreementRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository; // [필수]
    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;

    // 마이페이지 정보 조회 (핵심 로직)
    public MyPageResponse getMyPageInfo(String email) {
        return getMyPageInfo(email, 0); // 기본 페이지 0
    }

    public MyPageResponse getMyPageInfo(String email, int page) {
        return getMyPageInfo(memberService.getMemberByEmail(email).getMemberId(), page);
    }

    public MyPageResponse getMyPageInfo(Long memberId, int page) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        // 1. 기본 회원 정보 변환
        MemberResponse memberResponse = memberService.getMemberById(memberId);
        MyPageResponse response = MyPageResponse.fromMemberResponse(memberResponse);

        // 2. 내가 쓴 글 목록 조회 (페이징 적용)
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<CommunityPostResponse> postList = boardRepository.findByMember(member, pageable)
                .stream().map(CommunityPostResponse::new).collect(Collectors.toList());
        response.setMyCommunityPosts(postList);

        // 3. [★추가] 내가 신고한 내역 조회 및 변환
        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId);
        List<ReportResponse> reportList = reports.stream()
                .map(ReportResponse::from) // DTO의 from 메서드 사용
                .collect(Collectors.toList());
        response.setMyReports(reportList);

        // 4. 통계 정보 설정 (Optional)
        response.setPostCount(postList.size()); // 전체 개수를 구하려면 count 쿼리 별도 필요
        response.setReportCount(reportList.size());

        return response;
    }

    @Transactional
    public MyPageResponse updateMemberForMyPage(String email, UpdateMemberRequest request) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        if (request.getNickname() != null) member.setNickname(request.getNickname());
        if (request.getProfileImage() != null) member.setProfileImage(request.getProfileImage());
        memberRepository.save(member);
        return getMyPageInfo(member.getId(), 0);
    }

    // 오버로딩 (ID 기반)
    @Transactional
    public MyPageResponse updateMemberForMyPage(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if (request.getNickname() != null) member.setNickname(request.getNickname());
        if (request.getProfileImage() != null) member.setProfileImage(request.getProfileImage());
        memberRepository.save(member);
        return getMyPageInfo(memberId, 0);
    }

    @Transactional
    public void updatePassword(String email, UpdatePasswordRequest request) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void updateNotificationSettings(String email, UpdateNotificationRequest request) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        Agreement agreement = member.getAgreement();
        if (agreement == null) {
            agreement = Agreement.builder().member(member).build();
        }

        if (request.getBoardNotificationAgree() != null)
            agreement.setBoardNotificationAgree(request.getBoardNotificationAgree());
        if (request.getWeatherAlertAgree() != null)
            agreement.setWeatherAlertAgree(request.getWeatherAlertAgree());

        agreementRepository.save(agreement);
    }

    @Transactional
    public void deleteMyPost(String email, Long postId) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        Board board = boardRepository.findById(postId).orElseThrow();

        if (!board.getMember().getId().equals(member.getId())) {
            throw new MemberException("삭제 권한이 없습니다.");
        }
        boardRepository.delete(board);
    }
}