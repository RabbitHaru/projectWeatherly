package me.shinsunyoung.projectweatherly.member.controller;



import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
@Api(tags = "인증 API")
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @ApiOperation(value = "회원가입", notes = "새로운 회원을 등록합니다.")
    public ResponseEntity<ApiResponse2<Long>> signup(
            @Valid @RequestBody SignupRequest request) {

        Long memberId = memberService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse2.success("회원가입이 완료되었습니다.", memberId));
    }

    @PostMapping("/login")
    @ApiOperation(value = "로그인", notes = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<ApiResponse2<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = memberService.login(request);

        return ResponseEntity.ok(ApiResponse2.success(response));
    }

    @PostMapping("/logout")
    @ApiOperation(value = "로그아웃", notes = "로그아웃합니다. (클라이언트에서 토큰 삭제)")
    public ResponseEntity<ApiResponse2<Void>> logout() {
        // 클라이언트 측에서 토큰 삭제하도록 안내
        // JWT는 서버에 상태를 저장하지 않으므로 서버에서는 특별한 처리 불필요
        return ResponseEntity.ok(ApiResponse2.success("로그아웃되었습니다.", null));
    }

    @GetMapping("/check-email")
    @ApiOperation(value = "이메일 중복 체크", notes = "이메일이 이미 사용 중인지 확인합니다.")
    public ResponseEntity<ApiResponse2<Boolean>> checkEmail(
            @RequestParam String email) {

        // 이메일 중복 체크 로직 구현 (간단 예시)
        boolean exists = false; // 실제로는 memberRepository.existsByEmail(email)

        return ResponseEntity.ok(ApiResponse2.success("이메일 중복 체크 완료", exists));
    }
}
