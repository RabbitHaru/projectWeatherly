package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.CommentLike;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    // 유저가 해당 댓글에 좋아요를 눌렀는지 확인
    boolean existsByCommentAndMember(Comment comment, Member member);

    // 좋아요 취소 (삭제)
    void deleteByCommentAndMember(Comment comment, Member member);
}