package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardLike;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.repository.BoardLikeRepository;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final BoardLikeRepository boardLikeRepository;

    @Override
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findAll(pageable).map(this::convertToResponse);
    }

    @Override
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findAllByOrderByViewCountDesc(pageable).map(this::convertToResponse);
    }

    @Override
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::convertToResponse);
    }

    @Override
    public Page<BoardResponse> searchBoards(String keyword, Pageable pageable) {
        return boardRepository.findByTitleContainingOrContentContaining(keyword, pageable)
                .map(this::convertToResponse);
    }

    @Override
    public BoardResponse getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        return convertToResponse(board);
    }

    @Override
    public BoardResponse createBoard(Long memberId, BoardRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Board board = Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .member(member)
                .viewCount(0)
                .likeCount(0)
                .build();

        boardRepository.save(board);
        return convertToResponse(board);
    }

    @Override
    public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 권한 체크
        if (!board.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("게시글 수정 권한이 없습니다.");
        }

        board.update(request.getTitle(), request.getContent(), request.getCategory());
        boardRepository.save(board);
        return convertToResponse(board);
    }

    @Override
    public void deleteBoard(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 권한 체크
        if (!board.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("게시글 삭제 권한이 없습니다.");
        }

        boardRepository.delete(board);
    }

    @Override
    public boolean toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Optional<BoardLike> existingLike = boardLikeRepository.findByBoardAndMember(board, member);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            boardLikeRepository.delete(existingLike.get());
            board.decreaseLikeCount();
            boardRepository.save(board);
            return false;
        } else {
            // 좋아요 추가
            BoardLike boardLike = BoardLike.builder()
                    .board(board)
                    .member(member)
                    .build();
            boardLikeRepository.save(boardLike);
            board.increaseLikeCount();
            boardRepository.save(board);
            return true;
        }
    }

    @Override
    public int getLikeCount(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        return board.getLikeCount();
    }

    @Override
    public void increaseViewCount(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        board.increaseViewCount();
        boardRepository.save(board);
    }

    @Override
    public Page<BoardResponse> getMyBoards(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return boardRepository.findByMember(member, pageable).map(this::convertToResponse);
    }

    @Override
    public Page<BoardResponse> getBoardsByCategory(String category, Pageable pageable) {
        return boardRepository.findByCategory(category, pageable).map(this::convertToResponse);
    }

    private BoardResponse convertToResponse(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .memberId(board.getMember().getId())
                .memberNickname(board.getMember().getNickname())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}