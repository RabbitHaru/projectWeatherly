package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MemberRepository memberRepository;
    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;

    // 1. 전체 회원 목록 조회 (페이징)
    public Page<Member> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    // 2. 회원 상세 조회
    public Member getMemberDetail(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    // 3. 회원 정지/해제 처리
    @Transactional
    public void updateMemberStatus(Long memberId, boolean isActive) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        member.setIsActive(isActive); // true: 해제, false: 정지
        // JPA Dirty Checking으로 인해 save 호출 없이 자동 업데이트
    }

    // 4. 대시보드용 통계 집계
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();

        // 기본 통계
        stats.put("totalMembers", memberRepository.count());
        stats.put("totalPosts", boardRepository.count());
        stats.put("totalReports", reportRepository.count());

        // ★ 처리 대기 중(PENDING)인 신고 건수 (관리자가 처리해야 할 일)
        long pendingCount = reportRepository.countByStatus("PENDING");
        stats.put("pendingReports", pendingCount);

        return stats;
    }
}