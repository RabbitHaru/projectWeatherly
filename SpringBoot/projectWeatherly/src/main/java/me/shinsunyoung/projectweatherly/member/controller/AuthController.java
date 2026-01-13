package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.dto.request.LoginRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.SignupRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import me.shinsunyoung.projectweatherly.member.dto.response.LoginResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인, 로그아웃 관련 API")
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    public ResponseEntity<ApiResponse2<Long>> signup(
            @Valid @RequestBody SignupRequest request) {

        Long memberId = memberService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse2.success("회원가입이 완료되었습니다.", memberId));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. 로그인 성공 시 세션을 생성합니다.")
    public ResponseEntity<ApiResponse2<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        // Spring Security가 로그인을 처리하므로, 여기서는 추가적인 로그인 응답 정보만 반환
        LoginResponse response = memberService.login(request);

        return ResponseEntity.ok(ApiResponse2.success("로그인되었습니다.", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃",
            description = "로그아웃합니다. 세션을 무효화하고 쿠키를 삭제합니다.")
    public ResponseEntity<ApiResponse2<Void>> logout() {
        // Spring Security의 logout()이 처리하므로, 여기서는 성공 메시지만 반환
        return ResponseEntity.ok(ApiResponse2.success("로그아웃되었습니다."));
    }

    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 체크",
            description = "이메일이 이미 사용 중인지 확인합니다.")
    public ResponseEntity<ApiResponse2<Boolean>> checkEmail(
            @RequestParam String email) {

        boolean exists = memberService.checkEmailExists(email);

        return ResponseEntity.ok(ApiResponse2.success("이메일 중복 체크 완료", exists));
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 체크",
            description = "닉네임이 이미 사용 중인지 확인합니다.")
    public ResponseEntity<ApiResponse2<Boolean>> checkNickname(
            @RequestParam String nickname) {

        boolean exists = memberService.checkNicknameExists(nickname);

        return ResponseEntity.ok(ApiResponse2.success("닉네임 중복 체크 완료", exists));
    }

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 사용자 정보 조회",
            description = "세션에 저장된 현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse2<LoginResponse>> getCurrentUser() {
        // Spring Security의 @AuthenticationPrincipal을 통해 현재 사용자 정보를 얻을 수 있으나,
        // AuthController에서는 간단한 정보만 반환하도록 구성
        // 상세 정보는 MemberController의 /me 엔드포인트를 사용
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse2.error("이 엔드포인트는 사용할 수 없습니다. /api/members/me를 사용해주세요."));
    }
}