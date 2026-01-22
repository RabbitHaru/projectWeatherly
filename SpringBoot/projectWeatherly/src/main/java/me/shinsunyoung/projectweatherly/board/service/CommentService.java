package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.CommentLike;
import me.shinsunyoung.projectweatherly.board.domain.entity.Notification;
import me.shinsunyoung.projectweatherly.board.dto.CommentRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentLikeRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.board.repository.NotificationRepository;
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
    // NotificationService 대신 NotificationRepository를 직접 사용 (제목 커스텀을 위해)
    private final NotificationRepository notificationRepository;

    /**
     * 댓글 생성
     */
    public CommentResponse createComment(Long postId, Long memberId, CommentRequest request) {
        // 1. 게시글 조회 (변수 선언)
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 2. 회원 조회 (변수 선언)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 3. 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .content(request.getContent())
                .board(board)
                .member(member)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        // 4. 댓글 저장 (변수 선언)
        Comment savedComment = commentRepository.save(comment);

        // 5. 알림 전송 (게시글 제목 포함)
        try {
            // 본인이 쓴 글에 본인이 댓글 달 때는 알림 스킵
            if (!board.getMember().getId().equals(member.getId())) {

                // ★ 알림 메시지에 게시글 제목 포함
                String message = "'" + board.getTitle() + "' 게시글에 새 댓글이 달렸습니다.";

                notificationRepository.save(Notification.builder()
                        .receiver(board.getMember()) // 글 작성자에게
                        .sender(member)              // 댓글 작성자가
                        .board(board)
                        .message(message)            // 커스텀 메시지
                        .build());
            }
        } catch (Exception e) {
            log.error("알림 전송 실패: ", e);
        }

        // 6. 응답 반환
        return CommentResponse.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .writer(member.getNickname())
                .boardId(board.getId())
                .createdAt(savedComment.getCreatedAt())
                .likeCount(0)
                .isLiked(false)
                .build();
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        if (!comment.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }

        commentRepository.delete(comment);
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