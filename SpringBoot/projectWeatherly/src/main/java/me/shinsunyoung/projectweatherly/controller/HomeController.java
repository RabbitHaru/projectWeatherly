package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.beans.factory.annotation.Value; // [필수] import 추가
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // [추가] application.properties에서 키 가져오기
    @Value("${weatherly.kakao.map.key}")
    private String kakaoMapKey;

    @GetMapping({"/", "/index.html"})
    public String home(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());

        // [추가] HTML로 키 전달
        model.addAttribute("kakaoMapKey", kakaoMapKey);

        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
        return "index"; // templates/index.html을 반환
    }

    // 미세먼지 페이지 매핑
    @GetMapping("/fine-dust")
    public String fineDust(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());

        // [추가] 여기도 지도 쓸 수 있으니 키 전달
        model.addAttribute("kakaoMapKey", kakaoMapKey);

        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
        return "fine-dust";
    }
}