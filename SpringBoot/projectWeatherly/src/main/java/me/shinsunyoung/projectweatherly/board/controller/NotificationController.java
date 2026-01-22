package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Notification;
import me.shinsunyoung.projectweatherly.board.repository.NotificationRepository;
import me.shinsunyoung.projectweatherly.board.service.NotificationService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    // [기존 유지] 읽지 않은 알림 개수 반환 API
    @GetMapping("/api/notifications/count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserSecurityDTO user) {
        if (user == null) {
            return ResponseEntity.ok(0L);
        }
        long count = notificationService.countUnread(user.getUser().getId());
        return ResponseEntity.ok(count);
    }

    // [수정됨] 알림 목록 조회 API (타입 추론 오류 해결)
    @GetMapping("/api/notifications/unread")
    public List<Map<String, Object>> getUnreadNotifications(@AuthenticationPrincipal UserSecurityDTO user) {
        if (user == null) return List.of();

        List<Notification> notifications = notificationRepository
                .findByReceiverAndIsReadFalseOrderByCreatedDateDesc(user.getUser());

        return notifications.stream().map(n -> {
            // ★ 수정 포인트: Map.<String, Object>of(...) 로 타입을 명확히 지정
            return Map.<String, Object>of(
                    "id", n.getId(),
                    "message", n.getMessage(),
                    "boardId", n.getBoard().getId(),
                    "createdAt", n.getCreatedDate()
            );
        }).collect(Collectors.toList());
    }

    // [추가됨] 알림 읽음 처리 API
    @PostMapping("/api/notifications/{id}/read")
    @Transactional
    public void readNotification(@PathVariable Long id) {
        notificationRepository.markAsRead(id);
    }
}