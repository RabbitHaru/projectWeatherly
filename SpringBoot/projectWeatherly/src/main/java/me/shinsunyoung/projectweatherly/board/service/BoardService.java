package me.shinsunyoung.projectweatherly.board.service;

import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BoardService {
    // 기본 CRUD
    Page<BoardResponse> getAllBoards(Pageable pageable);
    Page<BoardResponse> getPopularBoards(Pageable pageable);
    Page<BoardResponse> getRecentBoards(Pageable pageable);
    Page<BoardResponse> searchBoards(String keyword, Pageable pageable);
    BoardResponse getBoard(Long id);
    BoardResponse createBoard(Long memberId, BoardRequest request) throws IOException;
    BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request);
    void deleteBoard(Long boardId, Long memberId);

    // 추가 기능
    Page<BoardResponse> getBoardsByMemberId(Long memberId, Pageable pageable);
    long countByMemberId(Long memberId);
    List<BoardResponse> getMyBoardsSimple(Long memberId);
    boolean existsByMemberIdAndBoardId(Long memberId, Long boardId);
    Page<BoardResponse> getBoardsByCategory(String category, Pageable pageable);
    BoardResponse verifyBoard(Long boardId, Boolean isVerified);
    List<Board> getMyBoardsForMyPage(Long memberId);
    BoardResponse changeBoardStatus(Long boardId, BoardStatus status);
    BoardResponse increaseLike(Long boardId);
    BoardResponse decreaseLike(Long boardId);
    List<Long> getBoardIdsByMemberId(Long memberId);
    List<BoardResponse> searchMyBoardsByTitle(Long memberId, String keyword);
    Board getBoardEntity(Long boardId);
    List<BoardResponse> getBoardsByIds(List<Long> boardIds);
    boolean isLiked(Long boardId, Long userId);
    // 좋아요 관련
    boolean toggleLike(Long boardId, Long memberId);
    int getLikeCount(Long boardId);
    void increaseViewCount(Long boardId);
    int getViewCount(Long boardId);
    Page<BoardResponse> getMyBoards(Long memberId, Pageable pageable);
    // ★ [추가] 주간 인기글 가져오기
    List<BoardResponse> getWeeklyPopularBoards(int limit);
}