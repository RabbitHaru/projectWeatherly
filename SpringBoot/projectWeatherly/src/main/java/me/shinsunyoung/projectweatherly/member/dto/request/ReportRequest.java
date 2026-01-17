// ReportRequest.java
package me.shinsunyoung.projectweatherly.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportType;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {
    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long boardId;

    @NotNull(message = "신고 유형은 필수입니다.")
    private ReportType reportType;

    @NotBlank(message = "신고 사유는 필수입니다.")
    private String reason;
}