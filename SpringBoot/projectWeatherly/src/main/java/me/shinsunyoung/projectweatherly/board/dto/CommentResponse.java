package me.shinsunyoung.projectweatherly.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String writer;  // 작성자 닉네임
    private Long boardId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 선택적 필드
    private Integer likeCount;
    private Long memberId;
    private String memberNickname;
    private boolean isLiked;

    // 빌더 메서드들
    public static class CommentResponseBuilder {
        public CommentResponseBuilder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public CommentResponseBuilder memberNickname(String memberNickname) {
            this.memberNickname = memberNickname;
            return this;
        }

        public CommentResponseBuilder likeCount(Integer likeCount) {
            this.likeCount = likeCount;
            return this;
        }
    }
}