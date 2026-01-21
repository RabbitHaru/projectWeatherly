package me.shinsunyoung.projectweatherly.board.domain.entity;

import lombok.*;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // [수정 1] cascade = CascadeType.ALL 제거! (댓글 지운다고 게시글 지우면 안 됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer likeCount = 0;

    // [수정 2] 좋아요(CommentLike) 관계 추가 & 자동 삭제 설정
    // 댓글이 삭제되면(remove), 연결된 좋아요(likes)도 같이 삭제(ALL, orphanRemoval) 됩니다.
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // 빌더 패턴 사용 시 리스트 초기화 유지
    private List<CommentLike> likes = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    // 편의 메서드
    public void setMember(Member member) {
        this.member = member;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getLikeCount() {
        return likeCount != null ? likeCount : 0;
    }

    @Transient
    public String getWriter() {
        return member != null ? member.getNickname() : "익명";
    }
}