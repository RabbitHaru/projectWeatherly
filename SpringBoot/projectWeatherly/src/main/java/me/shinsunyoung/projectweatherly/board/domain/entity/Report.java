package me.shinsunyoung.projectweatherly.board.domain.entity;

import lombok.*;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @Column(name = "type", nullable = false)
    private String type; // "post" or "comment"

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String status; // "PENDING", "PROCESSING", "COMPLETED", "REJECTED", "CANCELLED"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 선택적 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board targetBoard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment targetComment;

    // 편의 메서드
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }
}