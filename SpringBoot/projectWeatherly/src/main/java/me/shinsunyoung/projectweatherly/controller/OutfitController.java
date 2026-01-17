package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class OutfitController {

    private static final Logger logger = LoggerFactory.getLogger(OutfitController.class);

    @GetMapping("/outfit")
    public String outfitPage(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());
        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
        return "outfit"; // templates/outfit.html을 반환
    }

    @GetMapping("/outfit/detail")
    public String outfitDetailPage(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());
        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
        return "outfit-detail";
    }

}