package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "member_id"})) // 중복 방지
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public CommentLike(Comment comment, Member member) {
        this.comment = comment;
        this.member = member;
    }
}