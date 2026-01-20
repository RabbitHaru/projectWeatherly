package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    int countByBoardId(Long boardId);
    List<Comment> findByBoardIdOrderByCreatedAtAsc(Long boardId);
    List<Comment> findByBoardId(Long boardId);  // 정렬 없이 조회
}