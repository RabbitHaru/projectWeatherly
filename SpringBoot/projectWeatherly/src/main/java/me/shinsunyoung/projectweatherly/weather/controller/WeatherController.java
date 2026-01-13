package me.shinsunyoung.projectweatherly.weather.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
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

    @GetMapping("/current")
    public ApiResponse<WeatherResponseDto> getCurrentWeather(HttpServletRequest request) {
        WeatherResponseDto weather = weatherService.getWeatherByIp(request);
        return ApiResponse.success(weather);
    }

    @PostMapping("/gps")
    public ApiResponse<WeatherResponseDto> getWeatherByGps(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        WeatherResponseDto weather = weatherService.getWeatherByGps(latitude, longitude);
        return ApiResponse.success("GPS 위치 기반 날씨 정보", weather);
    }

    @GetMapping("/region/{regionCode}")
    public ApiResponse<WeatherResponseDto> getWeatherByRegion(
            @PathVariable String regionCode) {
        WeatherResponseDto weather = weatherService.getWeatherByRegionCode(regionCode);
        return ApiResponse.success(weather);
    }

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

    @PostMapping("/forecast")
    public ApiResponse<WeatherResponseDto> getWeatherForecast(
            @RequestBody WeatherRequestDto requestDto) {
        WeatherResponseDto weather = weatherService.getWeather(requestDto);
        return ApiResponse.success(weather);
    }

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