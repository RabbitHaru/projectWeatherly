package me.shinsunyoung.projectweatherly.board.application;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.domain.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.domain.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.domain.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    // 게시글 생성
    public BoardResponse createBoard(Long memberId, BoardRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        Board board = Board.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .weatherCondition(request.getWeatherCondition())
                .imageUrl(request.getImageUrl())
                .build();

        Board savedBoard = boardRepository.save(board);
        return convertToResponse(savedBoard);
    }

    // 게시글 조회 (조회수 증가)
    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board);

        return convertToResponse(board);
    }

    // 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    // 게시글 수정
    public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setWeatherCondition(request.getWeatherCondition());
        board.setImageUrl(request.getImageUrl());

        if (request.getBoardStatus() != null) {
            board.setBoardStatus(request.getBoardStatus());
        }

        if (request.getIsVerified() != null) {
            board.setIsVerified(request.getIsVerified());
        }

        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard);
    }

    // 게시글 삭제 (상태 변경)
    public void deleteBoard(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        board.setBoardStatus(BoardStatus.DELETED);
        boardRepository.save(board);
    }

    // 게시글 검증
    public BoardResponse verifyBoard(Long boardId, Boolean isVerified) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        board.setIsVerified(isVerified);
        Board verifiedBoard = boardRepository.save(board);
        return convertToResponse(verifiedBoard);
    }

    // 좋아요 증가
    public BoardResponse likeBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        board.increaseLikeCount();
        Board likedBoard = boardRepository.save(board);
        return convertToResponse(likedBoard);
    }

    // 검색 기능
    @Transactional(readOnly = true)
    public Page<BoardResponse> searchBoards(String keyword, Pageable pageable) {
        return boardRepository.searchByKeyword(keyword, pageable)
                .map(this::convertToResponse);
    }

    // 날씨 상태별 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getBoardsByWeatherCondition(String weatherCondition, Pageable pageable) {
        return boardRepository.findByWeatherCondition(weatherCondition, pageable)
                .map(this::convertToResponse);
    }

    // 인기글 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findAllByOrderByViewCountDesc(pageable)
                .map(this::convertToResponse);
    }

    // 최신글 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::convertToResponse);
    }

    // DTO 변환 메서드
    private BoardResponse convertToResponse(Board board) {
        return BoardResponse.builder()
                .boardId(board.getId())
                .memberId(board.getMember().getId())
                .memberNickname(board.getMember().getNickname())
                .memberProfileImage(board.getMember().getProfileImage())
                .title(board.getTitle())
                .content(board.getContent())
                .weatherCondition(board.getWeatherCondition())
                .imageUrl(board.getImageUrl())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .isVerified(board.getIsVerified())
                .boardStatus(board.getBoardStatus())
                .boardStatusDescription(board.getBoardStatus() != null ? board.getBoardStatus().getDescription() : null)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}