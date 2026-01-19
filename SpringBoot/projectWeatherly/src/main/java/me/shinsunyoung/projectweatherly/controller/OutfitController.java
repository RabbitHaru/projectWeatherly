package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.outfit.dto.OutfitConditionDTO;
import me.shinsunyoung.projectweatherly.outfit.model.OutfitSet;
import me.shinsunyoung.projectweatherly.outfit.service.OutfitForecastService;
import me.shinsunyoung.projectweatherly.outfit.service.OutfitRecommendationService;
import me.shinsunyoung.projectweatherly.outfit.service.OutfitService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/outfit")
@RequiredArgsConstructor
@Slf4j
public class OutfitController {

    private final OutfitService outfitService;
    private final WeatherService weatherService;
    private final OutfitForecastService outfitForecastService;
    private final AirQualityService airQualityService;
    private final OutfitRecommendationService outfitRecommendationService;

    // =========================
    // /outfit (메인 페이지)
    // =========================
    @GetMapping
    public String outfitPage(Model model,
                             HttpServletRequest request,
                             @AuthenticationPrincipal UserSecurityDTO user,
                             @RequestParam(required = false) Double lat,
                             @RequestParam(required = false) Double lon) {

        addCommonAttributes(model, request, user);

        // 날씨 & 미세먼지 로딩
        WeatherResponseDTO weatherResponse = loadWeatherData(model, request, lat, lon);

        // 옷차림 요약 (데이터 있을 때만 실행)
        if (weatherResponse != null && weatherResponse.getCurrent() != null) {
            WeatherResponseDTO.CurrentWeather current = weatherResponse.getCurrent();
            OutfitConditionDTO condition = new OutfitConditionDTO(
                    current.getFeelsLike(),
                    current.getWindSpeed(),
                    current.getHumidity(),
                    0.0,
                    current.getWeatherCondition()
            );
            model.addAttribute("outfit", outfitService.decide(condition));
        }

        // 단기 예보
        if (weatherResponse != null && weatherResponse.getDaily() != null) {
            model.addAttribute("forecastOutfits",
                    outfitForecastService.createForecasts(weatherResponse.getDaily()));
        }

        return "outfit";
    }

    // =========================
    // /outfit/detail (상세 페이지)
    // =========================
    @GetMapping("/detail")
    public String outfitDetailPage(Model model,
                                   HttpServletRequest request,
                                   @AuthenticationPrincipal UserSecurityDTO user,
                                   @RequestParam(required = false) Double lat,
                                   @RequestParam(required = false) Double lon) {

        addCommonAttributes(model, request, user);

        // 1. 날씨 데이터 조회
        WeatherResponseDTO weatherResponse = loadWeatherData(model, request, lat, lon);

        // [핵심 수정] 날씨 데이터가 없으면 에러가 나므로, 강제로 메인으로 돌려보냄
        if (weatherResponse == null || weatherResponse.getCurrent() == null) {
            log.warn("날씨 데이터가 없어 상세 페이지를 띄울 수 없습니다. 메인으로 리다이렉트합니다.");
            return "redirect:/outfit";
        }

        // 2. 상세 옷차림 아이템 추천 (안전하게 조회)
        OutfitSet outfitSet = null;
        try {
            double feelsLike = weatherResponse.getCurrent().getFeelsLike();
            outfitSet = outfitRecommendationService.recommend(feelsLike);
        } catch (Exception e) {
            log.error("상세 옷차림 추천 실패", e);
        }

        // 데이터가 없으면 빈 객체를 넣어 화면 꺼짐 방지
        if (outfitSet == null) {
            outfitSet = new OutfitSet(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        model.addAttribute("outfitSet", outfitSet);


        // 3. 요약 정보 (Level 표시용)
        OutfitConditionDTO condition = new OutfitConditionDTO(
                weatherResponse.getCurrent().getFeelsLike(),
                weatherResponse.getCurrent().getWindSpeed(),
                weatherResponse.getCurrent().getHumidity(),
                0.0,
                weatherResponse.getCurrent().getWeatherCondition()
        );
        model.addAttribute("outfit", outfitService.decide(condition));

        return "outfit-detail";
    }

    // --- 공통 메서드 ---

    private void addCommonAttributes(Model model, HttpServletRequest request, UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());
        if (user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
    }

    private WeatherResponseDTO loadWeatherData(Model model, HttpServletRequest request, Double lat, Double lon) {
        // 날씨 조회
        WeatherResponseDTO weatherResponse = null;
        try {
            if (lat != null && lon != null) {
                weatherResponse = weatherService.getWeatherByGps(lat, lon);
            } else {
                weatherResponse = weatherService.getWeatherByIp(request);
            }
        } catch (Exception e) {
            log.error("날씨 조회 에러", e);
        }

        model.addAttribute("weather", weatherResponse);

        if (weatherResponse != null) {
            model.addAttribute("currentTime", weatherResponse.getCurrentTime());
            model.addAttribute("location", Map.of("name", weatherResponse.getRegionName()));
        }

        // 미세먼지 조회
        AirQualityResponseDTO airQuality = null;
        try {
            if (lat != null && lon != null) {
                airQuality = airQualityService.getAirQualityByGps(lat, lon);
            } else {
                airQuality = airQualityService.getAirQualityByIp(request);
            }
        } catch (Exception e) {
            log.error("미세먼지 조회 실패", e);
        }
        model.addAttribute("airQuality", airQuality);

        return weatherResponse;
    }
}