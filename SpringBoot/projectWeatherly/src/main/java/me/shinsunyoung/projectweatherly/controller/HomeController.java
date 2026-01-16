package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/index.html"})
    public String home(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());
        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
        return "index"; // templates/index.html을 반환
    }
    // 추가: 미세먼지 페이지 매핑
    @GetMapping("/fine-dust")
    public String fineDust() {
        return "fine-dust";
    }



} 