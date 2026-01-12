package me.shinsunyoung.projectweatherly.member.RequestDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberJoinRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.")
    private String userEmail;

    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    private String userPassword; // 소셜 로그인 사용자는 null 가능

    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.")
    private String userName;

    @Size(max = 255, message = "프로필 이미지 URL은 255자 이내로 입력해주세요.")
    private String profileImage;

    private String userRole = "USER"; // 기본값 USER
    private String authProvider = "local"; // 기본값 local
    private String providerId; // 소셜 로그인 ID
    private Boolean isActive = true; // 기본값 true
}