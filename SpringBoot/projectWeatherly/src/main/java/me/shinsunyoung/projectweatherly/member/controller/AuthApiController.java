package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final MemberService memberService;

    // 이메일 중복 체크 페이지
    @GetMapping("/check-email-page")
    @Operation(summary = "이메일 중복 체크 페이지", description = "이메일 중복 체크 폼을 표시합니다.")
    public String showEmailCheckPage(Model model) {
        return "auth/check-email";
    }

    // 이메일 중복 체크 처리
    @PostMapping("/check-email")
    @Operation(summary = "이메일 중복 체크 처리", description = "이메일이 이미 사용 중인지 확인합니다.")
    public String checkEmail(
            @RequestParam String email,
            Model model) {

        boolean exists = memberService.checkEmailExists(email);
        model.addAttribute("email", email);
        model.addAttribute("exists", exists);
        model.addAttribute("available", !exists);

        return "auth/check-email-result";
    }

    // 닉네임 중복 체크 페이지
    @GetMapping("/check-nickname-page")
    @Operation(summary = "닉네임 중복 체크 페이지", description = "닉네임 중복 체크 폼을 표시합니다.")
    public String showNicknameCheckPage(Model model) {
        return "auth/check-nickname";
    }

    // 닉네임 중복 체크 처리
    @PostMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 체크 처리", description = "닉네임이 이미 사용 중인지 확인합니다.")
    public String checkNickname(
            @RequestParam String nickname,
            Model model) {

        boolean exists = memberService.checkNicknameExists(nickname);
        model.addAttribute("nickname", nickname);
        model.addAttribute("exists", exists);
        model.addAttribute("available", !exists);

        return "auth/check-nickname-result";
    }
}