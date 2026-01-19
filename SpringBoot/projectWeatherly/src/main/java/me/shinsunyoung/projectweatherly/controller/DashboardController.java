package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final WeatherService weatherService;
    private final AirQualityService airQualityService;

    @GetMapping("/admin")
    public String adminPage(Model model) {
        return "admin";
    }

    @GetMapping("/insights")
    public String insightsPage(HttpServletRequest request, Model model,
                               @RequestParam(required = false) Double lat,
                               @RequestParam(required = false) Double lon) {

        WeatherResponseDTO weather = null;
        AirQualityResponseDTO air = null;

        // 1. GPS 좌표가 있으면 우선 시도
        try {
            if (lat != null && lon != null) {
                log.info("GPS 좌표 수신: lat={}, lon={}", lat, lon);
                weather = weatherService.getWeatherByGps(lat, lon);
                air = airQualityService.getAirQualityByGps(lat, lon);
            }
        } catch (Exception e) {
            log.error("GPS 위치 동기화 실패 (IP로 대체): {}", e.getMessage());
            weather = null;
            air = null;
        }

        // 2. 데이터가 없으면(GPS 미입력 or 에러) IP 기반 조회
        if (weather == null || air == null) {
            weather = weatherService.getWeatherByIp(request);
            air = airQualityService.getAirQualityByIp(request);
        }

        // 3. 지역 정보 표시
        model.addAttribute("regionName", weather.getRegionName());
        String station = (air != null && air.getStationName() != null) ? air.getStationName() : "측정소";
        model.addAttribute("stationName", station + " 측정소");

        // 4. [주간 기온] 데이터 채우기 (Padding Logic)
        List<Double> maxTemps = new ArrayList<>();
        List<Double> minTemps = new ArrayList<>();

        if (weather != null && weather.getDaily() != null) {
            for (var day : weather.getDaily()) {
                maxTemps.add(day.getMaxTemp());
                minTemps.add(day.getMinTemp());
            }
        }
        // 데이터 부족 시 채우기
        while (maxTemps.size() < 7) {
            double lastMax = maxTemps.isEmpty() ? 20.0 : maxTemps.get(maxTemps.size() - 1);
            double lastMin = minTemps.isEmpty() ? 10.0 : minTemps.get(minTemps.size() - 1);
            maxTemps.add(lastMax);
            minTemps.add(lastMin);
        }
        // 7개로 자르기
        if (maxTemps.size() > 7) {
            maxTemps = maxTemps.subList(0, 7);
            minTemps = minTemps.subList(0, 7);
        }

        // 5. [미세먼지] 12시간 추이 (시뮬레이션 데이터)
        List<Integer> pm10Data = new ArrayList<>();
        List<Integer> pm25Data = new ArrayList<>();
        int currentPm10 = (air != null && air.getPm10() != null) ? air.getPm10().getValue() : 30;
        int currentPm25 = (air != null && air.getPm25() != null) ? air.getPm25().getValue() : 15;

        for (int i = 0; i < 12; i++) {
            pm10Data.add(Math.max(0, currentPm10 + (int)(Math.random() * 10 - 5)));
            pm25Data.add(Math.max(0, currentPm25 + (int)(Math.random() * 6 - 3)));
        }
        pm10Data.set(11, currentPm10);
        pm25Data.set(11, currentPm25);

        // 6. [습도/바람] 시간별 예보 활용
        List<Double> humidityData = new ArrayList<>();
        List<Double> windData = new ArrayList<>();
        List<String> hourLabels = new ArrayList<>();

        if (weather != null && weather.getHourly() != null) {
            int count = 0;
            for (var h : weather.getHourly()) {
                if (count >= 12) break;
                humidityData.add(h.getHumidity());
                windData.add(h.getWindSpeed());
                hourLabels.add(h.getTime());
                count++;
            }
        }
        // 데이터 안전장치
        if (humidityData.isEmpty()) {
            for(int i=0; i<6; i++) {
                humidityData.add(50.0);
                windData.add(2.0);
                hourLabels.add((i*2)+"시");
            }
        }

        // 7. 모델 전송
        model.addAttribute("maxTemps", maxTemps);
        model.addAttribute("minTemps", minTemps);
        model.addAttribute("pm10Data", pm10Data);
        model.addAttribute("pm25Data", pm25Data);
        model.addAttribute("humidityData", humidityData);
        model.addAttribute("windData", windData);
        model.addAttribute("hourLabels", hourLabels);

        return "insights";
    }
}