// ReportResponse.java
package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private Long boardId;
    private ReportType reportType;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}