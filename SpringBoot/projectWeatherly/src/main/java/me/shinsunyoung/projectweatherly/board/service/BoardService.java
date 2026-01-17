package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardImage;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.entity.Comment;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final CommentRepository commentRepository;

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
                        .isThumbnail(board.getImages().isEmpty())
                        .board(board)
                        .build();
                board.addImage(boardImage);
            }
        }

        Board savedBoard = boardRepository.save(board);
        return convertToResponse(savedBoard, false);
    }

    // 게시글 상세 조회
    public BoardResponse getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board);

        return convertToResponse(board, true);
    }

    // 모든 게시글 조회
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findByBoardStatus(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    // ✅ 회원별 게시물 조회 (페이지네이션)
    public Page<BoardResponse> getBoardsByMemberId(Long memberId, Pageable pageable) {
        return boardRepository.findByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    // ✅ 회원별 게시물 수 조회
    public long countByMemberId(Long memberId) {
        return boardRepository.countByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE);
    }

    // ✅ 모든 게시물 중 회원별 게시물 조회 (간단한 버전)
    public List<BoardResponse> getMyBoardsSimple(Long memberId) {
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE, pageable);

        return boards.getContent().stream()
                .map(board -> convertToResponse(board, false))
                .collect(Collectors.toList());
    }

    // ✅ 멤버 ID와 게시물 ID로 게시물 존재 여부 확인
    public boolean existsByMemberIdAndBoardId(Long memberId, Long boardId) {
        return boardRepository.existsByMemberIdAndIdAndBoardStatus(memberId, boardId, BoardStatus.ACTIVE);
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
        return convertToResponse(updatedBoard, true);
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
                        keyword, BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    // 인기글 조회 (좋아요 순)
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findPopularBoards(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    // 최신글 조회 (생성일 순)
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findRecentBoards(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    // 게시글 검증 (관리자용)
    @Transactional
    public BoardResponse verifyBoard(Long boardId, Boolean isVerified) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.setIsVerified(isVerified);
        Board verifiedBoard = boardRepository.save(board);
        return convertToResponse(verifiedBoard, false);
    }

    // 내 게시물만 가져오기 (마이페이지용 최적화)
    public List<Board> getMyBoardsForMyPage(Long memberId) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return boardRepository.findByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE, pageable).getContent();
    }

    // ✅ 카테고리별 게시글 조회
    public Page<BoardResponse> getBoardsByCategory(String category, Pageable pageable) {
        return boardRepository.findByCategoryAndBoardStatus(category, BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    // ✅ 게시글 상태 변경 (관리자용)
    @Transactional
    public BoardResponse changeBoardStatus(Long boardId, BoardStatus status) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.setBoardStatus(status);
        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard, false);
    }

    // ✅ 좋아요 증가
    @Transactional
    public BoardResponse increaseLike(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.increaseLikeCount();
        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard, false);
    }

    // ✅ 좋아요 감소
    @Transactional
    public BoardResponse decreaseLike(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        board.decreaseLikeCount();
        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard, false);
    }

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
            List<Comment> commentList = commentRepository.findByBoardId(board.getId());

            // Comment 엔티티를 CommentResponse로 변환
            comments = commentList.stream()
                    .map(comment -> CommentResponse.builder()
                            .id(comment.getId())
                            .content(comment.getContent())
                            .writer(comment.getWriter())
                            .boardId(board.getId())
                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
            commentCount = comments.size();
        } else {
            // 목록 조회인 경우 댓글 수만 가져오기
            commentCount = commentRepository.countByBoardId(board.getId());
        }

        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .commentCount(commentCount)
                .comments(comments)
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

    // ✅ 멤버 ID로 게시물 ID 목록 조회 (신고 기능용)
    public List<Long> getBoardIdsByMemberId(Long memberId) {
        return boardRepository.findBoardIdsByMemberIdAndStatus(memberId, BoardStatus.ACTIVE);
    }

    // ✅ 게시물 제목으로 검색 (마이페이지에서 내 게시물 검색용)
    public List<BoardResponse> searchMyBoardsByTitle(Long memberId, String keyword) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByMemberIdAndTitleContainingAndBoardStatus(
                memberId, keyword, BoardStatus.ACTIVE, pageable);

        return boards.getContent().stream()
                .map(board -> convertToResponse(board, false))
                .collect(Collectors.toList());
    }

    // ✅ 게시글 ID로 게시글 엔티티 조회 (내부용)
    public Board getBoardEntity(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    // ✅ 게시글 목록을 ID 리스트로 조회
    public List<BoardResponse> getBoardsByIds(List<Long> boardIds) {
        List<Board> boards = boardRepository.findAllById(boardIds);
        return boards.stream()
                .map(board -> convertToResponse(board, false))
                .collect(Collectors.toList());
    }
}