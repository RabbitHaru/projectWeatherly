package me.shinsunyoung.projectweatherly.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.shinsunyoung.projectweatherly.member.dto.LoginRequest;
import me.shinsunyoung.projectweatherly.member.dto.LoginResponse;
import me.shinsunyoung.projectweatherly.member.dto.SignUpRequest;
import me.shinsunyoung.projectweatherly.member.dto.SignUpResponse;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.ChangePasswordWithCodeDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.PasswordResetRequestDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.VerifyCodeRequestDto;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(
            @Valid @RequestBody SignUpRequest signUpRequest) {

        log.info("회원가입 요청: {}", signUpRequest.getUserEmail());

        SignUpResponse response = authService.signUp(signUpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "회원가입이 완료되었습니다.",
                        response
                ));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        log.info("로그인 요청: {}", loginRequest.getUserEmail());

        LoginResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "로그인되었습니다.",
                response
        ));
    }

    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Void>> checkEmailDuplicate(
            @RequestParam String email) {

        log.info("이메일 중복 확인: {}", email);

        boolean isAvailable = authService.isEmailAvailable(email);

        if (isAvailable) {
            return ResponseEntity.ok(ApiResponse.success(
                    "사용 가능한 이메일입니다.",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "이미 사용 중인 이메일입니다."
            ));
        }
    }

    /**
     * 닉네임 중복 확인
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Void>> checkNicknameDuplicate(
            @RequestParam String nickname) {

        log.info("닉네임 중복 확인: {}", nickname);

        boolean isAvailable = authService.isNicknameAvailable(nickname);

        if (isAvailable) {
            return ResponseEntity.ok(ApiResponse.success(
                    "사용 가능한 닉네임입니다.",
                    null
            ));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "이미 사용 중인 닉네임입니다."
            ));
        }
    }

    /**
     * 비밀번호 재설정 요청 (인증번호 발송)
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {

        log.info("비밀번호 재설정 요청: {}", request.getUserEmail());

        authService.requestPasswordReset(request);

        return ResponseEntity.ok(ApiResponse.success(
                "인증번호가 이메일로 발송되었습니다.",
                null
        ));
    }

    /**
     * 인증번호 확인
     */
    @PostMapping("/password/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(
            @Valid @RequestBody VerifyCodeRequestDto request) {

        log.info("인증번호 확인 요청: {}", request.getUserEmail());

        boolean isValid = authService.verifyResetCode(request);

        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success(
                    "인증번호가 확인되었습니다.",
                    true
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
    public ResponseEntity<ApiResponse<Void>> changePasswordWithCode(
            @Valid @RequestBody ChangePasswordWithCodeDto request) {

        log.info("인증번호로 비밀번호 변경 요청: {}", request.getUserEmail());

        // 비밀번호 확인 일치 검사
        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
            ));
        }

        authService.changePasswordWithVerificationCode(request);

        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 성공적으로 변경되었습니다.",
                null
        ));
    }

    /**
     * 로그인 상태에서 비밀번호 변경
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordDto request,
            @RequestHeader("Authorization") String token) {

        log.info("비밀번호 변경 요청");

        // 토큰에서 사용자 ID 추출 (JWT 인증 구현 필요)
        String jwtToken = token.replace("Bearer ", "");

        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
            ));
        }

        authService.changePassword(jwtToken, request);

        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 성공적으로 변경되었습니다.",
                null
        ));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String token) {

        log.info("로그아웃 요청");

        String jwtToken = token.replace("Bearer ", "");
        authService.logout(jwtToken);

        return ResponseEntity.ok(ApiResponse.success(
                "로그아웃되었습니다.",
                null
        ));
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("토큰 갱신 요청");

        LoginResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.success(
                "토큰이 갱신되었습니다.",
                response
        ));
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiRespons<Void>> withdraw(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody WithdrawRequest request) {

        log.info("회원 탈퇴 요청");

        String jwtToken = token.replace("Bearer ", "");
        authService.withdraw(jwtToken, request);

        return ResponseEntity.ok(ApiResponse.success(
                "회원 탈퇴가 완료되었습니다.",
                null
        ));
    }
}