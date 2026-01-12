package me.shinsunyoung.projectweatherly.member.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordDTO {


    @NotBlank(message = "현재 비밀번호는 필수 입력 항목입니다.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다.")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }


    @Data
    public class RefreshTokenRequest {

        @NotBlank(message = "Refresh Token은 필수 입력 항목입니다.")
        private String refreshToken;
    }

    @Data
    public class WithdrawRequest {

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        private String password;

        private String reason;
    }
}