package me.shinsunyoung.projectweatherly.weather.controller;

import com.google.maps.internal.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherRequestDto;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDto;
import me.shinsunyoung.projectweatherly.weather.service.WeatherService;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final LocationService locationService;

    /**
     * IP 기반 현재 위치 날씨 조회
     */
    @GetMapping("/current")
    public ApiResponse<WeatherResponseDto> getCurrentWeather(HttpServletRequest request) {
        WeatherResponseDto weather = weatherService.getWeatherByIp(request);
        return ApiResponse.success(weather);
    }

    /**
     * GPS 좌표 기반 날씨 조회
     */
    @PostMapping("/gps")
    public ApiResponse<WeatherResponseDto> getWeatherByGps(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        WeatherResponseDto weather = weatherService.getWeatherByGps(latitude, longitude);
        return ApiResponse.success("GPS 위치 기반 날씨 정보", weather);
    }

    /**
     * 지역 코드 기반 날씨 조회
     */
    @GetMapping("/region/{regionCode}")
    public ApiResponse<WeatherResponseDto> getWeatherByRegion(
            @PathVariable String regionCode) {
        WeatherResponseDto weather = weatherService.getWeatherByRegionCode(regionCode);
        return ApiResponse.success(weather);
    }

    /**
     * 위치 동기화 (클라이언트에서 GPS 정보 전송)
     */
    @PostMapping("/sync-location")
    public ApiResponse<LocationDTO> syncLocation(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            HttpServletRequest request) {

        LocationDTO location;
        if (latitude != null && longitude != null) {
            // GPS 기반 위치 정보
            location = locationService.getLocationByGps(latitude, longitude);
        } else {
            // IP 기반 위치 정보
            String clientIp = locationService.getClientIp(request);
            location = locationService.getLocationByIp(clientIp);
        }

        return ApiResponse.success("위치 동기화 완료", location);
    }

    /**
     * 다양한 파라미터로 날씨 조회
     */
    @PostMapping("/forecast")
    public ApiResponse<WeatherResponseDto> getWeatherForecast(
            @RequestBody WeatherRequestDto requestDto) {
        WeatherResponseDto weather = weatherService.getWeather(requestDto);
        return ApiResponse.success(weather);
    }

    /**
     * 지역별 날씨 비교
     */
    @GetMapping("/compare")
    public ApiResponse<List<WeatherResponseDto>> compareWeather(
            @RequestParam List<String> regionCodes) {
        List<WeatherResponseDto> results = new java.util.ArrayList<>();
        for (String regionCode : regionCodes) {
            results.add(weatherService.getWeatherByRegionCode(regionCode));
        }
        return ApiResponse.success(results);
    }
}