package me.shinsunyoung.projectweatherly.member.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationRequest {
    private Boolean boardNotificationAgree;   // 게시물 알림 동의
    private Boolean weatherAlertAgree;        // 기상특보 알림 동의
}