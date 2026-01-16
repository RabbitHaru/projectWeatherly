package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardImage;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ImageUploadService imageUploadService;
    private final CommentService commentService;  // CommentService 주입 추가

    // 게시글 생성
    @Transactional
    public BoardResponse createBoard(Long memberId, BoardRequest request) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Board board = Board.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
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
        return convertToResponse(savedBoard, false);  // 목록 조회에서는 댓글 미포함
    }

    // 게시글 상세 조회
    public BoardResponse getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board);

        return convertToResponse(board, true);  // 상세 조회에서는 댓글 포함
    }

    // 모든 게시글 조회
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findByBoardStatus(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));  // 목록 조회에서는 댓글 미포함
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
        board.setCategory(request.getCategory());

        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard, true);  // 수정 후 상세 조회는 댓글 포함
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
                .map(board -> convertToResponse(board, false));  // 목록 조회에서는 댓글 미포함
    }

    // 인기글 조회 (좋아요 순)
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findPopularBoards(pageable)
                .map(board -> convertToResponse(board, false));  // 목록 조회에서는 댓글 미포함
    }

    // 최신글 조회 (생성일 순)
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findRecentBoards(pageable)
                .map(board -> convertToResponse(board, false));  // 목록 조회에서는 댓글 미포함
    }

    // 게시글 검증 (관리자용)
    @Transactional
    public BoardResponse verifyBoard(Long boardId, Boolean isVerified) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.setIsVerified(isVerified);
        Board verifiedBoard = boardRepository.save(board);
        return convertToResponse(verifiedBoard, false);  // 목록 조회에서는 댓글 미포함
    }

    // ✅ 수정된 convertToResponse 메서드 (comments 필드 추가)
    private BoardResponse convertToResponse(Board board, boolean includeComments) {
        // 이미지 URL 목록 추출
        var imageUrls = board.getImages().stream()
                .map(BoardImage::getImageUrl)
                .collect(Collectors.toList());

        // 댓글 정보 설정
        List<CommentResponse> comments = null;
        Integer commentCount = 0;

        if (includeComments) {
            // 상세 조회인 경우 댓글 목록 가져오기
            comments = commentService.getCommentsByBoardId(board.getId());
            commentCount = comments.size();
        } else {
            // 목록 조회인 경우 댓글 수만 가져오기
            commentCount = commentService.getCommentCountByBoardId(board.getId());
        }

        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .commentCount(commentCount)
                .comments(comments)  // 댓글 목록 설정
                .memberId(board.getMember().getId())
                .memberNickname(board.getMember().getNickname())
                .memberEmail(board.getMember().getEmail())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .isVerified(board.getIsVerified())
                .boardStatus(board.getBoardStatus().name())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .imageUrls(imageUrls)
                .images(imageUrls)
                .thumbnailUrl(board.getThumbnailUrl())
                .build();
    }
}