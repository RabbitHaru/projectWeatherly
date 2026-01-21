package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus; // ★ Enum 임포트
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.hibernate.annotations.CreationTimestamp;

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

    // 신고자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Member reporter;

    private String type; // "POST" or "COMMENT"

    @Column(name = "target_id")
    private Long targetId; // 게시글 ID 또는 댓글 ID

    // 신고된 게시글 (POST일 때 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board targetBoard;

    // 신고된 댓글 (COMMENT일 때 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment targetComment;

    private String reason; // 신고 사유 (spam, abuse 등)

    @Column(columnDefinition = "TEXT")
    private String details; // 상세 내용

    // ★ [핵심 수정] String -> ReportStatus (Enum) 변경
    // DB에는 "PENDING", "RESOLVED" 같은 문자열로 저장됨
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReportStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}