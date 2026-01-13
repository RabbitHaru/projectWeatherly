package me.shinsunyoung.projectweatherly.member.dto.request;



import lombok.Data;

@Data
public class UpdateNotificationRequest {

    private Boolean boardNotificationAgree;
    private Boolean weatherAlertAgree;
}