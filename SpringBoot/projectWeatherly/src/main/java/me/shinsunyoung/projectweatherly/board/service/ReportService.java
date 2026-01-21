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
import me.shinsunyoung.projectweatherly.member.dto.response.ReportResponse; // ★ DTO Import 추가
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 1. 신고 생성
    public boolean createReport(Long reporterId, String type, Long targetId, String reason, String details) {
        try {
            Member reporter = memberRepository.findById(reporterId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            boolean alreadyReported = reportRepository.existsByReporterIdAndTypeAndTargetId(
                    reporterId, type, targetId);

            if (alreadyReported) return false;

            Report.ReportBuilder reportBuilder = Report.builder()
                    .reporter(reporter)
                    .type(type)
                    .targetId(targetId)
                    .reason(reason)
                    .details(details)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now());

            if ("POST".equalsIgnoreCase(type)) {
                Board board = boardRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
                reportBuilder.targetBoard(board);
            } else if ("COMMENT".equalsIgnoreCase(type)) {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
                reportBuilder.targetComment(comment);
            }

            reportRepository.save(reportBuilder.build());
            log.info("신고 접수 완료 - type: {}, targetId: {}", type, targetId);
            return true;
        } catch (Exception e) {
            log.error("신고 생성 실패: ", e);
            throw new RuntimeException("신고 처리 중 오류 발생");
        }
    }

    // 2. 내가 신고한 목록
    @Transactional(readOnly = true)
    public List<Report> getMyReports(Long memberId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId);
    }

    // ★ 3. [수정됨] 전체 신고 목록 페이징 (DTO 변환 포함)
    // 트랜잭션 안에서 변환하므로 LazyInitializationException 해결!
    @Transactional(readOnly = true)
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        return reportRepository.findAll(pageable)
                .map(ReportResponse::from);
    }

    // 4. [관리자용] 신고 처리 (승인/반려)
    public void processReport(Long reportId, String status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        report.setStatus(status);

        if ("ACCEPTED".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status)) {
            deleteReportedContent(report);
        }

        log.info("신고 처리 완료 - id: {}, status: {}", reportId, status);
    }

    // 5. 신고 콘텐츠 삭제 (내부 메서드)
    private void deleteReportedContent(Report report) {
        try {
            if ("POST".equalsIgnoreCase(report.getType()) && report.getTargetBoard() != null) {
                boardRepository.delete(report.getTargetBoard());
                log.info("게시글 삭제 완료 - id: {}", report.getTargetId());
            } else if ("COMMENT".equalsIgnoreCase(report.getType()) && report.getTargetComment() != null) {
                commentRepository.delete(report.getTargetComment());
                log.info("댓글 삭제 완료 - id: {}", report.getTargetId());
            }
        } catch (Exception e) {
            log.error("콘텐츠 삭제 실패 (이미 삭제됨?): {}", e.getMessage());
        }
    }
}