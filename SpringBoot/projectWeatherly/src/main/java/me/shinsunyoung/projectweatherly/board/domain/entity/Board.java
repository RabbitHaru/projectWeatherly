package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 50)
    private String category;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BoardImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int likeCount;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_status", length = 20)
    @Builder.Default
    private BoardStatus boardStatus = BoardStatus.ACTIVE;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 조회수 증가 메서드
    public void increaseViewCount() {
        this.viewCount++;
    }

    // 좋아요 수 증가 메서드
    public void increaseLikeCount() {
        this.likeCount++;
    }

    // 좋아요 수 감소 메서드
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // 게시글 수정 메서드
    public void update(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    // 이미지 추가 메서드
    public void addImage(BoardImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
        image.setBoard(this);
    }
}