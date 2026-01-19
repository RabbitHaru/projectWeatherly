package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
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
                    .build();
        } catch (Exception e) {
            log.error("댓글 생성 실패 - postId: {}, memberId: {}", postId, memberId, e);
            throw new RuntimeException("댓글 작성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 삭제 (추가된 메서드)
     */
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        // 권한 체크: 요청한 사람(memberId)과 댓글 작성자(comment.getMember().getId())가 같은지 확인
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

            // 현재는 단순 증가 로직 (추후 LikeRepository 사용 시 토글 구현 가능)
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