package me.shinsunyoung.projectweatherly.weather.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final LocationService locationService;

    // 1. 현재 위치 날씨
    @GetMapping("/current")
    public CompletableFuture<ApiResponse<WeatherResponseDTO>> getCurrentWeather(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            WeatherResponseDTO weather = weatherService.getWeatherByIp(request);
            return ApiResponse.success(weather);
        });
    }

    // 2. GPS 기반 날씨
    @PostMapping("/gps")
    public CompletableFuture<ApiResponse<WeatherResponseDTO>> getWeatherByGps(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            WeatherResponseDTO weather = weatherService.getWeatherByGps(latitude, longitude);
            return ApiResponse.success("GPS 위치 기반 날씨 정보", weather);
        });
    }

    // 3. 지역 날씨
    @GetMapping("/region/{regionCode}")
    public CompletableFuture<ApiResponse<WeatherResponseDTO>> getWeatherByRegion(
            @PathVariable String regionCode) {
        return CompletableFuture.supplyAsync(() -> {
            WeatherResponseDTO weather = weatherService.getWeatherByRegionCode(regionCode);
            return ApiResponse.success(weather);
        });
    }

    // 4. 지역 날씨 (가벼운 버전)
    @GetMapping("/region/{regionCode}/lite")
    public CompletableFuture<ApiResponse<WeatherResponseDTO>> getWeatherByRegionLite(
            @PathVariable String regionCode) {
        return CompletableFuture.supplyAsync(() -> {
            WeatherResponseDTO weather = weatherService.getWeatherByRegionCodeLite(regionCode);
            return ApiResponse.success(weather);
        });
    }

    // 5. 위치 동기화
    @PostMapping("/sync-location")
    public ApiResponse<LocationDTO> syncLocation(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            HttpServletRequest request) {

        LocationDTO location;
        if (latitude != null && longitude != null) {
            location = locationService.getLocationByGps(latitude, longitude);
        } else {
            String clientIp = locationService.getClientIp(request);
            location = locationService.getLocationByIp(clientIp);
        }
        return ApiResponse.success("위치 동기화 완료", location);
    }

    // [수정] /forecast 삭제됨, /compare/lite 삭제됨

    // 6. 전국 날씨 지도용 비교 API
    @GetMapping("/compare")
    public CompletableFuture<ApiResponse<List<WeatherResponseDTO>>> compareWeather(
            @RequestParam List<String> regionCodes) {
        return CompletableFuture.supplyAsync(() -> {
            List<WeatherResponseDTO> results = weatherService.compareWeatherLite(regionCodes);
            return ApiResponse.success(results);
        });
    }
}