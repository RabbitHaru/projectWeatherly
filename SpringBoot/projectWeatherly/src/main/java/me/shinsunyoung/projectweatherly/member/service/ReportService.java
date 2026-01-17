package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.service.BoardService;
import me.shinsunyoung.projectweatherly.member.dto.request.ReportRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.ReportResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.ReportedPostResponse;
import me.shinsunyoung.projectweatherly.report.domain.Report;
import me.shinsunyoung.projectweatherly.report.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final BoardService boardService;

    // 신고하기
    @Transactional
    public ReportResponse createReport(Long reporterId, ReportRequest request) {
        // 중복 신고 체크 - Repository 메서드 호출 수정
        List<Report> existingReports = reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
        boolean alreadyReported = existingReports.stream()
                .anyMatch(report -> report.getBoardId().equals(request.getBoardId()));

        if (alreadyReported) {
            throw new IllegalArgumentException("이미 신고한 게시물입니다.");
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .boardId(request.getBoardId())
                .reportType(request.getReportType())
                .reason(request.getReason())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Report savedReport = reportRepository.save(report);
        return convertToResponse(savedReport);
    }

    // 내가 신고한 게시물 목록 조회
    public List<ReportedPostResponse> getMyReports(Long memberId) {
        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId);

        return reports.stream().map(report -> {
            try {
                // 게시물 정보 조회
                var boardResponse = boardService.getBoard(report.getBoardId());

                return new ReportedPostResponse(
                        boardResponse.getId(),
                        boardResponse.getTitle(),
                        // 내용이 너무 길면 줄임
                        boardResponse.getContent() != null && boardResponse.getContent().length() > 100
                                ? boardResponse.getContent().substring(0, 100) + "..."
                                : boardResponse.getContent(),
                        report.getCreatedAt(),
                        report.getStatus().toString()
                );
            } catch (Exception e) {
                log.warn("게시물 조회 실패: 게시물 ID={}, 오류: {}", report.getBoardId(), e.getMessage());
                return new ReportedPostResponse(
                        report.getBoardId(),
                        "삭제된 게시물",
                        "해당 게시물은 삭제되었거나 존재하지 않습니다.",
                        report.getCreatedAt(),
                        report.getStatus().toString()
                );
            }
        }).collect(Collectors.toList());
    }

    // 신고 수 가져오기
    public int getReportCountByMemberId(Long memberId) {
        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(memberId);
        return reports.size();
    }

    // 신고 취소 (대기 중인 신고만)
    @Transactional
    public void cancelReport(Long reportId, Long memberId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));

        if (!report.getReporterId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 신고만 취소할 수 있습니다.");
        }

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalArgumentException("처리 중이거나 완료된 신고는 취소할 수 없습니다.");
        }

        reportRepository.delete(report);
    }

    private ReportResponse convertToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .boardId(report.getBoardId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .processedAt(report.getProcessedAt())
                .build();
    }
}