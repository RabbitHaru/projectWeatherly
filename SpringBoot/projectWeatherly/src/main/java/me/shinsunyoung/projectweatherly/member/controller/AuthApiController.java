package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    // 이메일 중복 체크 페이지
    @GetMapping("/check-email-page")
    public String showEmailCheckPage(Model model) {
        return "auth/check-email";
    }

    // 이메일 중복 체크 처리
    @PostMapping("/check-email")
    public String checkEmail(@RequestParam String email, Model model) {
        boolean exists = memberService.checkEmailExists(email);
        model.addAttribute("email", email);
        model.addAttribute("exists", exists);
        model.addAttribute("available", !exists);
        return "auth/check-email-result";
    }

    // 닉네임 중복 체크 페이지
    @GetMapping("/check-nickname-page")
    public String showNicknameCheckPage(Model model) {
        return "auth/check-nickname";
    }

    // 닉네임 중복 체크 처리
    @PostMapping("/check-nickname")
    public String checkNickname(@RequestParam String nickname, Model model) {
        boolean exists = memberService.checkNicknameExists(nickname);
        model.addAttribute("nickname", nickname);
        model.addAttribute("exists", exists);
        model.addAttribute("available", !exists);
        return "auth/check-nickname-result";
    }

    // 이메일 중복 확인 (JSON)
    @GetMapping("/api/check-email")
    @ResponseBody
    public ResponseEntity<Boolean> checkEmailJson(@RequestParam String email) {
        boolean exists = memberRepository.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }

    // 닉네임 중복 확인 (JSON)
    @GetMapping("/api/check-nickname")
    @ResponseBody
    public ResponseEntity<Boolean> checkNicknameJson(@RequestParam String nickname) {
        boolean exists = memberRepository.existsByNickname(nickname);
        return ResponseEntity.ok(exists);
    }

    // ==================== [NEW] 아이디/비밀번호 찾기 ====================

    // [수정됨] 경로 변경: "auth/find-account" -> "find-account"
    @GetMapping("/find-account")
    public String showFindAccountPage() {
        return "find-account"; // templates/find-account.html 을 찾음
    }

    // [API] 아이디(이메일) 찾기
    @PostMapping("/api/find-email")
    @ResponseBody
    public ResponseEntity<String> findEmail(@RequestParam String nickname) {
        try {
            String email = memberService.findEmailByNickname(nickname);
            return ResponseEntity.ok(email);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [API] 임시 비밀번호 발송
    @PostMapping("/api/reset-password")
    @ResponseBody
    public ResponseEntity<String> resetPassword(@RequestParam String email) {
        try {
            memberService.sendTemporaryPassword(email);
            return ResponseEntity.ok("임시 비밀번호가 발송되었습니다. (콘솔 확인)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}