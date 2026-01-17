package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class UserController {

    @ModelAttribute
    public void addRequestUri(Model model, HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
    }

    @GetMapping("/login")
    public String login() {
        return "login";  // 이제 requestURI가 자동으로 추가됨
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";  // 이제 requestURI가 자동으로 추가됨
    }
}