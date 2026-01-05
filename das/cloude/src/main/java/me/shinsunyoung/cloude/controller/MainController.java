package me.shinsunyoung.cloude.controller;


import jakarta.servlet.http.HttpSession;
import me.shinsunyoung.cloude.dto.HourlyForecast;
import me.shinsunyoung.cloude.dto.Post;
import me.shinsunyoung.cloude.dto.WeatherDto;
import me.shinsunyoung.cloude.dto.WeeklyForecast;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(HttpSession session, Model model) {


        Object loginUser = session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);
        return "index";
    }
}
