package me.shinsunyoung.projectweatherly.member.dto.request;


import jakarta.mail.Multipart;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Data
public class SignupRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.")
    private String email;

   @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 이내로 입력해주세요.")
    private String password;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 2, max = 50, message = "닉네임은 2~50자 이내로 입력해주세요.")
    private String nickname;

    private List<MultipartFile> profileImage;

    // 약관 동의 (캡회2처)
    @NotNull(message = "이용약관 동의는 필수입니다.")
    private Boolean termsOfServiceAgree;

    @NotNull(message = "개인정보 처리 동의는 필수입니다.")
    private Boolean privacyPolicyAgree;

    private Boolean marketingAgree = false;

    // 알림 설정 (캡회3처)
    private Boolean boardNotificationAgree = false;
    private Boolean weatherAlertAgree = false;
}