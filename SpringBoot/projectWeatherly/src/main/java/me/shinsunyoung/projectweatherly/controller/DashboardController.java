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
    

    @GetMapping("/insights")
    public String insightsPage(HttpServletRequest request, Model model,
                               @RequestParam(required = false) Double lat,
                               @RequestParam(required = false) Double lon) {

        WeatherResponseDTO weather = null;
        AirQualityResponseDTO air = null;

        // 1. GPS 좌표가 있으면 우선 시도
        if (lat != null && lon != null) {
            try {
                weather = weatherService.getWeatherByGps(lat, lon);
            } catch (Exception e) {
                log.error("GPS 날씨 조회 실패: {}", e.getMessage());
            }

            try {
                air = airQualityService.getAirQualityByGps(lat, lon);
            } catch (Exception e) {
                log.error("GPS 미세먼지 조회 실패: {}", e.getMessage());
            }
        }

        // 2. 날씨 데이터가 없으면 IP 기반으로 조회 (미세먼지가 없어도 날씨가 있으면 유지!)
        if (weather == null) {
            try {
                weather = weatherService.getWeatherByIp(request);
            } catch (Exception e) {
                log.error("IP 날씨 조회 실패", e);
            }
        }

        // 3. 미세먼지 데이터가 없으면 IP 기반으로 재시도 (선택 사항)
        if (air == null) {
            try {
                air = airQualityService.getAirQualityByIp(request);
            } catch (Exception e) {
                log.error("IP 미세먼지 조회 실패", e);
            }
        }

        // 4. 모델 데이터 담기 (weather가 null일 경우 방어 로직)
        if (weather != null) {
            model.addAttribute("regionName", weather.getRegionName());
        } else {
            model.addAttribute("regionName", "위치 정보 없음");
        }

        String station = (air != null && air.getStationName() != null) ? air.getStationName() : "측정소";
        model.addAttribute("stationName", station + " 측정소");

        // --- (이하 차트 데이터 처리 로직은 기존과 동일) ---

        List<Double> maxTemps = new ArrayList<>();
        List<Double> minTemps = new ArrayList<>();

        if (weather != null && weather.getDaily() != null) {
            for (var day : weather.getDaily()) {
                if (day.getMaxTemp() != null) maxTemps.add(day.getMaxTemp());
                if (day.getMinTemp() != null) minTemps.add(day.getMinTemp());
            }
        }

        while (maxTemps.size() < 7) {
            double lastMax = maxTemps.isEmpty() ? 20.0 : maxTemps.get(maxTemps.size() - 1);
            double lastMin = minTemps.isEmpty() ? 10.0 : minTemps.get(minTemps.size() - 1);
            maxTemps.add(lastMax);
            minTemps.add(lastMin);
        }
        if (maxTemps.size() > 7) {
            maxTemps = maxTemps.subList(0, 7);
            minTemps = minTemps.subList(0, 7);
        }

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

        if (humidityData.isEmpty()) {
            for(int i=0; i<6; i++) {
                humidityData.add(50.0);
                windData.add(2.0);
                hourLabels.add((i*2)+"시");
            }
        }

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