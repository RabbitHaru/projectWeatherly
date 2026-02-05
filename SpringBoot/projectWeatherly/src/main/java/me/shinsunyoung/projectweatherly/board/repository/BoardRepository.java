package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long>, JpaSpecificationExecutor<Board> {

    // ✅ 상태별 게시글 조회 메서드 추가
    @Query("SELECT b FROM Board b WHERE b.boardStatus = :status")
    Page<Board> findByBoardStatus(@Param("status") BoardStatus status, Pageable pageable);

    // 기존 메서드들
    Page<Board> findAllByOrderByViewCountDesc(Pageable pageable);
    Page<Board> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 회원별 게시글 조회
    @Query("SELECT b FROM Board b WHERE b.member.id = :memberId AND b.boardStatus = :status")
    Page<Board> findByMemberIdAndBoardStatus(
            @Param("memberId") Long memberId,
            @Param("status") BoardStatus status,
            Pageable pageable);

    // 게시글 수 조회
    @Query("SELECT COUNT(b) FROM Board b WHERE b.member.id = :memberId AND b.boardStatus = :status")
    long countByMemberIdAndBoardStatus(
            @Param("memberId") Long memberId,
            @Param("status") BoardStatus status);

    // 게시글 존재 여부 확인
    @Query("SELECT COUNT(b) > 0 FROM Board b WHERE b.member.id = :memberId AND b.id = :boardId AND b.boardStatus = :status")
    boolean existsByMemberIdAndIdAndBoardStatus(
            @Param("memberId") Long memberId,
            @Param("boardId") Long boardId,
            @Param("status") BoardStatus status);

    // ✅ 수정된 검색 메서드 (LOWER 함수 제거)
    @Query("SELECT b FROM Board b WHERE (b.title LIKE %:keyword% OR b.content LIKE %:keyword%) AND b.boardStatus = :status")
    Page<Board> findByTitleContainingOrContentContainingAndBoardStatus(
            @Param("keyword") String keyword,
            @Param("status") BoardStatus status,
            Pageable pageable);

    // 회원별 제목 검색
    @Query("SELECT b FROM Board b WHERE b.member.id = :memberId AND b.title LIKE %:keyword% AND b.boardStatus = :status")
    Page<Board> findByMemberIdAndTitleContainingAndBoardStatus(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            @Param("status") BoardStatus status,
            Pageable pageable);

    // 인기글 조회 (좋아요 + 조회수 기준)
    @Query("SELECT b FROM Board b WHERE b.boardStatus = :status ORDER BY b.likeCount DESC, b.viewCount DESC")
    Page<Board> findPopularBoards(@Param("status") BoardStatus status, Pageable pageable);

    // 최신글 조회
    @Query("SELECT b FROM Board b WHERE b.boardStatus = :status ORDER BY b.createdAt DESC")
    Page<Board> findRecentBoards(@Param("status") BoardStatus status, Pageable pageable);

    // 회원별 게시글 ID 목록 조회
    @Query("SELECT b.id FROM Board b WHERE b.member.id = :memberId AND b.boardStatus = :status")
    List<Long> findBoardIdsByMemberIdAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") BoardStatus status);

    // 카테고리별 게시글 조회
    @Query("SELECT b FROM Board b WHERE b.category = :category AND b.boardStatus = :status")
    Page<Board> findByCategoryAndBoardStatus(
            @Param("category") String category,
            @Param("status") BoardStatus status,
            Pageable pageable);

    // 게시글 검증 상태별 조회
    @Query("SELECT b FROM Board b WHERE b.isVerified = :isVerified AND b.boardStatus = :status")
    Page<Board> findByIsVerifiedAndBoardStatus(
            @Param("isVerified") Boolean isVerified,
            @Param("status") BoardStatus status,
            Pageable pageable);

    @Query("SELECT b FROM Board b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    Page<Board> findByTitleContainingOrContentContaining(@Param("keyword") String keyword, Pageable pageable);

    // ★ [추가] 일주일 간 작성된 글 중, 상태가 ACTIVE인 것을 조회 (페이징 지원)
    Page<Board> findByBoardStatusAndCreatedAtAfter(BoardStatus status, LocalDateTime createdAt, Pageable pageable);
    // 새로 추가된 메서드들
    Page<Board> findByMember(Member member, Pageable pageable);
    Page<Board> findByMemberAndBoardStatus(Member member, BoardStatus status, Pageable pageable);
    Page<Board> findByCategory(String category, Pageable pageable);

    // [관리자용] 특정 날짜(오늘 0시) 이후에 작성된 게시글 개수 조회
    long countByCreatedAtAfter(LocalDateTime date);
    // [추가] 공지사항 상위 3개 조회 (카테고리가 'notice'이고 상태가 ACTIVE인 것)
    List<Board> findTop3ByCategoryAndBoardStatusOrderByCreatedAtDesc(String category, BoardStatus status);
    // -------------------------------------------------------------------------
    // ★ [추가] 공지사항 제외 및 전체보기 로직을 위한 메서드
    // -------------------------------------------------------------------------

    // 1. 특정 카테고리가 '아닌' 게시글 조회 (예: 'notice' 제외하고 조회)
    Page<Board> findByCategoryNotAndBoardStatus(String category, BoardStatus status, Pageable pageable);

    // 2. 검색 시에도 특정 카테고리 제외하고 검색 (제목+내용)
    @Query("SELECT b FROM Board b WHERE (b.title LIKE %:keyword% OR b.content LIKE %:keyword%) AND b.category <> :excludeCategory AND b.boardStatus = :status")
    Page<Board> findByTitleOrContentContainingAndCategoryNot(
            @Param("keyword") String keyword,
            @Param("excludeCategory") String excludeCategory,
            @Param("status") BoardStatus status,
            Pageable pageable);
}
