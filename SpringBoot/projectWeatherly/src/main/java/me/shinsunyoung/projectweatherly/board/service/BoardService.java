package me.shinsunyoung.projectweatherly.board.service;

import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardService {

    // 기존 메서드들
    Page<BoardResponse> getAllBoards(Pageable pageable);
    Page<BoardResponse> getPopularBoards(Pageable pageable);
    Page<BoardResponse> getRecentBoards(Pageable pageable);
    Page<BoardResponse> searchBoards(String keyword, Pageable pageable);
    BoardResponse getBoard(Long id);
    BoardResponse createBoard(Long memberId, BoardRequest request);
    BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request);
    void deleteBoard(Long boardId, Long memberId);

    // 새로 추가된 메서드들
    boolean toggleLike(Long boardId, Long memberId);
    int getLikeCount(Long boardId);
    void increaseViewCount(Long boardId);
    Page<BoardResponse> getMyBoards(Long memberId, Pageable pageable);
    Page<BoardResponse> getBoardsByCategory(String category, Pageable pageable);
}