package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Builder;
import lombok.Getter;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class MyCommentResponse {
    private Long commentId;
    private String content;
    private Long boardId;       // 댓글이 달린 게시글 ID (이동용)
    private String boardTitle;  // 댓글이 달린 게시글 제목
    private String createdAt;   // 작성일
    private Integer likeCount;  // 좋아요 수

    public static MyCommentResponse from(Comment comment) {
        return MyCommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .boardId(comment.getBoard().getId())
                .boardTitle(comment.getBoard().getTitle())
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }
}