package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.service.NotificationService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 읽지 않은 알림 개수 반환 API
    @GetMapping("/api/notifications/count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserSecurityDTO user) {
        if (user == null) {
            return ResponseEntity.ok(0L);
        }

        long count = notificationService.countUnread(user.getUser().getId());
        return ResponseEntity.ok(count);
    }
}