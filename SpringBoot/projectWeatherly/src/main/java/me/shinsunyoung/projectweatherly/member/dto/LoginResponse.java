package me.shinsunyoung.projectweatherly.member.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String userEmail;
    private String userName;
    private String profileImage;
    private String userRole;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}