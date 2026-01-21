package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.dto.response.ReportResponse;
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

    // 1. 신고 생성 (★ 삭제된 글 신고 방지 로직 포함)
    public boolean createReport(Long reporterId, String type, Long targetId, String reason, String details) {
        try {
            Member reporter = memberRepository.findById(reporterId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            // 이미 내가 신고했는지 중복 체크
            boolean alreadyReported = reportRepository.existsByReporterIdAndTypeAndTargetId(
                    reporterId, type, targetId);

            if (alreadyReported) return false; // 이미 신고했으면 false 반환

            Report.ReportBuilder reportBuilder = Report.builder()
                    .reporter(reporter)
                    .type(type)
                    .targetId(targetId)
                    .reason(reason)
                    .details(details)
                    .status(ReportStatus.PENDING)
                    .createdAt(LocalDateTime.now());

            if ("POST".equalsIgnoreCase(type)) {
                Board board = boardRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

                // ★ [핵심] 이미 삭제된 글(DELETED)이면 신고 차단
                if (board.getBoardStatus() == BoardStatus.DELETED) {
                    throw new IllegalArgumentException("이미 삭제된 게시글은 신고할 수 없습니다.");
                }

                reportBuilder.targetBoard(board);

            } else if ("COMMENT".equalsIgnoreCase(type)) {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

                // ★ [핵심] 원글이 삭제되었으면 댓글 신고도 차단
                if (comment.getBoard().getBoardStatus() == BoardStatus.DELETED) {
                    throw new IllegalArgumentException("삭제된 게시글의 댓글은 신고할 수 없습니다.");
                }

                reportBuilder.targetComment(comment);
            }

            reportRepository.save(reportBuilder.build());
            return true;

        } catch (IllegalArgumentException e) {
            // "이미 삭제된..." 에러는 여기서 잡아서 로그 남기고 던짐 (컨트롤러가 처리하도록)
            log.warn("신고 거부: {}", e.getMessage());
            throw e;
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

    // 3. [관리자] 대기 중인 신고 목록
    @Transactional(readOnly = true)
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable)
                .map(ReportResponse::from);
    }

    // 4. [관리자] 처리된 신고 내역 조회 (History)
    @Transactional(readOnly = true)
    public Page<ReportResponse> getProcessedReports(Pageable pageable) {
        return reportRepository.findByStatusNotOrderByProcessedAtDesc(ReportStatus.PENDING, pageable)
                .map(ReportResponse::from);
    }

    // 5. 신고 처리 로직 (기록 + 정지 + 삭제)
    public void processReport(Long reportId, String statusStr, int banDays) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        ReportStatus status;
        try {
            status = ReportStatus.valueOf(statusStr.toUpperCase());
        } catch (Exception e) {
            status = ReportStatus.RESOLVED;
        }

        report.setStatus(status);
        report.setProcessedAt(LocalDateTime.now());

        // 기록(Log) 생성
        StringBuilder auditLog = new StringBuilder();
        if (report.getDetails() != null) auditLog.append(report.getDetails()).append("\n");
        auditLog.append("[관리자 처리] 상태: ").append(status);

        Member offender = null;
        if ("POST".equalsIgnoreCase(report.getType()) && report.getTargetBoard() != null) {
            offender = report.getTargetBoard().getMember();
        } else if ("COMMENT".equalsIgnoreCase(report.getType()) && report.getTargetComment() != null) {
            offender = report.getTargetComment().getMember();
        }

        // 정지 처리
        if (banDays > 0 && offender != null) {
            offender.setIsActive(false);
            offender.setBanExpiresAt(LocalDateTime.now().plusDays(banDays));
            auditLog.append(", 제재: ").append(banDays).append("일 정지");
            log.info("회원 제재 적용: ID={}, 기간={}일", offender.getId(), banDays);
        }

        // 승인 시 콘텐츠 삭제 (소프트 삭제)
        if (status == ReportStatus.RESOLVED || status == ReportStatus.ACCEPTED) {
            deleteReportedContent(report);
            auditLog.append(", 콘텐츠 삭제됨(Soft)");
        }

        report.setDetails(auditLog.toString());
    }

    // 6. 콘텐츠 삭제 (소프트 삭제 적용으로 500 에러 방지)
    private void deleteReportedContent(Report report) {
        try {
            if ("POST".equalsIgnoreCase(report.getType()) && report.getTargetBoard() != null) {
                Board board = report.getTargetBoard();
                board.setBoardStatus(BoardStatus.DELETED); // 강제 삭제(delete) 대신 상태 변경
                boardRepository.save(board);
            } else if ("COMMENT".equalsIgnoreCase(report.getType()) && report.getTargetComment() != null) {
                commentRepository.delete(report.getTargetComment());
            }
        } catch (Exception e) {
            log.error("삭제 실패: {}", e.getMessage());
        }
    }
}