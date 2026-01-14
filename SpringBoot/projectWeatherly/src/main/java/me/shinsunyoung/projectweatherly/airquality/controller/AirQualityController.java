package me.shinsunyoung.projectweatherly.airquality.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityRequestDTO;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
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
    public ApiResponse<AirQualityResponseDTO> getCurrentAirQuality(HttpServletRequest request) {
        AirQualityResponseDTO airQuality = airQualityService.getAirQualityByIp(request);
        return ApiResponse.success(airQuality);
    }

    @PostMapping("/gps")
    public ApiResponse<AirQualityResponseDTO> getAirQualityByGps(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        AirQualityResponseDTO airQuality = airQualityService.getAirQualityByGps(latitude, longitude);
        return ApiResponse.success("GPS 위치 기반 대기질 정보", airQuality);
    }

    @GetMapping("/sido/{sidoName}")
    public ApiResponse<List<AirQualityResponseDTO>> getAirQualityBySido(
            @PathVariable String sidoName) {
        List<AirQualityResponseDTO> airQualityList = airQualityService.getAirQualityBySido(sidoName);
        return ApiResponse.success(airQualityList);
    }

    @GetMapping("/station/{stationName}")
    public ApiResponse<AirQualityResponseDTO> getAirQualityByStation(
            @PathVariable String stationName) {
        AirQualityResponseDTO airQuality = airQualityService.getAirQualityByStation(stationName);
        return ApiResponse.success(airQuality);
    }

    @GetMapping("/forecast/{sidoName}")
    public ApiResponse<List<AirQualityResponseDTO.AirQualityForecast>> getAirQualityForecast(
            @PathVariable String sidoName) {
        List<AirQualityResponseDTO.AirQualityForecast> forecasts =
                airQualityService.getAirQualityForecast(sidoName);
        return ApiResponse.success(forecasts);
    }

    @GetMapping("/compare")
    public ApiResponse<List<AirQualityResponseDTO>> compareRegionalAirQuality(
            @RequestParam List<String> sidoNames) {
        List<AirQualityResponseDTO> results = new java.util.ArrayList<>();
        for (String sidoName : sidoNames) {
            List<AirQualityResponseDTO> sidoData = airQualityService.getAirQualityBySido(sidoName);
            if (!sidoData.isEmpty()) {
                results.add(sidoData.get(0));
            }
        }
        return ApiResponse.success(results);
    }

    @PostMapping("/search")
    public ApiResponse<AirQualityResponseDTO> searchAirQuality(
            @RequestBody AirQualityRequestDTO requestDto) {
        AirQualityResponseDTO airQuality = airQualityService.getAirQuality(requestDto);
        return ApiResponse.success(airQuality);
    }

}