package me.shinsunyoung.projectweatherly.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationRequest {
    private Boolean boardNotificationAgree;   // 게시물 알림 동의
    private Boolean weatherAlertAgree;        // 기상특보 알림 동의
}