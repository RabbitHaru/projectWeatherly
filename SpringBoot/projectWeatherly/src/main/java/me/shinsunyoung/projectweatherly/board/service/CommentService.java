package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.entity.Comment;  // 올바른 Comment 엔티티 import
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    public List<CommentResponse> getCommentsByBoardId(Long boardId) {
        List<Comment> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId);
        return comments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Integer getCommentCountByBoardId(Long boardId) {
        return commentRepository.countByBoardId(boardId);
    }

    private CommentResponse convertToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(comment.getWriter())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}