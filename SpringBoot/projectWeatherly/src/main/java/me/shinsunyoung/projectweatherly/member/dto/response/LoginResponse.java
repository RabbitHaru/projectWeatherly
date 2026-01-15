package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private Long memberId;
    private String email;
    private String nickname;
    private String profileImage;
    private String role;
    private String authProvider;


    public LoginResponse(Long memberId, String email, String nickname,
                         String profileImage, String role, String authProvider) {
        this.memberId = memberId;
        this.email = email;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.role = role;
        this.authProvider = authProvider;
    }
}