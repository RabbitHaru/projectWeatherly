package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.outfit.service.OutfitService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/outfit")
public class OutfitController {

    private final OutfitService outfitService;
    private final WeatherService weatherService;

    public OutfitController(OutfitService outfitService,
                            WeatherService weatherService) {
        this.outfitService = outfitService;
        this.weatherService = weatherService;
    }

    /**
     * /outfit 페이지
     */
    @GetMapping
    public String outfitPage(Model model,
                             HttpServletRequest request,
                             @AuthenticationPrincipal UserSecurityDTO user) {

        // 공통 데이터
        model.addAttribute("requestURI", request.getRequestURI());

        if (user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }

        // 메인 페이지와 동일하게 IP 기반 날씨 조회
        WeatherResponseDTO weather = weatherService.getWeatherByIp(request);
        model.addAttribute("weather", weather);

        // 체감온도는 CurrentWeather 안에 있음
        Double feelsLike = weather.getCurrent().getFeelsLike();
        model.addAttribute("outfit", outfitService.decide(feelsLike.intValue()));


        return "outfit"; // templates/outfit.html
    }

    /**
     * /outfit/detail 페이지
     */
    @GetMapping("/detail")
    public String outfitDetailPage(Model model,
                                   HttpServletRequest request,
                                   @AuthenticationPrincipal UserSecurityDTO user) {

        model.addAttribute("requestURI", request.getRequestURI());

        if (user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }

        return "outfit-detail"; // templates/outfit-detail.html
    }
}
