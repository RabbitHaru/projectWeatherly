package me.shinsunyoung.projectweatherly.weather.controller;

import com.google.maps.internal.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import me.shinsunyoung.projectweatherly.weather.service.WeatherQueryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/weather")
public class WeatherQueryController {

    private final WeatherQueryService weatherQueryService;

    @GetMapping("/{regionCode}")
    public ApiResponse<Object> getWeatherByRegion(
            @PathVariable String regionCode
    ) {
        return ApiResponse.success(
                weatherQueryService.getWeatherByRegion(regionCode)
        );
    }
}
