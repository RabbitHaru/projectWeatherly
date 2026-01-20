package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BOARD_IMAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "image_name")
    private String imageName;

    @Column(name = "image_size")
    private Long imageSize;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_thumbnail")
    private Boolean isThumbnail = false;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}