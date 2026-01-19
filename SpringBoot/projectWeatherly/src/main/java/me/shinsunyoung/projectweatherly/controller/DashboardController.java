package me.shinsunyoung.projectweatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    // 1. 관리자 대시보드
    @GetMapping("/admin")
    public String adminPage(Model model) {
        return "admin";
    }

    // 2. 기상 통계 인사이트
    @GetMapping("/insights")
    public String insightsPage(Model model) {

        return "insights";
    }
}