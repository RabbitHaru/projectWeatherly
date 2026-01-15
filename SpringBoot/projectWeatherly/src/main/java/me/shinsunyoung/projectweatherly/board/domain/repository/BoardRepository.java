package me.shinsunyoung.projectweatherly.board.domain.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 사용자별 게시글 조회
    Page<Board> findByMemberId(Long memberId, Pageable pageable);

    // 검증된 게시글 조회
    Page<Board> findByIsVerifiedTrue(Pageable pageable);

    // 상태별 게시글 조회
    Page<Board> findByBoardStatus(BoardStatus boardStatus, Pageable pageable);

    // 제목 또는 내용으로 검색
    @Query("SELECT b FROM Board b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    Page<Board> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 날씨 상태별 조회
    Page<Board> findByWeatherCondition(String weatherCondition, Pageable pageable);

    // 인기글 조회 (조회수 기준)
    Page<Board> findAllByOrderByViewCountDesc(Pageable pageable);

    // 최신글 조회
    Page<Board> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 사용자 ID와 상태로 조회
    List<Board> findByMemberIdAndBoardStatus(Long memberId, BoardStatus boardStatus);
}