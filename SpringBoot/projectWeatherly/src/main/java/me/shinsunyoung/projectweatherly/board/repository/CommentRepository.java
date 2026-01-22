package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 댓글 조회
    int countByBoardId(Long boardId);
    List<Comment> findByBoardIdOrderByCreatedAtAsc(Long boardId);
    List<Comment> findByBoardId(Long boardId);



    // [추가됨] 내가 쓴 댓글 목록 조회 (최신순 + 페이징)
    Page<Comment> findByMemberOrderByCreatedAtDesc(Member member, Pageable pageable);

    // [추가됨] 내가 쓴 댓글 개수 카운트
    int countByMember(Member member);
}