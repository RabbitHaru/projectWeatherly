package me.shinsunyoung.projectweatherly.member.dto.response;



import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long memberId;
    private String email;
    private String nickname;
    private String role;
    private Long expiresIn;
}