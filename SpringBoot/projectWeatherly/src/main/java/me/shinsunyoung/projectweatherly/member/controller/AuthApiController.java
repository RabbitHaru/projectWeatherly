package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final MemberService memberService;
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
}
