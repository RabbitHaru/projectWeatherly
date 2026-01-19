package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
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
    private final AirQualityService airQualityService; // [추가] 미세먼지 서비스

    public OutfitController(OutfitService outfitService,
                            WeatherService weatherService,
                            OutfitForecastService outfitForecastService,
                            AirQualityService airQualityService) { // [추가] 생성자 주입
        this.outfitService = outfitService;
        this.weatherService = weatherService;
        this.outfitForecastService = outfitForecastService;
        this.airQualityService = airQualityService;
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

        // 1. 🌦 날씨 조회
        WeatherResponseDTO weatherResponse =
                weatherService.getWeatherByIp(request);

        // 2. 😷 미세먼지 조회 [추가]
        // 에러가 발생해도 페이지는 떠야 하므로 try-catch로 방어 처리
        AirQualityResponseDTO airQuality = null;
        try {
            airQuality = airQualityService.getAirQualityByIp(request);
        } catch (Exception e) {
            // 로그를 남기거나 null 처리 (화면에서는 데이터 없음으로 표시됨)
            airQuality = null;
        }
        model.addAttribute("airQuality", airQuality);


        WeatherResponseDTO.CurrentWeather current =
                weatherResponse.getCurrent();

        // 3. 👕 현재 옷차림 조건
        OutfitConditionDTO condition = new OutfitConditionDTO(
                current.getFeelsLike(),
                current.getWindSpeed(),
                current.getHumidity(),
                0.0,
                current.getWeatherCondition()
        );

        // 4. 👕 현재 옷차림 추천
        model.addAttribute("outfit",
                outfitService.decide(condition));

        // 5. 📆 단기 예보 옷차림 (outer 중심)
        model.addAttribute(
                "forecastOutfits",
                outfitForecastService.createForecasts(
                        weatherResponse.getDaily()
                )
        );

        // 6. 기타 정보
        model.addAttribute("currentTime",
                weatherResponse.getCurrentTime());

        model.addAttribute("location",
                Map.of("name", weatherResponse.getRegionName()));

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