package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    public boolean createReport(Long reporterId, String type, Long targetId, String reason, String details) {
        try {
            Member reporter = memberRepository.findById(reporterId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            // 중복 신고 체크
            boolean alreadyReported = reportRepository.existsByReporterIdAndTypeAndTargetId(
                    reporterId, type, targetId);

            if (alreadyReported) {
                return false;
            }

            // Report 생성 - status를 문자열 "PENDING"으로 설정
            Report.ReportBuilder reportBuilder = Report.builder()
                    .reporter(reporter)
                    .type(type)
                    .targetId(targetId)
                    .reason(reason)
                    .details(details)
                    .status("PENDING")  // 문자열로 설정
                    .createdAt(LocalDateTime.now());

            // 신고 대상 정보 설정
            if ("post".equals(type)) {
                Board board = boardRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
                reportBuilder.targetBoard(board);
            } else if ("comment".equals(type)) {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
                reportBuilder.targetComment(comment);
            }

            Report report = reportBuilder.build();
            reportRepository.save(report);

            log.info("신고 생성 완료 - type: {}, targetId: {}, reporterId: {}", type, targetId, reporterId);
            return true;
        } catch (Exception e) {
            log.error("신고 생성 실패: ", e);
            throw new RuntimeException("신고 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 내가 신고한 목록 조회
    @Transactional(readOnly = true)
    public List<Report> getMyReports(Long memberId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId);
    }

    // 신고 수 가져오기
    @Transactional(readOnly = true)
    public int getReportCountByMemberId(Long memberId) {
        return reportRepository.countByReporterId(memberId);
    }

    // 신고 취소
    public void cancelReport(Long reportId, Long memberId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));

        if (!report.getReporter().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 신고만 취소할 수 있습니다.");
        }

        if (!"PENDING".equals(report.getStatus())) {
            throw new IllegalArgumentException("처리 중이거나 완료된 신고는 취소할 수 없습니다.");
        }

        reportRepository.delete(report);
        log.info("신고 취소 - reportId: {}, memberId: {}", reportId, memberId);
    }
}