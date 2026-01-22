package me.shinsunyoung.projectweatherly.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordRequest {

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String confirmPassword;

    // 현재 비밀번호와 새 비밀번호가 같은지 확인하는 메서드
    public boolean isSamePassword() {
        return currentPassword != null &&
                newPassword != null &&
                currentPassword.equals(newPassword);
    }

    // 새 비밀번호와 확인 비밀번호가 같은지 확인하는 메서드
    public boolean isPasswordMatch() {
        return newPassword != null &&
                confirmPassword != null &&
                newPassword.equals(confirmPassword);
    }
}