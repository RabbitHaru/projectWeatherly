package me.shinsunyoung.projectweatherly.weather.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.region.Service.RegionService;
import me.shinsunyoung.projectweatherly.region.domain.Region;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherQueryService {

    private final RegionService regionService;
    private final KmaWeatherService kmaWeatherService;

    public Object getWeatherByRegion(String regionCode) {

        Region region = regionService.getActiveRegion(regionCode);

        return kmaWeatherService.getWeather(
                region.getNx(),
                region.getNy()
        );
    }
}
