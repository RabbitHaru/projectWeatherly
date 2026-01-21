package me.shinsunyoung.projectweatherly.weather.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final LocationService locationService;
    // [중요] Repository 제거됨

    public WeatherResponseDTO getWeatherByIp(HttpServletRequest request) {
        String clientIp = locationService.getClientIp(request);
        LocationDTO location = locationService.getLocationByIp(clientIp);
        log.info("IP 기반 날씨 조회: {} -> {}", clientIp, location.getRegionName());
        return getWeatherByRegionCode(location.getRegionCode());
    }

    public WeatherResponseDTO getWeatherByGps(Double latitude, Double longitude) {
        LocationDTO location = locationService.getLocationByGps(latitude, longitude);
        log.info("GPS 기반 날씨 조회: ({}, {}) -> {}", latitude, longitude, location.getRegionName());
        return getWeatherByRegionCode(location.getRegionCode());
    }

    public WeatherResponseDTO getWeatherByRegionCode(String regionCode) {
        return getWeatherByRegionCode(regionCode, false);
    }

    public WeatherResponseDTO getWeatherByRegionCode(String regionCode, boolean liteMode) {
        try {
            // API 호출 (성공 시 정상 데이터 반환)
            return weatherApiService.getAllWeatherData(regionCode, liteMode);
        } catch (Exception e) {
            // 실패 시 로그 출력 후 더미 데이터 반환
            log.error("날씨 정보 조회 실패: {}", e.getMessage());
            return createFallbackWeatherData(regionCode, liteMode);
        }
    }

    public WeatherResponseDTO getWeatherByRegionCodeLite(String regionCode) {
        return getWeatherByRegionCode(regionCode, true);
    }

    // [중요] getWeather(RequestDTO) 메서드 제거됨

    @Async
    public CompletableFuture<WeatherResponseDTO> getWeatherByRegionCodeAsync(String regionCode) {
        return CompletableFuture.supplyAsync(() -> getWeatherByRegionCode(regionCode));
    }

    @Async
    public CompletableFuture<WeatherResponseDTO> getWeatherByRegionCodeAsync(String regionCode, boolean liteMode) {
        return CompletableFuture.supplyAsync(() -> getWeatherByRegionCode(regionCode, liteMode));
    }

    public List<WeatherResponseDTO> compareWeather(List<String> regionCodes) {
        return compareWeather(regionCodes, false);
    }

    public List<WeatherResponseDTO> compareWeather(List<String> regionCodes, boolean liteMode) {
        List<CompletableFuture<WeatherResponseDTO>> futures = regionCodes.stream()
                .map(code -> getWeatherByRegionCodeAsync(code, liteMode))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public List<WeatherResponseDTO> compareWeatherLite(List<String> regionCodes) {
        return compareWeather(regionCodes, true);
    }

    // 더미 데이터 생성 메서드 (API 에러 시 사용)
    private WeatherResponseDTO createFallbackWeatherData(String regionCode, boolean liteMode) {
        String regionName = switch (regionCode) {
            case "1100000000" -> "서울특별시";
            case "2600000000" -> "부산광역시";
            default -> "대한민국";
        };

        WeatherResponseDTO response = WeatherResponseDTO.builder()
                .regionName(regionName)
                .regionCode(regionCode)
                .currentTime(java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일 HH:mm")))
                .current(WeatherResponseDTO.CurrentWeather.builder()
                        .temperature(20.0)
                        .feelsLike(20.0)
                        .humidity(50.0)
                        .windSpeed(2.0)
                        .windDirection("북서풍")
                        .precipitation(0.0)
                        .weatherCondition("맑음")
                        .weatherIcon("fas fa-sun")
                        .updateTime(java.time.LocalDateTime.now())
                        .build())
                .summary(WeatherResponseDTO.WeatherSummary.builder()
                        .ultraShortSummary("데이터를 불러오는 중입니다.")
                        .shortSummary("잠시 후 다시 시도해주세요.")
                        .midSummary("API 연결 상태를 확인하세요.")
                        .build())
                .build();

        return liteMode ? WeatherResponseDTO.createLiteVersion(response) : response;
    }
}