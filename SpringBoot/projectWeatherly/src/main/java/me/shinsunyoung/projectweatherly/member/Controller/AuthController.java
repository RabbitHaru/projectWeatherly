package me.shinsunyoung.projectweatherly.member.Controller;




import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.shinsunyoung.projectweatherly.member.dto.request.auth.ChangePasswordWithCodeDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.PasswordResetRequestDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.VerifyCodeRequestDto;
import me.shinsunyoung.projectweatherly.member.service.auth.PasswordResetService;
import me.shinsunyoung.projectweatherly.member.dto.response.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PasswordResetService passwordResetService;

    /**
     * 비밀번호 재설정 요청 (인증번호 발송)
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {

        passwordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(ApiResponse.success(
                "인증번호가 이메일로 발송되었습니다.",
                null
        ));
    }

    /**
     * 인증번호 확인
     */
    @PostMapping("/password/verify-code")
    public ResponseEntity<ApiResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequestDto request) {

        boolean isValid = passwordResetService.verifyCode(request);

        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success(
                    "인증번호가 확인되었습니다.",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "인증번호가 유효하지 않습니다."
            ));
        }
    }

    /**
     * 인증번호로 비밀번호 변경
     */
    @PostMapping("/password/change-with-code")
    public ResponseEntity<ApiResponse> changePasswordWithCode(
            @Valid @RequestBody ChangePasswordWithCodeDto request) {

        // 비밀번호 확인 일치 검사
        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
            ));
        }

        passwordResetService.changePasswordWithVerificationCode(request);

        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 성공적으로 변경되었습니다.",
                null
        ));
    }
}