package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
}
