package me.shinsunyoung.projectweatherly.weather.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherRequestDTO;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final LocationService locationService;
    private final RestTemplate restTemplate;

    public WeatherResponseDTO getWeatherByIp(HttpServletRequest request) {
        String clientIp = locationService.getClientIp(request);
        LocationDTO location = locationService.getLocationByIp(clientIp);

        log.info("IP 기반 날씨 조회: {} -> {}", clientIp, location.getRegionName());

        return getWeatherByRegionCode(location.getRegionCode());
    }

    public WeatherResponseDTO getWeatherByGps(Double latitude, Double longitude) {
        LocationDTO location = locationService.getLocationByGps(latitude, longitude);

        log.info("GPS 기반 날씨 조회: ({}, {}) -> {}",
                latitude, longitude, location.getRegionName());

        return getWeatherByRegionCode(location.getRegionCode());
    }

    public WeatherResponseDTO getWeatherByRegionCode(String regionCode) {
        try {
            // 모든 데이터를 한 번에 가져오기
            WeatherResponseDTO weatherData = weatherApiService.getAllWeatherData(regionCode);

            return weatherData;

        } catch (Exception e) {
            log.error("날씨 정보 조회 실패: {}", e.getMessage());
            return createFallbackWeatherData(regionCode);
        }
    }

    public WeatherResponseDTO getWeather(WeatherRequestDTO requestDto) {
        if (requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            return getWeatherByGps(requestDto.getLatitude(), requestDto.getLongitude());
        } else if (requestDto.getRegionCode() != null) {
            return getWeatherByRegionCode(requestDto.getRegionCode());
        } else {
            throw new IllegalArgumentException("지역 코드 또는 좌표 정보가 필요합니다.");
        }
    }

    private WeatherResponseDTO createFallbackWeatherData(String regionCode) {
        String regionName = switch (regionCode) {
            case "1100000000" -> "서울특별시";
            case "2600000000" -> "부산광역시";
            case "2800000000" -> "인천광역시";
            case "2700000000" -> "대구광역시";
            case "3000000000" -> "대전광역시";
            case "2900000000" -> "광주광역시";
            case "3100000000" -> "울산광역시";
            default -> "서울특별시";
        };

        return WeatherResponseDTO.builder()
                .regionName(regionName)
                .regionCode(regionCode)
                .currentTime(java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일 HH:mm")))
                .current(WeatherResponseDTO.CurrentWeather.builder()
                        .temperature(22.0)
                        .feelsLike(23.0)
                        .humidity(45.0)
                        .windSpeed(2.5)
                        .windDirection("남서풍")
                        .precipitation(0.0)
                        .weatherCondition("맑음")
                        .weatherIcon("fas fa-sun")
                        .updateTime(java.time.LocalDateTime.now())
                        .build())
                .summary(WeatherResponseDTO.WeatherSummary.builder()
                        .ultraShortSummary("데이터를 불러오는 중입니다.")
                        .shortSummary("기본 날씨 정보를 표시합니다.")
                        .midSummary("API 연결을 확인해주세요.")
                        .build())
                .build();
    }
}