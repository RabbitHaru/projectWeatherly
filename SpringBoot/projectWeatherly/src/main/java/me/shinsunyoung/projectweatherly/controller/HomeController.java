package me.shinsunyoung.projectweatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // 추가: 미세먼지 페이지 매핑
    @GetMapping("/fine-dust")
    public String fineDust() {
        return "fine-dust";
    }

    // 추가: 커뮤니티 페이지 매핑
    @GetMapping("/community")
    public String community() {
        return "community";
    }
} 