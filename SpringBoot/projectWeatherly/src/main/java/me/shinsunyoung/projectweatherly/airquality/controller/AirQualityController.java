package me.shinsunyoung.projectweatherly.airquality.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityRequestDto;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDto;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/air-quality")
@RequiredArgsConstructor
public class AirQualityController {

    private final AirQualityService airQualityService;

    @GetMapping("/current")
    public ApiResponse<AirQualityResponseDto> getCurrentAirQuality(HttpServletRequest request) {
        AirQualityResponseDto airQuality = airQualityService.getAirQualityByIp(request);
        return ApiResponse.success(airQuality);
    }

    @PostMapping("/gps")
    public ApiResponse<AirQualityResponseDto> getAirQualityByGps(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        AirQualityResponseDto airQuality = airQualityService.getAirQualityByGps(latitude, longitude);
        return ApiResponse.success("GPS 위치 기반 대기질 정보", airQuality);
    }

    @GetMapping("/sido/{sidoName}")
    public ApiResponse<List<AirQualityResponseDto>> getAirQualityBySido(
            @PathVariable String sidoName) {
        List<AirQualityResponseDto> airQualityList = airQualityService.getAirQualityBySido(sidoName);
        return ApiResponse.success(airQualityList);
    }

    @GetMapping("/station/{stationName}")
    public ApiResponse<AirQualityResponseDto> getAirQualityByStation(
            @PathVariable String stationName) {
        AirQualityResponseDto airQuality = airQualityService.getAirQualityByStation(stationName);
        return ApiResponse.success(airQuality);
    }

    @GetMapping("/forecast/{sidoName}")
    public ApiResponse<List<AirQualityResponseDto.AirQualityForecast>> getAirQualityForecast(
            @PathVariable String sidoName) {
        List<AirQualityResponseDto.AirQualityForecast> forecasts =
                airQualityService.getAirQualityForecast(sidoName);
        return ApiResponse.success(forecasts);
    }

    @GetMapping("/compare")
    public ApiResponse<List<AirQualityResponseDto>> compareRegionalAirQuality(
            @RequestParam List<String> sidoNames) {
        List<AirQualityResponseDto> results = new java.util.ArrayList<>();
        for (String sidoName : sidoNames) {
            List<AirQualityResponseDto> sidoData = airQualityService.getAirQualityBySido(sidoName);
            if (!sidoData.isEmpty()) {
                results.add(sidoData.get(0));
            }
        }
        return ApiResponse.success(results);
    }

    @PostMapping("/search")
    public ApiResponse<AirQualityResponseDto> searchAirQuality(
            @RequestBody AirQualityRequestDto requestDto) {
        AirQualityResponseDto airQuality = airQualityService.getAirQuality(requestDto);
        return ApiResponse.success(airQuality);
    }
}