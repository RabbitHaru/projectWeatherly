package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Notification;
import me.shinsunyoung.projectweatherly.board.repository.NotificationRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 댓글 알림 전송
    public void send(Member receiver, Member sender, Board board) {
        // 내 글에 내가 댓글 단 경우는 알림 안 보냄
        if (receiver.getId().equals(sender.getId())) {
            return;
        }

        String message = sender.getNickname() + "님이 게시글에 댓글을 남겼습니다.";

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .board(board)
                .message(message)
                .build();

        notificationRepository.save(notification);
    }

    // 읽지 않은 알림 개수 조회
    @Transactional(readOnly = true)
    public long countUnread(Long memberId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(memberId);
    }
}