package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private String writer;
    private Long boardId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}