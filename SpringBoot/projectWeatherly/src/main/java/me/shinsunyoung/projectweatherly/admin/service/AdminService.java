package me.shinsunyoung.projectweatherly.admin.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;

    // 대시보드 통계 가져오기
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();

        // ★ [핵심] DB에서 'PENDING' 상태인 신고만 정확히 카운트
        long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        stats.put("pendingReports", pendingCount);

        // 총 회원수
        stats.put("totalMembers", memberRepository.count());

        // 오늘 작성된 게시글 수
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        stats.put("todayPosts", boardRepository.countByCreatedAtAfter(startOfDay));

        return stats;
    }

    // 전체 회원 목록 조회
    public List<Member> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable).getContent();
    }

    // 회원 정지 처리
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
        // Dirty Checking으로 자동 저장됨
    }
}