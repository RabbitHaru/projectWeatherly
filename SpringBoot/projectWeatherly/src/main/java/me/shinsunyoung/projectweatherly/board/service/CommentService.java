package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.CommentLike; // 추가됨
import me.shinsunyoung.projectweatherly.board.dto.CommentRequest;
import me.shinsunyoung.projectweatherly.board.dto.CommentResponse;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentLikeRepository; // 추가됨
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
    private final CommentLikeRepository commentLikeRepository; // 의존성 추가

    /**
     * 댓글 생성
     */
    public CommentResponse createComment(Long postId, Long memberId, CommentRequest request) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .board(board)
                .member(member)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .writer(member.getNickname())
                .boardId(board.getId())
                .createdAt(savedComment.getCreatedAt())
                .likeCount(0)
                .isLiked(false) // 초기값은 false
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
     * [수정됨] 댓글 좋아요 토글 (Toggle)
     * return: true(좋아요 추가됨), false(좋아요 취소됨)
     */
    public boolean toggleLike(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        // 1. 이미 좋아요를 눌렀는지 확인
        if (commentLikeRepository.existsByCommentAndMember(comment, member)) {
            // 이미 있음 -> 삭제 (좋아요 취소)
            commentLikeRepository.deleteByCommentAndMember(comment, member);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1)); // 숫자 감소
            return false; // 좋아요 취소됨
        } else {
            // 없음 -> 추가 (좋아요)
            commentLikeRepository.save(new CommentLike(comment, member));
            comment.setLikeCount(comment.getLikeCount() + 1); // 숫자 증가
            return true; // 좋아요 추가됨
        }
        // Dirty Checking으로 comment.likeCount는 자동 저장됨
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