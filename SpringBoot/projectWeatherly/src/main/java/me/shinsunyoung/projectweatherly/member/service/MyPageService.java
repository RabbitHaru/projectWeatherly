package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.dto.MyCommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final AgreementRepository agreementRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;

    // 마이페이지 정보 조회
    public MyPageResponse getMyPageInfo(String email) {
        return getMyPageInfo(email, 1 );
    }

    public MyPageResponse getMyPageInfo(String email, Integer page) {
        return getMyPageInfo(memberService.getMemberByEmail(email).getMemberId(), page);
    }

    public MyPageResponse getMyPageInfo(Long memberId, Integer page) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        MemberResponse memberResponse = memberService.getMemberById(memberId);
        MyPageResponse response = MyPageResponse.fromMemberResponse(memberResponse);

        // 게시글
        Pageable pageable = PageRequest.of((int) (page-1), 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommunityPostResponse> postList = boardRepository.findByMemberAndBoardStatus(member,BoardStatus.ACTIVE, pageable)
                .map(CommunityPostResponse::new);
        response.setMyCommunityPosts(postList);

        // 신고 내역
        Page<ReportResponse> reportList = reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId, pageable).map(ReportResponse::from);;
        response.setMyReports(reportList);

        // 댓글 내역
        if (commentRepository != null) {
            Page<MyCommentResponse> commentList = commentRepository.findByMemberOrderByCreatedAtDesc(member, pageable)
                    .map(MyCommentResponse::from);
            response.setMyComments(commentList);
            response.setCommentCount(commentRepository.countByMember(member));
        }

        // 통계
        response.setPostCount(postList.getSize());
        response.setReportCount(reportList.getSize());

        return response;
    }

    @Transactional
    public void updateMemberForMyPage(String email, UpdateMemberRequest request) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        if (request.getNickname() != null) member.setNickname(request.getNickname());
        if (request.getProfileImage() != null) member.setProfileImage(request.getProfileImage());
        memberRepository.save(member);

    }



    // [★수정됨] 비밀번호 변경 로직 (save 추가)
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

        // [중요] 변경 사항 즉시 저장! (이게 없어서 안됐던 것)
        memberRepository.save(member);
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
        board.setBoardStatus(BoardStatus.DELETED);
    }
}