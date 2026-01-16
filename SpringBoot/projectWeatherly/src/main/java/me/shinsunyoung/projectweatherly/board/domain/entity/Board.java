package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BOARD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    // ✅ 카테고리 필드 추가
    @Column(name = "category", length = 50)
    private String category;

    // 단일 image_url 대신 다중 이미지 관계
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BoardImage> images = new ArrayList<>();

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_status", length = 20)
    @Builder.Default
    private BoardStatus boardStatus = BoardStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 이미지 추가 메서드
    public void addImage(BoardImage image) {
        images.add(image);
        image.setBoard(this);
    }

    // 이미지 제거 메서드
    public void removeImage(BoardImage image) {
        images.remove(image);
        image.setBoard(null);
    }

    // 모든 이미지 제거 메서드
    public void clearImages() {
        images.clear();
    }

    // 썸네일 URL 가져오기 메서드
    public String getThumbnailUrl() {
        return images.stream()
                .filter(BoardImage::getIsThumbnail)
                .findFirst()
                .map(BoardImage::getImageUrl)
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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
}