package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.outfit.dto.OutfitConditionDTO;
import me.shinsunyoung.projectweatherly.outfit.service.OutfitForecastService;
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
    private final OutfitForecastService outfitForecastService;

    public OutfitController(OutfitService outfitService,
                            WeatherService weatherService,
                            OutfitForecastService outfitForecastService) {
        this.outfitService = outfitService;
        this.weatherService = weatherService;
        this.outfitForecastService = outfitForecastService;
    }

    // =========================
    // /outfit 페이지
    // =========================
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

        // 👕 현재 옷차림 조건
        OutfitConditionDTO condition = new OutfitConditionDTO(
                current.getFeelsLike(),
                current.getWindSpeed(),
                current.getHumidity(),
                0.0,
                current.getWeatherCondition()
        );

        // 👕 현재 옷차림
        model.addAttribute("outfit",
                outfitService.decide(condition));

        // 📆 단기 예보 옷차림 (outer 중심)
        model.addAttribute(
                "forecastOutfits",
                outfitForecastService.createForecasts(
                        weatherResponse.getDaily()
                )
        );

        // ⏰ 시간
        model.addAttribute("currentTime",
                weatherResponse.getCurrentTime());

        // 📍 위치
        model.addAttribute("location",
                Map.of("name", weatherResponse.getRegionName()));

        // 🌦 전체 날씨
        model.addAttribute("weather", weatherResponse);

        return "outfit";
    }

    // =========================
    // /outfit/detail 페이지
    // =========================
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
