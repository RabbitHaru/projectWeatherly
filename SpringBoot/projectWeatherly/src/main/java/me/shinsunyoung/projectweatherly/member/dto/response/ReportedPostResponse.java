package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportedPostResponse {
    private Long postId;
    private String title;
    private String content;
    private LocalDateTime reportedAt;
    private String status; // 처리 상태
    public ReportedPostResponse(Report report){
        this.postId = report.getId();
        this.title = report.getReason();
        this.content = report.getDetails();
        this.reportedAt = report.getCreatedAt();
        this.status = report.getStatus();
    }
}
