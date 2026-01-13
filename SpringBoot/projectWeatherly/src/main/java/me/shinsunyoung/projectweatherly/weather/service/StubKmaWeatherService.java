package me.shinsunyoung.projectweatherly.weather.service;

import org.springframework.stereotype.Service;

@Service
public class StubKmaWeatherService implements KmaWeatherService {

    @Override
    public Object getWeather(int nx, int ny) {
        return "WEATHER_STUB_RESPONSE";
    }
}
