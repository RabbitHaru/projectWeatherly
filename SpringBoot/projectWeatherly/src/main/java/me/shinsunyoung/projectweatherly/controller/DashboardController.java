package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    // 날씨 & 대기질 서비스 (Insights 페이지용)
    private final WeatherService weatherService;
    private final AirQualityService airQualityService;

    // [추가] 리포지토리 (Admin 페이지 DB 연동용)
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;


    /**
     * [기상 인사이트 페이지]
     * - 날씨/대기질 차트 데이터 및 GPS 위치 연동
     */
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

        // --- 차트 데이터 처리 (기존 로직 유지) ---
        List<Double> maxTemps = new ArrayList<>();
        List<Double> minTemps = new ArrayList<>();

        if (weather != null && weather.getDaily() != null) {
            for (var day : weather.getDaily()) {
                if (day.getMaxTemp() != null) maxTemps.add(day.getMaxTemp());
                if (day.getMinTemp() != null) minTemps.add(day.getMinTemp());
            }
        }

        // 데이터 부족 시 채우기 (그래프 깨짐 방지)
        while (maxTemps.size() < 7) {
            double lastMax = maxTemps.isEmpty() ? 20.0 : maxTemps.get(maxTemps.size() - 1);
            double lastMin = minTemps.isEmpty() ? 10.0 : minTemps.get(minTemps.size() - 1);
            maxTemps.add(lastMax);
            minTemps.add(lastMin);
        }
        // 7일치만 자르기
        if (maxTemps.size() > 7) {
            maxTemps = maxTemps.subList(0, 7);
            minTemps = minTemps.subList(0, 7);
        }

        // 미세먼지 시뮬레이션 데이터 (현재 값 기준 변동)
        List<Integer> pm10Data = new ArrayList<>();
        List<Integer> pm25Data = new ArrayList<>();
        int currentPm10 = (air != null && air.getPm10() != null) ? air.getPm10().getValue() : 30;
        int currentPm25 = (air != null && air.getPm25() != null) ? air.getPm25().getValue() : 15;

        for (int i = 0; i < 12; i++) {
            pm10Data.add(Math.max(0, currentPm10 + (int)(Math.random() * 10 - 5)));
            pm25Data.add(Math.max(0, currentPm25 + (int)(Math.random() * 6 - 3)));
        }
        // 마지막 값은 실제 현재 값으로 맞춤
        pm10Data.set(11, currentPm10);
        pm25Data.set(11, currentPm25);

        // 습도/바람 데이터
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