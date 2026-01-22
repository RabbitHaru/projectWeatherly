package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardImage;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardLike;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardLikeRepository;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final ImageUploadService imageUploadService;
    private final CommentRepository commentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findByBoardStatus(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findPopularBoards(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findRecentBoards(BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> searchBoards(String keyword, Pageable pageable) {
        return boardRepository.findByTitleContainingOrContentContainingAndBoardStatus(
                        keyword, BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional
    public BoardResponse getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // [수정] 여기서 조회수 증가(board.increaseViewCount())를 하지 않습니다.
        // 조회수 증가는 Controller에서 조건 확인 후 increaseViewCount()를 명시적으로 호출합니다.

        return convertToResponse(board, true);
    }

    @Override
    public BoardResponse createBoard(Long memberId, BoardRequest request) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (Board.CATEGORY_NOTICE.equalsIgnoreCase(request.getCategory())) {
            if (member.getRole() != MemberRole.ADMIN) {
                throw new IllegalStateException("공지사항은 관리자만 작성할 수 있습니다.");
            }
        }

        Board board = Board.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .viewCount(0)
                .likeCount(0)
                .build();

        Board savedBoard = boardRepository.save(board);

        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            for (MultipartFile imageFile : request.getImageFiles()) {
                if (!imageFile.isEmpty()) {
                    String imageUrl = imageUploadService.uploadImage(imageFile);
                    BoardImage boardImage = BoardImage.builder()
                            .imageUrl(imageUrl)
                            .isThumbnail(savedBoard.getImages().isEmpty())
                            .board(savedBoard)
                            .build();
                    savedBoard.addImage(boardImage);
                }
            }
            savedBoard = boardRepository.save(savedBoard);
        }

        return convertToResponse(savedBoard, false);
    }

    @Override
    public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new IllegalArgumentException("이미 삭제된 게시글은 수정할 수 없습니다.");
        }

        Member requestMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        boolean isOwner = board.getMember().getId().equals(memberId);
        boolean isAdmin = requestMember.getRole() == MemberRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("게시글 수정 권한이 없습니다.");
        }

        if (Board.CATEGORY_NOTICE.equalsIgnoreCase(request.getCategory())) {
            if (!isAdmin) {
                throw new IllegalStateException("관리자만 공지사항으로 설정할 수 있습니다.");
            }
        }

        board.update(request.getTitle(), request.getContent(), request.getCategory());

        if (request.getDeleteImages() != null && !request.getDeleteImages().isEmpty()) {
            for (String fileName : request.getDeleteImages()) {
                board.getImages().removeIf(img -> img.getImageUrl().equals(fileName));
            }
        }

        if (request.getNewImages() != null && !request.getNewImages().isEmpty()) {
            for (MultipartFile imageFile : request.getNewImages()) {
                if (!imageFile.isEmpty()) {
                    try {
                        String imageUrl = imageUploadService.uploadImage(imageFile);
                        BoardImage boardImage = BoardImage.builder()
                                .imageUrl(imageUrl)
                                .board(board)
                                .isThumbnail(board.getImages().isEmpty())
                                .build();
                        board.addImage(boardImage);
                    } catch (IOException e) {
                        log.error("이미지 수정 중 업로드 실패", e);
                        throw new RuntimeException("이미지 업로드 실패");
                    }
                }
            }
        }

        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard, false);
    }

    @Override
    public void deleteBoard(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new IllegalArgumentException("이미 삭제된 게시글입니다.");
        }

        Member requestMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        boolean isOwner = board.getMember().getId().equals(memberId);
        boolean isAdmin = requestMember.getRole() == MemberRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("게시글 삭제 권한이 없습니다.");
        }

        board.setBoardStatus(BoardStatus.DELETED);
        boardRepository.save(board);
    }

    @Override
    public boolean toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            return false;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Optional<BoardLike> existingLike = boardLikeRepository.findByBoardAndMember(board, member);

        if (existingLike.isPresent()) {
            boardLikeRepository.delete(existingLike.get());
            board.decreaseLikeCount();
            boardRepository.save(board);
            return false;
        } else {
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
    @Transactional(readOnly = true)
    public boolean isLiked(Long boardId, Long memberId) {
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Member> member = memberRepository.findById(memberId);

        if (board.isEmpty() || member.isEmpty()) {
            return false;
        }
        return boardLikeRepository.findByBoardAndMember(board.get(), member.get()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public int getViewCount(Long boardId) {
        return boardRepository.findById(boardId).map(Board::getViewCount).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> getMyBoards(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return boardRepository.findByMember(member, pageable).map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> getBoardsByCategory(String category, Pageable pageable) {
        return boardRepository.findByCategoryAndBoardStatus(category, BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoardResponse> getBoardsByMemberId(Long memberId, Pageable pageable) {
        return boardRepository.findByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE, pageable)
                .map(board -> convertToResponse(board, false));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByMemberId(Long memberId) {
        return boardRepository.countByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardResponse> getMyBoardsSimple(Long memberId) {
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE, pageable);
        return boards.getContent().stream()
                .map(board -> convertToResponse(board, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMemberIdAndBoardId(Long memberId, Long boardId) {
        return boardRepository.existsByMemberIdAndIdAndBoardStatus(memberId, boardId, BoardStatus.ACTIVE);
    }

    @Override
    public BoardResponse verifyBoard(Long boardId, Boolean isVerified) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        board.setIsVerified(isVerified);
        return convertToResponse(boardRepository.save(board), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> getMyBoardsForMyPage(Long memberId) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return boardRepository.findByMemberIdAndBoardStatus(memberId, BoardStatus.ACTIVE, pageable).getContent();
    }

    @Override
    public BoardResponse changeBoardStatus(Long boardId, BoardStatus status) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        board.setBoardStatus(status);
        return convertToResponse(boardRepository.save(board), false);
    }

    @Override
    public BoardResponse increaseLike(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow();
        board.increaseLikeCount();
        return convertToResponse(boardRepository.save(board), false);
    }

    @Override
    public BoardResponse decreaseLike(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow();
        board.decreaseLikeCount();
        return convertToResponse(boardRepository.save(board), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBoardIdsByMemberId(Long memberId) {
        return boardRepository.findBoardIdsByMemberIdAndStatus(memberId, BoardStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardResponse> searchMyBoardsByTitle(Long memberId, String keyword) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByMemberIdAndTitleContainingAndBoardStatus(
                memberId, keyword, BoardStatus.ACTIVE, pageable);
        return boards.getContent().stream().map(b -> convertToResponse(b, false)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Board getBoardEntity(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardResponse> getBoardsByIds(List<Long> boardIds) {
        return boardRepository.findAllById(boardIds).stream().map(b -> convertToResponse(b, false)).collect(Collectors.toList());
    }

    private BoardResponse convertToResponse(Board board, boolean includeComments) {
        var imageUrls = board.getImages().stream()
                .map(BoardImage::getImageUrl)
                .collect(Collectors.toList());

        List<CommentResponse> comments = null;
        Integer commentCount = 0;

        if (includeComments) {
            List<Comment> commentList = commentRepository.findByBoardId(board.getId());
            comments = commentList.stream()
                    .map(comment -> CommentResponse.builder()
                            .id(comment.getId())
                            .content(comment.getContent())
                            .writer(comment.getWriter())
                            .memberNickname(comment.getWriter())
                            .memberProfileImage(comment.getMember().getProfileImage())
                            .boardId(board.getId())
                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .likeCount(comment.getLikeCount())
                            .build())
                    .collect(Collectors.toList());
            commentCount = comments.size();
        } else {
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
}