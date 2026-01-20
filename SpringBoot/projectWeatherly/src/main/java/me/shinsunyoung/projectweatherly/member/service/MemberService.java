package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.dto.MyCommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.*;
import me.shinsunyoung.projectweatherly.member.exception.MemberException;
import me.shinsunyoung.projectweatherly.member.repository.AgreementRepository;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
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
import java.util.stream.Collectors;

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
    private final CommentRepository commentRepository; // [★추가]

    // ==================== UserDetails 반환 ====================
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        return new UserSecurityDTO(member);
    }

    // ==================== 로그인 ====================
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new MemberException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!member.getIsActive()) throw new MemberException("정지된 계정입니다.");
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) throw new MemberException("비밀번호 불일치");

        return LoginResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .role(member.getRole().name())
                .authProvider(member.getAuthProvider().name())
                .build();
    }

    // ==================== 회원가입 ====================
    @Transactional
    public Long signup(SignupRequest request, String profileImg) {
        if (memberRepository.existsByEmail(request.getEmail())) throw new MemberException("이미 사용 중인 이메일");
        if (!request.getTermsOfServiceAgree()) throw new MemberException("약관 동의 필요");

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

        Agreement agreement = Agreement.builder().member(savedMember)
                .termsOfServiceAgree(request.getTermsOfServiceAgree())
                .privacyPolicyAgree(request.getPrivacyPolicyAgree())
                .boardNotificationAgree(request.getBoardNotificationAgree())
                .weatherAlertAgree(request.getWeatherAlertAgree()).build();
        agreementRepository.save(agreement);
        return savedMember.getId();
    }

    // ==================== 조회 ====================
    public MemberResponse getMemberByEmail(String email) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        return convertToResponse(member);
    }

    public MemberResponse getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        return convertToResponse(member);
    }

    public MyPageResponse getMyPageInfoByEmail(String email) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        return getMyPageInfo(member.getId(), 1);
    }

    public boolean checkEmailExists(String email) { return memberRepository.existsByEmail(email); }
    public boolean checkNicknameExists(String nickname) { return memberRepository.existsByNickname(nickname); }

    // ==================== [핵심] 마이페이지 정보 조회 ====================
    public MyPageResponse getMyPageInfo(Long memberId, int page) {
        MemberResponse memberResponse = getMemberById(memberId);
        MyPageResponse response = MyPageResponse.fromMemberResponse(memberResponse);

        Member member = memberRepository.findById(memberId).orElseThrow();

        // 1. 게시글 목록 (10개)
        int pageNum = (page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNum, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<CommunityPostResponse> boardList = boardRepository.findByMember(member, pageable)
                .stream().map(CommunityPostResponse::new).collect(Collectors.toList());
        response.setMyCommunityPosts(boardList);

        // 2. 신고 내역 (전체, 최신순)
        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId);
        List<ReportResponse> reportList = reports.stream()
                .map(ReportResponse::from).collect(Collectors.toList());
        response.setMyReports(reportList);

        // 3. [★추가] 작성한 댓글 목록 (10개)
        List<Comment> comments = commentRepository.findByMemberOrderByCreatedAtDesc(member, pageable);
        List<MyCommentResponse> commentList = comments.stream()
                .map(MyCommentResponse::from).collect(Collectors.toList());
        response.setMyComments(commentList);

        // 4. 통계 정보
        response.setPostCount(boardList.size()); // 실제로는 count 쿼리 권장
        response.setReportCount(reportList.size());
        response.setCommentCount(commentRepository.countByMember(member)); // [★추가]
        response.setLikeCount(0);

        return response;
    }

    // ==================== 수정/삭제 ====================
    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if(request.getNickname() != null) member.setNickname(request.getNickname());
        if(request.getProfileImage() != null) member.setProfileImage(request.getProfileImage());
        memberRepository.save(member);
        return convertToResponse(member);
    }

    @Transactional
    public MyPageResponse updateMemberForMyPage(Long memberId, UpdateMemberRequest request) {
        updateMember(memberId, request);
        return getMyPageInfo(memberId, 1);
    }

    @Transactional
    public void updatePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if(!passwordEncoder.matches(currentPassword, member.getPassword())) throw new MemberException("비밀번호 불일치");
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Transactional
    public MemberResponse updateAgreement(Long memberId, UpdateAgreementRequest request) {
        Agreement agreement = agreementRepository.findByMemberId(memberId).orElseThrow();
        Optional.ofNullable(request.getTermsOfServiceAgree()).ifPresent(agreement::setTermsOfServiceAgree);
        Optional.ofNullable(request.getPrivacyPolicyAgree()).ifPresent(agreement::setPrivacyPolicyAgree);
        Optional.ofNullable(request.getBoardNotificationAgree()).ifPresent(agreement::setBoardNotificationAgree);
        Optional.ofNullable(request.getWeatherAlertAgree()).ifPresent(agreement::setWeatherAlertAgree);
        agreementRepository.save(agreement);
        return getMemberById(memberId);
    }

    @Transactional
    public MyPageResponse updateAgreementForMyPage(Long memberId, UpdateAgreementRequest request) {
        updateAgreement(memberId, request);
        return getMyPageInfo(memberId, 1);
    }

    @Transactional
    public MyPageResponse updateNotificationForMyPage(Long memberId, UpdateNotificationRequest request) {
        Agreement agreement = agreementRepository.findByMemberId(memberId).orElseThrow();
        Optional.ofNullable(request.getBoardNotificationAgree()).ifPresent(agreement::setBoardNotificationAgree);
        Optional.ofNullable(request.getWeatherAlertAgree()).ifPresent(agreement::setWeatherAlertAgree);
        agreementRepository.save(agreement);
        return getMyPageInfo(memberId, 1);
    }

    @Transactional
    public void deactivateMember(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.setIsActive(false);
        memberRepository.save(member);
    }

    @Transactional
    public void deleteMyPost(String email, Long postId) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(email).orElseThrow();
        Board board = boardRepository.findById(postId).orElseThrow();
        if(!board.getMember().getId().equals(member.getId())) throw new MemberException("권한 없음");
        boardRepository.delete(board);
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
}