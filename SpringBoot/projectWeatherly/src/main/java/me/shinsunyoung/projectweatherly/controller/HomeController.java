package me.shinsunyoung.projectweatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";  // 또는 "main" 등 적절한 뷰 이름
    }
}