package me.shinsunyoung.projectweatherly.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "홈 - Weatherly");
        model.addAttribute("activePage", "home");
        return "index";
    }

    @GetMapping("/fine-dust")
    public String fineDustPage(Model model) {
        model.addAttribute("pageTitle", "미세먼지 정보");
        model.addAttribute("activePage", "fine-dust");
        return "fine-dust";
    }

    @GetMapping("/outfit")
    public String outfitPage(Model model) {
        model.addAttribute("pageTitle", "옷차림 추천");
        model.addAttribute("activePage", "outfit");
        return "outfit";
    }

    @GetMapping("/community")
    public String communityPage(Model model) {
        model.addAttribute("pageTitle", "커뮤니티");
        model.addAttribute("activePage", "community");
        return "community";
    }

    @GetMapping("/mypage")
    public String mypage(Model model) {
        model.addAttribute("pageTitle", "마이페이지");
        model.addAttribute("activePage", "mypage");
        return "mypage";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "설정");
        return "settings";
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        model.addAttribute("pageTitle", "즐겨찾기");
        return "favorites";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "로그인");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(Model model) {
        model.addAttribute("pageTitle", "로그아웃");
        return "logout";
    }
}