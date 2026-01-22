package me.shinsunyoung.projectweatherly.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;

    // 1. 대시보드 통계 가져오기
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();

        // 대기 중인 신고 수
        long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        stats.put("pendingReports", pendingCount);

        // 총 회원수
        stats.put("totalMembers", memberRepository.count());

        // 오늘 작성된 게시글 수
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        stats.put("todayPosts", boardRepository.countByCreatedAtAfter(startOfDay));

        return stats;
    }

    // 2. 전체 회원 목록 조회 (Page 객체 반환 필수)
    public Page<Member> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    // 3. 회원 정지 처리
    @Transactional
    public void suspendMember(Long memberId, int days) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (days > 0) {
            member.setIsActive(false);
            member.setBanExpiresAt(LocalDateTime.now().plusDays(days));
        } else {
            // 0일이면 정지 해제
            member.setIsActive(true);
            member.setBanExpiresAt(null);
        }
    }

    // 4. [NEW] 회원 권한 변경 (USER <-> ADMIN)
    @Transactional
    public void changeMemberRole(Long memberId, MemberRole newRole) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        member.setRole(newRole);
        log.info("회원(ID: {})의 권한이 {}로 변경되었습니다.", memberId, newRole);
    }
}