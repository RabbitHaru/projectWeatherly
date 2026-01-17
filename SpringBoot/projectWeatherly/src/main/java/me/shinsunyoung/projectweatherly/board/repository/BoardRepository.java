package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Board;

import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 기존 메서드들
    Page<Board> findAllByOrderByViewCountDesc(Pageable pageable);
    Page<Board> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT b FROM Board b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    Page<Board> findByTitleContainingOrContentContaining(@Param("keyword") String keyword, Pageable pageable);

    // 새로 추가된 메서드들
    Page<Board> findByMember(Member member, Pageable pageable);
    Page<Board> findByCategory(String category, Pageable pageable);
}