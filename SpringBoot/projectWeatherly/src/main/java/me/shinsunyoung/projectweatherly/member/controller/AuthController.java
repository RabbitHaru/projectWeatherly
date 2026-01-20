package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.dto.request.LoginRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.SignupRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.LoginResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import me.shinsunyoung.projectweatherly.util.FileNameUtil;
import me.shinsunyoung.projectweatherly.util.FileUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Tag(name = "인증 컨트롤러", description = "회원가입, 로그인, 로그아웃 관련 페이지")
public class AuthController {

    private final MemberService memberService;
    private final FileUtil fileUtil;

    // 로그인 페이지
    @GetMapping("/login")
    @Operation(summary = "로그인 페이지", description = "로그인 폼을 표시합니다.")
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    // 로그인 처리
    @PostMapping("/login")
    @Operation(summary = "로그인 처리", description = "이메일과 비밀번호로 로그인합니다.")
    public String processLogin(
            @Valid @ModelAttribute LoginRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        try {
            LoginResponse response = memberService.login(request);
            redirectAttributes.addFlashAttribute("message", "로그인되었습니다.");
            redirectAttributes.addFlashAttribute("user", response);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    // 회원가입 페이지
    @GetMapping("/signup")
    @Operation(summary = "회원가입 페이지", description = "회원가입 폼을 표시합니다.")
    public String showSignupPage() {
        return "signup";
    }

    // 회원가입 처리
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "회원가입 처리", description = "새로운 회원을 등록합니다.")
    public String processSignup(
            @ModelAttribute SignupRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            List<FileNameUtil> fileNames = fileUtil.uploadFile(request.getProfileImage());
            String profileImg = fileNames != null && !fileNames.isEmpty() ?
                    fileNames.get(0).getNewFileName() : "default.png";

            Long memberId = memberService.signup(request, profileImg);

            redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다.");
            redirectAttributes.addFlashAttribute("memberId", memberId);
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

//    // 회원가입 성공 페이지
//    @GetMapping("/signup-success")
//    @Operation(summary = "회원가입 성공 페이지", description = "회원가입 성공 메시지를 표시합니다.")
//    public String showSignupSuccessPage() {
//        return "auth/signup-success";
//    }
//
//    // 로그아웃
//    @GetMapping("/logout")
//    @Operation(summary = "로그아웃", description = "사용자를 로그아웃합니다.")
//    public String logout(RedirectAttributes redirectAttributes) {
//        redirectAttributes.addFlashAttribute("message", "로그아웃되었습니다.");
//        return "redirect:/auth/login";
//    }
}