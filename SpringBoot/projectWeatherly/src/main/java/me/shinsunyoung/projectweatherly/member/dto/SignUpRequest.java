package me.shinsunyoung.projectweatherly.member.dto;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class SignUpRequest {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String userEmail;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 사이로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
    private String userName;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
    private String userPassword;

    private String profileImage; // Base64 또는 URL

    @AssertTrue(message = "이용약관에 동의해야 합니다.")
    private boolean agreeTerms;

    @AssertTrue(message = "개인정보 처리 방침에 동의해야 합니다.")
    private boolean agreePrivacy;

    private boolean agreeMarketing; // 선택 동의
}