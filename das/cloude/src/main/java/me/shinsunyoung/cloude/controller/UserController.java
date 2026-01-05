package me.shinsunyoung.cloude.controller;



import lombok.RequiredArgsConstructor;
import me.shinsunyoung.cloude.entity.User;
import me.shinsunyoung.cloude.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입 화면
    @GetMapping("/join")
    public String signupForm() {
        return "join";
    }

    // 회원가입 처리
    @PostMapping("/join")
    public String signup(User user) {
        userService.register(user);
        return "redirect:/login";
    }

    // 임시 로그인 페이지
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
