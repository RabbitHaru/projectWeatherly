package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private Member receiver; // 알림 받는 사람 (게시글 작성자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender; // 알림 보낸 사람 (댓글 작성자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board; // 관련 게시글

    private String message; // 알림 내용

    private boolean isRead; // 읽음 여부

    @CreatedDate
    private LocalDateTime createdDate;

    @Builder
    public Notification(Member receiver, Member sender, Board board, String message) {
        this.receiver = receiver;
        this.sender = sender;
        this.board = board;
        this.message = message;
        this.isRead = false;
    }

    public void read() {
        this.isRead = true;
    }
}