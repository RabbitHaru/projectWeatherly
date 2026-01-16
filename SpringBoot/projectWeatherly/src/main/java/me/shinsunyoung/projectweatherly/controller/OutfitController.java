package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.outfit.dto.OutfitConditionDTO;
import me.shinsunyoung.projectweatherly.outfit.service.OutfitService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

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

    // /outfit 페이지
    @GetMapping
    public String outfitPage(Model model,
                             HttpServletRequest request,
                             @AuthenticationPrincipal UserSecurityDTO user) {

        model.addAttribute("requestURI", request.getRequestURI());

        if (user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }

        // 🌦 날씨 조회
        WeatherResponseDTO weatherResponse =
                weatherService.getWeatherByIp(request);

        WeatherResponseDTO.CurrentWeather current =
                weatherResponse.getCurrent();

        // 👕 옷차림 조건 DTO
        OutfitConditionDTO condition = new OutfitConditionDTO(
                current.getFeelsLike(),
                current.getWindSpeed(),
                current.getHumidity(),
                0.0, // 강수량은 추후
                current.getWeatherCondition()
        );

        // 👕 옷차림 추천
        model.addAttribute("outfit",
                outfitService.decide(condition));

        // ⏰ 시간
        model.addAttribute("currentTime",
                weatherResponse.getCurrentTime());

        // 📍 위치
        model.addAttribute("location",
                Map.of("name", weatherResponse.getRegionName()));

        // 🌦 날씨 전체
        model.addAttribute("weather", weatherResponse);

        return "outfit";
    }

    // /outfit/detail 페이지
    @GetMapping("/detail")
    public String outfitDetailPage(Model model,
                                   HttpServletRequest request,
                                   @AuthenticationPrincipal UserSecurityDTO user) {

        model.addAttribute("requestURI", request.getRequestURI());

        if (user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }

        return "outfit-detail";
    }
}
