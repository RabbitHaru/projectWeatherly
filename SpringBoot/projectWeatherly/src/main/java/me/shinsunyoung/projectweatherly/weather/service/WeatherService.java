package me.shinsunyoung.projectweatherly.weather.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherRequestDTO;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final LocationService locationService;
    private final RestTemplate restTemplate;

    // 지역별 요청 횟수 제한 캐시
    private static final ConcurrentHashMap<String, Long> REQUEST_RATE_LIMIT = new ConcurrentHashMap<>();
    private static final long REQUEST_COOLDOWN_MS = 10000; // 10초

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
        return getWeatherByRegionCode(regionCode, false);
    }

    public WeatherResponseDTO getWeatherByRegionCode(String regionCode, boolean liteMode) {
        try {
            // 요청 빈도 제한 체크
            if (shouldRateLimit(regionCode)) {
                log.warn("요청 빈도 제한: {}", regionCode);
                return createFallbackWeatherData(regionCode, liteMode);
            }

            // 모든 데이터를 한 번에 가져오기 (캐시 적용)
            WeatherResponseDTO weatherData = weatherApiService.getAllWeatherData(regionCode, liteMode);

            // 요청 기록 업데이트
            updateRequestTime(regionCode);

            return weatherData;

        } catch (Exception e) {
            log.error("날씨 정보 조회 실패: {}", e.getMessage());
            return createFallbackWeatherData(regionCode, liteMode);
        }
    }

    public WeatherResponseDTO getWeatherByRegionCodeLite(String regionCode) {
        return getWeatherByRegionCode(regionCode, true);
    }

    public WeatherResponseDTO getWeather(WeatherRequestDTO requestDto) {
        if (requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            return getWeatherByGps(requestDto.getLatitude(), requestDto.getLongitude());
        } else if (requestDto.getRegionCode() != null) {
            return getWeatherByRegionCode(requestDto.getRegionCode(), requestDto.isLiteMode());
        } else {
            throw new IllegalArgumentException("지역 코드 또는 좌표 정보가 필요합니다.");
        }
    }

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
        // 병렬 처리로 모든 지역 데이터 조회
        List<CompletableFuture<WeatherResponseDTO>> futures = regionCodes.stream()
                .map(code -> getWeatherByRegionCodeAsync(code, liteMode))
                .collect(Collectors.toList());

        // 모든 작업 완료 후 결과 수집
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public List<WeatherResponseDTO> compareWeatherLite(List<String> regionCodes) {
        return compareWeather(regionCodes, true);
    }

    private boolean shouldRateLimit(String regionCode) {
        Long lastRequestTime = REQUEST_RATE_LIMIT.get(regionCode);
        if (lastRequestTime == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastRequestTime) < REQUEST_COOLDOWN_MS;
    }

    private void updateRequestTime(String regionCode) {
        REQUEST_RATE_LIMIT.put(regionCode, System.currentTimeMillis());
    }

    private WeatherResponseDTO createFallbackWeatherData(String regionCode, boolean liteMode) {
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

        WeatherResponseDTO response = WeatherResponseDTO.builder()
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

        return liteMode ? WeatherResponseDTO.createLiteVersion(response) : response;
    }
}