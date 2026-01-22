package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private String type;
    private Long targetId;
    private String reporterName;
    private String rawStatus;
    private String targetContent;
    private String reason;
    private String status;
    private String statusClass;
    private String createdAt;

    // ★ [필수] 이 필드들이 없으면 HTML 에러 발생
    private String processedAt;
    private boolean isDeleted;
    private String penaltyDetail;
    private String details;

    public static ReportResponse from(Report report) {
        String content = "내용 없음";
        Long linkId = null;
        boolean deleted = false;

        if (report.getType() != null) {
            String t = report.getType().toUpperCase();
            if (("POST".equals(t) || "게시글".equals(t)) && report.getTargetBoard() != null) {
                Board b = report.getTargetBoard();
                content = b.getTitle();
                linkId = b.getId();
                deleted = (b.getBoardStatus() == BoardStatus.DELETED);
            }
            else if (("COMMENT".equals(t) || "댓글".equals(t)) && report.getTargetComment() != null) {
                content = report.getTargetComment().getContent();
                if (report.getTargetComment().getBoard() != null) {
                    Board b = report.getTargetComment().getBoard();
                    linkId = b.getId();
                    deleted = (b.getBoardStatus() == BoardStatus.DELETED);
                }
            }
        }

        if (content != null && content.length() > 20) content = content.substring(0, 20) + "...";

        String reasonDisplay = report.getReason();
        if (reasonDisplay != null) {
            if ("spam".equalsIgnoreCase(reasonDisplay)) reasonDisplay = "스팸/광고";
            else if ("abuse".equalsIgnoreCase(reasonDisplay)) reasonDisplay = "욕설/비하";
            else if ("illegal".equalsIgnoreCase(reasonDisplay)) reasonDisplay = "불법 정보";
            else if ("other".equalsIgnoreCase(reasonDisplay)) reasonDisplay = "기타 사유";
        }

        String statusStr = report.getStatus() != null ? report.getStatus().toString() : "PENDING";
        String statusDisplay = "대기중";
        String cssClass = "status-pending";

        if ("RESOLVED".equals(statusStr) || "ACCEPTED".equals(statusStr) || "COMPLETED".equals(statusStr)) {
            statusDisplay = "처리완료";
            cssClass = "status-completed";
        } else if ("REJECTED".equals(statusStr)) {
            statusDisplay = "반려됨";
            cssClass = "status-rejected";
        }

        String pAt = report.getProcessedAt() != null ?
                report.getProcessedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "-";

        String rName = (report.getReporter() != null) ? report.getReporter().getNickname() : "알 수 없음";

        // 제재 내용 추출 로직
        String penalty = "-";
        String fullDetails = report.getDetails();
        if (fullDetails != null && fullDetails.contains("제재: ")) {
            try {
                int start = fullDetails.indexOf("제재: ");
                int end = fullDetails.indexOf(",", start);
                if (end == -1) end = fullDetails.indexOf("\n", start);
                if (end == -1) end = fullDetails.length();
                penalty = fullDetails.substring(start, end).replace("제재: ", "").trim();
            } catch (Exception e) {
                penalty = "확인 필요";
            }
        } else if ("RESOLVED".equals(statusStr)) {
            penalty = "조치됨";
        }

        return ReportResponse.builder()
                .id(report.getId())
                .type("post".equalsIgnoreCase(report.getType()) ? "게시글" : "댓글")
                .targetId(linkId)
                .targetContent(content)
                .reporterName(rName)
                .reason(reasonDisplay)
                .status(statusDisplay)
                .rawStatus(statusStr)
                .statusClass(cssClass)
                .createdAt(report.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd")))
                .processedAt(pAt)
                .isDeleted(deleted)
                .penaltyDetail(penalty) // ★ 필드 채우기
                .details(report.getDetails() != null ? report.getDetails() : "") // ★ null 방지
                .build();
    }
}