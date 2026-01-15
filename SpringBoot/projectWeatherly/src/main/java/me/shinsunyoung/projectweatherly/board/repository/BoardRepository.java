package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 상태별 게시글 조회
    Page<Board> findByBoardStatus(BoardStatus boardStatus, Pageable pageable);

    // 제목 또는 내용으로 검색 (활성 상태만)
    @Query("SELECT b FROM Board b WHERE (b.title LIKE %:keyword% OR b.content LIKE %:keyword%) AND b.boardStatus = :status")
    Page<Board> findByTitleContainingOrContentContainingAndBoardStatus(
            @Param("keyword") String titleKeyword,
            @Param("keyword") String contentKeyword,
            @Param("status") BoardStatus status,
            Pageable pageable);

    // 인기글 조회 (좋아요 순)
    @Query("SELECT b FROM Board b WHERE b.boardStatus = 'ACTIVE' ORDER BY b.likeCount DESC")
    Page<Board> findPopularBoards(Pageable pageable);

    // 최신글 조회
    @Query("SELECT b FROM Board b WHERE b.boardStatus = 'ACTIVE' ORDER BY b.createdAt DESC")
    Page<Board> findRecentBoards(Pageable pageable);

    // 작성자의 게시글 조회
    Page<Board> findByMemberIdAndBoardStatus(Long memberId, BoardStatus boardStatus, Pageable pageable);
}