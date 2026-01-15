package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.BoardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    // 게시글 ID로 이미지 목록 조회
    List<BoardImage> findByBoardIdOrderByDisplayOrderAsc(Long boardId);

    // 게시글 ID와 이미지 ID로 삭제
    @Transactional
    @Modifying
    @Query("DELETE FROM BoardImage bi WHERE bi.board.id = :boardId AND bi.id = :imageId")
    void deleteByBoardIdAndImageId(@Param("boardId") Long boardId, @Param("imageId") Long imageId);

    // 게시글 ID로 모든 이미지 삭제
    @Transactional
    @Modifying
    @Query("DELETE FROM BoardImage bi WHERE bi.board.id = :boardId")
    void deleteAllByBoardId(@Param("boardId") Long boardId);
}