package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.CommentLike;
<<<<<<< HEAD
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
=======
>>>>>>> member
import me.shinsunyoung.projectweatherly.board.dto.CommentRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentLikeRepository;
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
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService; // [사용] 같은 패키지라 import 불필요

    /**
     * 댓글 생성 (알림 전송 기능 추가됨)
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
                    .member(member) // Member 엔티티 연결
                    .likeCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            Comment savedComment = commentRepository.save(comment);

<<<<<<< HEAD
            log.info("댓글 생성 성공 - postId: {}, memberId: {}, commentId: {}",
                    postId, memberId, savedComment.getId());

            // Response 변환
            return CommentResponse.builder()
                    .id(savedComment.getId())
                    .content(savedComment.getContent())
                    .writer(member.getNickname()) // Member에서 닉네임 가져오기
                    .boardId(board.getId())
                    .createdAt(savedComment.getCreatedAt())
                    .updatedAt(savedComment.getCreatedAt())
                    .likeCount(0)
                    .isLiked(false) // 초기값은 false
                    .build();
        } catch (Exception e) {
            log.error("댓글 생성 실패 - postId: {}, memberId: {}", postId, memberId, e);
            throw new RuntimeException("댓글 작성 중 오류가 발생했습니다.", e);
        }
=======
        // [추가됨] 게시글 작성자에게 알림 전송
        try {
            notificationService.send(board.getMember(), member, board);
        } catch (Exception e) {
            log.error("알림 전송 실패: ", e); // 알림 실패해도 댓글은 등록되어야 함
        }

        return CommentResponse.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .writer(member.getNickname())
                .boardId(board.getId())
                .createdAt(savedComment.getCreatedAt())
                .likeCount(0)
                .isLiked(false)
                .build();
>>>>>>> member
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
     * 댓글 좋아요 토글
     */
    public boolean toggleLike(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (commentLikeRepository.existsByCommentAndMember(comment, member)) {
            commentLikeRepository.deleteByCommentAndMember(comment, member);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            return false;
        } else {
            commentLikeRepository.save(new CommentLike(comment, member));
            comment.setLikeCount(comment.getLikeCount() + 1);
            return true;
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