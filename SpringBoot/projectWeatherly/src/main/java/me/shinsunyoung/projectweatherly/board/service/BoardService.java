package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardImage;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ImageUploadService imageUploadService;

    // 게시글 생성
    @Transactional
    public BoardResponse createBoard(Long memberId, BoardRequest request) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Board board = Board.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        // 이미지 업로드 및 첨부
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            for (MultipartFile imageFile : request.getImageFiles()) {
                String imageUrl = imageUploadService.uploadImage(imageFile);
                BoardImage boardImage = BoardImage.builder()
                        .imageUrl(imageUrl)
                        .isThumbnail(board.getImages().isEmpty()) // 첫 번째 이미지를 썸네일로 설정
                        .board(board)
                        .build();
                board.addImage(boardImage);
            }
        }

        Board savedBoard = boardRepository.save(board);
        return convertToResponse(savedBoard);
    }

    // 게시글 상세 조회
    public BoardResponse getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board);

        return convertToResponse(board);
    }

    // 모든 게시글 조회
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findByBoardStatus(BoardStatus.ACTIVE, pageable)
                .map(this::convertToResponse);
    }

    // 게시글 수정
    @Transactional
    public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("게시글 작성자만 수정할 수 있습니다.");
        }

        board.setTitle(request.getTitle());
        board.setContent(request.getContent());

        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard);
    }

    // 게시글 삭제
    @Transactional
    public void deleteBoard(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("게시글 작성자만 삭제할 수 있습니다.");
        }

        board.setBoardStatus(BoardStatus.DELETED);
        boardRepository.save(board);
    }

    // 게시글 검색
    public Page<BoardResponse> searchBoards(String keyword, Pageable pageable) {
        return boardRepository.findByTitleContainingOrContentContainingAndBoardStatus(
                        keyword, keyword, BoardStatus.ACTIVE, pageable)
                .map(this::convertToResponse);
    }

    // 인기글 조회 (좋아요 순)
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findPopularBoards(pageable)
                .map(this::convertToResponse);
    }

    // 최신글 조회 (생성일 순)
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findRecentBoards(pageable)
                .map(this::convertToResponse);
    }

    // 게시글 검증 (관리자용)
    @Transactional
    public BoardResponse verifyBoard(Long boardId, Boolean isVerified) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.setIsVerified(isVerified);
        Board verifiedBoard = boardRepository.save(board);
        return convertToResponse(verifiedBoard);
    }

    // Board 엔티티 -> BoardResponse DTO 변환
    private BoardResponse convertToResponse(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .memberId(board.getMember().getId())
                .memberNickname(board.getMember().getNickname()) // 닉네임 매핑
                .memberEmail(board.getMember().getEmail()) // 이메일 매핑
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .isVerified(board.getIsVerified())
                .boardStatus(board.getBoardStatus().name())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .imageUrls(board.getImages().stream()
                        .map(BoardImage::getImageUrl)
                        .collect(Collectors.toList()))
                .thumbnailUrl(board.getThumbnailUrl())
                .build();
    }
}