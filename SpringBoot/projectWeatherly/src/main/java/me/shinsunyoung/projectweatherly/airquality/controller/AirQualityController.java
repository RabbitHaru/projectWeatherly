package me.shinsunyoung.projectweatherly.airquality.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/air-quality")
@RequiredArgsConstructor
@Slf4j
public class AirQualityController {

    private final AirQualityService airQualityService;

    @GetMapping("/current")
    public ApiResponse<AirQualityResponseDTO> getCurrentAirQuality(HttpServletRequest request) {
        try {
            return ApiResponse.success(airQualityService.getAirQualityByIp(request));
        } catch (Exception e) { return ApiResponse.success(null); }
    }

    @PostMapping("/gps")
    public ApiResponse<AirQualityResponseDTO> getAirQualityByGps(@RequestParam Double latitude, @RequestParam Double longitude) {
        try {
            return ApiResponse.success("GPS 위치 기반 대기질 정보", airQualityService.getAirQualityByGps(latitude, longitude));
        } catch (Exception e) { return ApiResponse.error("GPS 대기질 정보를 가져오는데 실패했습니다."); }
    }

    @GetMapping("/forecast/{sidoName}")
    public ApiResponse<List<AirQualityResponseDTO.AirQualityForecast>> getAirQualityForecast(@PathVariable String sidoName) {
        return ApiResponse.success(airQualityService.getAirQualityForecast(sidoName));
    }

    @GetMapping("/sido/{sidoName}")
    public ApiResponse<List<AirQualityResponseDTO>> getAirQualityBySido(@PathVariable String sidoName) {
        return ApiResponse.success(airQualityService.getAirQualityBySido(sidoName));
    }

    // 강제 업데이트 (테스트용)
    @GetMapping("/force-update")
    public ApiResponse<String> forceUpdate() {
        airQualityService.updateRealtimeData();
        airQualityService.updateForecastData();
        return ApiResponse.success("강제 업데이트 실행됨");
    }
}