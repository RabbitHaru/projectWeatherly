package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private String type;          // "게시글" 또는 "댓글"
    private String targetContent; // 신고한 글 제목 또는 댓글 내용 요약
    private String reason;        // 신고 사유 (화면 표시용)
    private String status;        // 처리 상태 (대기중, 완료 등)
    private String statusClass;   // 상태별 뱃지 색상 (CSS용)
    private String createdAt;     // 작성일

    // Entity -> DTO 변환 메서드
    public static ReportResponse from(Report report) {
        // 1. 신고 대상 내용 요약 (게시글이면 제목, 댓글이면 내용)
        String content = "삭제된 항목입니다.";

        if ("post".equals(report.getType()) && report.getTargetBoard() != null) {
            content = report.getTargetBoard().getTitle();
        } else if ("comment".equals(report.getType()) && report.getTargetComment() != null) {
            content = report.getTargetComment().getContent();
        } else if ("POST".equals(report.getType()) && report.getTargetBoard() != null) { // 대소문자 호환
            content = report.getTargetBoard().getTitle();
        } else if ("COMMENT".equals(report.getType()) && report.getTargetComment() != null) {
            content = report.getTargetComment().getContent();
        }

        // 내용이 너무 길면 자르기 (20자)
        if (content != null && content.length() > 20) {
            content = content.substring(0, 20) + "...";
        }

        // 2. 사유 한글 변환
        String reasonDisplay = report.getReason();
        if ("spam".equals(reasonDisplay)) reasonDisplay = "스팸/광고";
        else if ("abuse".equals(reasonDisplay)) reasonDisplay = "욕설/비하";
        else if ("illegal".equals(reasonDisplay)) reasonDisplay = "불법 정보";
        else if ("other".equals(reasonDisplay)) reasonDisplay = "기타 사유";

        // 3. 상태 한글 변환 및 CSS 클래스 지정
        String statusDisplay = "대기중";
        String cssClass = "status-pending"; // 노란색 (CSS에 정의 필요)

        if ("COMPLETED".equals(report.getStatus()) || "RESOLVED".equals(report.getStatus())) {
            statusDisplay = "처리완료";
            cssClass = "status-completed"; // 초록색
        } else if ("REJECTED".equals(report.getStatus())) {
            statusDisplay = "반려됨";
            cssClass = "status-rejected"; // 빨간색
        }

        return ReportResponse.builder()
                .id(report.getId())
                .type("post".equalsIgnoreCase(report.getType()) ? "게시글" : "댓글")
                .targetContent(content)
                .reason(reasonDisplay)
                .status(statusDisplay)
                .statusClass(cssClass)
                .createdAt(report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .build();
    }
}