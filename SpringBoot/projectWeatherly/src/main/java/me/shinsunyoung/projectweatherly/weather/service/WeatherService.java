package me.shinsunyoung.projectweatherly.weather.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.LocationDto;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherRequestDto;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDto;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final LocationService locationService;

    /**
     * IP 기반 현재 위치의 날씨 정보 조회
     */
    public WeatherResponseDto getWeatherByIp(HttpServletRequest request) {
        String clientIp = locationService.getClientIp(request);
        LocationDTO location = locationService.getLocationByIp(clientIp);

        log.info("IP 기반 날씨 조회: {} -> {}", clientIp, location.getRegionName());

        return getWeatherByRegionCode(location.getRegionCode());
    }

    /**
     * GPS 좌표 기반 날씨 정보 조회
     */
    public WeatherResponseDto getWeatherByGps(Double latitude, Double longitude) {
        LocationDTO location = locationService.getLocationByGps(latitude, longitude);

        log.info("GPS 기반 날씨 조회: ({}, {}) -> {}",
                latitude, longitude, location.getRegionName());

        return getWeatherByRegionCode(location.getRegionCode());
    }

    /**
     * 지역 코드로 날씨 정보 조회
     */
    public WeatherResponseDto getWeatherByRegionCode(String regionCode) {
        WeatherResponseDto response = new WeatherResponseDto();

        // 현재 날씨
        response.setCurrent(weatherApiService.getCurrentWeather(regionCode));

        // 시간별 예보
        WeatherResponseDto ultraShort = weatherApiService.getUltraShortForecast(
                regionCode,
                me.shinsunyoung.projectweatherly.common.util.DateUtil.formatDateOnly(java.time.LocalDateTime.now()),
                me.shinsunyoung.projectweatherly.common.util.DateUtil.getBaseTime()
        );
        response.setHourly(ultraShort.getHourly());

        // 일별 예보
        WeatherResponseDto shortTerm = weatherApiService.getShortTermForecast(regionCode);
        response.setDaily(shortTerm.getDaily());

        // 지역 정보 및 시간
        response.setRegionName(ultraShort.getRegionName());
        response.setRegionCode(regionCode);
        response.setCurrentTime(me.shinsunyoung.projectweatherly.common.util.DateUtil.getCurrentFormattedDateTime());

        // 요약 정보
        WeatherResponseDto.WeatherSummary summary = WeatherResponseDto.WeatherSummary.builder()
                .ultraShortSummary(ultraShort.getSummary().getUltraShortSummary())
                .shortSummary(shortTerm.getSummary().getShortSummary())
                .midSummary(shortTerm.getSummary().getMidSummary())
                .build();
        response.setSummary(summary);

        return response;
    }

    /**
     * 다양한 예보 타입에 따른 날씨 정보 조회
     */
    public WeatherResponseDto getWeather(WeatherRequestDto requestDto) {
        if (requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            return getWeatherByGps(requestDto.getLatitude(), requestDto.getLongitude());
        } else if (requestDto.getRegionCode() != null) {
            return getWeatherByRegionCode(requestDto.getRegionCode());
        } else {
            throw new IllegalArgumentException("지역 코드 또는 좌표 정보가 필요합니다.");
        }
    }
}