package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.dto.CommentRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    /**
     * 댓글 생성
     */
    public CommentResponse createComment(Long postId, Long memberId, CommentRequest request) {
        try {
            Board board = boardRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

            // ★ [추가] 삭제된 게시글에는 댓글 작성 불가
            if (board.getBoardStatus() == BoardStatus.DELETED) {
                throw new IllegalArgumentException("삭제된 게시글에는 댓글을 작성할 수 없습니다.");
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

            // Comment 생성
            Comment comment = Comment.builder()
                    .content(request.getContent())
                    .board(board)
                    .member(member)
                    .likeCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            Comment savedComment = commentRepository.save(comment);

            log.info("댓글 생성 성공 - postId: {}, memberId: {}, commentId: {}",
                    postId, memberId, savedComment.getId());

            return CommentResponse.builder()
                    .id(savedComment.getId())
                    .content(savedComment.getContent())
                    .writer(member.getNickname())
                    .boardId(board.getId())
                    .createdAt(savedComment.getCreatedAt())
                    .updatedAt(savedComment.getCreatedAt())
                    .build();
        } catch (Exception e) {
            log.error("댓글 생성 실패 - postId: {}, memberId: {}", postId, memberId, e);
            // 여기서 발생한 예외는 컨트롤러로 전파되어 푸터의 팝업으로 표시됩니다.
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        // ★ [추가] 원글이 삭제되었으면 댓글 삭제도 차단 (혹은 정책에 따라 허용 가능)
        if (comment.getBoard().getBoardStatus() == BoardStatus.DELETED) {
            throw new IllegalArgumentException("삭제된 게시글의 댓글은 수정/삭제할 수 없습니다.");
        }

        if (!comment.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }

        commentRepository.delete(comment);
        log.info("댓글 삭제 성공 - commentId: {}, memberId: {}", commentId, memberId);
    }

    /**
     * 댓글 좋아요 (단순 증가)
     */
    public boolean toggleLike(Long commentId, Long memberId) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);

            return true;
        } catch (Exception e) {
            log.error("댓글 좋아요 처리 실패 - commentId: {}, memberId: {}", commentId, memberId, e);
            throw new RuntimeException("댓글 좋아요 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 좋아요 개수 조회
     */
    @Transactional(readOnly = true)
    public int getLikeCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        return comment.getLikeCount();
    }
}