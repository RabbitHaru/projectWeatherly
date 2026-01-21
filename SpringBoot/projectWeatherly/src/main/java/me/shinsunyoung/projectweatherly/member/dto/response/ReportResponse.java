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

    // ★ [핵심] 이 3개 필드가 없으면 화면이 하얗게 변합니다!
    private Long targetId;        // 신고 대상 ID (링크용)
    private String reporterName;  // 신고자 닉네임 (화면 표시용)
    private String rawStatus;     // 상태 원본 (PENDING 등 - 버튼 로직용)

    private String targetContent; // 내용 요약
    private String reason;        // 신고 사유 (한글)
    private String status;        // 상태 (한글)
    private String statusClass;   // CSS 클래스
    private String createdAt;     // 작성일

    public static ReportResponse from(Report report) {
        // 1. 내용 요약
        String content = "삭제된 항목입니다.";
        if (report.getType() != null) {
            String t = report.getType().toUpperCase();
            if (("POST".equals(t) || "게시글".equals(t)) && report.getTargetBoard() != null) {
                content = report.getTargetBoard().getTitle();
            } else if (("COMMENT".equals(t) || "댓글".equals(t)) && report.getTargetComment() != null) {
                content = report.getTargetComment().getContent();
            }
        }
        if (content != null && content.length() > 20) content = content.substring(0, 20) + "...";

        // 2. 사유 변환 (other -> 기타 사유)
        String reasonDisplay = report.getReason();
        if (reasonDisplay != null) {
            String r = reasonDisplay.trim(); // 공백 제거
            if ("spam".equalsIgnoreCase(r)) reasonDisplay = "스팸/광고";
            else if ("abuse".equalsIgnoreCase(r)) reasonDisplay = "욕설/비하";
            else if ("illegal".equalsIgnoreCase(r)) reasonDisplay = "불법 정보";
            else if ("other".equalsIgnoreCase(r)) reasonDisplay = "기타 사유"; // ★ 여기가 있어야 한글로 나옵니다
        }

        // 3. 상태 변환
        String statusStr = report.getStatus() != null ? report.getStatus().toString() : "PENDING";
        String statusDisplay = "대기중";
        String cssClass = "status-pending";

        if ("COMPLETED".equals(statusStr) || "RESOLVED".equals(statusStr)) {
            statusDisplay = "처리완료";
            cssClass = "status-completed";
        } else if ("REJECTED".equals(statusStr)) {
            statusDisplay = "반려됨";
            cssClass = "status-rejected";
        }

        // 4. 신고자 이름 안전하게 가져오기
        String rName = (report.getReporter() != null) ? report.getReporter().getNickname() : "알 수 없음";

        return ReportResponse.builder()
                .id(report.getId())
                .type("post".equalsIgnoreCase(report.getType()) ? "게시글" : "댓글")
                .targetId(report.getTargetId())
                .targetContent(content)
                .reporterName(rName)   // ★ DTO에 담기
                .reason(reasonDisplay)
                .status(statusDisplay)
                .rawStatus(statusStr)  // ★ DTO에 담기
                .statusClass(cssClass)
                .createdAt(report.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd")))
                .build();
    }
}