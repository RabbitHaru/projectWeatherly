package me.shinsunyoung.projectweatherly.admin.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;

    // 1. 대시보드 통계
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalMembers", memberRepository.count());
        stats.put("pendingReports", reportRepository.count());
        stats.put("todayPosts", boardRepository.count());
        return stats;
    }

    // 2. 전체 회원 목록 조회 (페이징)
    public Page<Member> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    // ★ 3. [변경됨] 회원 정지 및 기간 설정
    // days > 0 : 해당 기간만큼 정지
    // days == 0 : 정지 해제
    @Transactional
    public void suspendMember(Long memberId, int days) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원을 찾을 수 없습니다. id=" + memberId));

        if (days > 0) {
            // 정지 처리
            member.setIsActive(false);
            member.setBanExpiresAt(LocalDateTime.now().plusDays(days)); // 현재시간 +일수
        } else {
            // 정지 해제
            member.setIsActive(true);
            member.setBanExpiresAt(null); // 날짜 초기화
        }
    }
}