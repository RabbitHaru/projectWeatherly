package me.shinsunyoung.projectweatherly.airquality.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityRequestDTO;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/air-quality")
@RequiredArgsConstructor
@Slf4j
public class AirQualityController {

    private final AirQualityService airQualityService;

    @GetMapping("/current")
    public ApiResponse<AirQualityResponseDTO> getCurrentAirQuality(HttpServletRequest request) {
        try {
            AirQualityResponseDTO airQuality = airQualityService.getAirQualityByIp(request);
            return ApiResponse.success(airQuality);
        } catch (Exception e) {
            log.error("현재 위치 대기질 조회 실패", e);
            // 에러 시 null을 담아 성공으로 처리 (프론트에서 처리)
            return ApiResponse.success(null);
        }
    }

    @PostMapping("/gps")
    public ApiResponse<AirQualityResponseDTO> getAirQualityByGps(@RequestParam Double latitude, @RequestParam Double longitude) {
        try {
            AirQualityResponseDTO airQuality = airQualityService.getAirQualityByGps(latitude, longitude);
            return ApiResponse.success("GPS 위치 기반 대기질 정보", airQuality);
        } catch (Exception e) {
            log.error("GPS 대기질 조회 실패", e);
            return ApiResponse.error("GPS 대기질 정보를 가져오는데 실패했습니다.");
        }
    }

    @GetMapping("/forecast/{sidoName}")
    public ApiResponse<List<AirQualityResponseDTO.AirQualityForecast>> getAirQualityForecast(@PathVariable String sidoName) {
        List<AirQualityResponseDTO.AirQualityForecast> forecasts = airQualityService.getAirQualityForecast(sidoName);
        return ApiResponse.success(forecasts);
    }

    @GetMapping("/compare")
    public ApiResponse<List<AirQualityResponseDTO>> compareRegionalAirQuality(@RequestParam List<String> sidoNames) {
        List<AirQualityResponseDTO> results = new ArrayList<>();

        // [수정] try-catch를 for문 안으로 넣어서 하나가 에러나도 멈추지 않게 함
        for (String sidoName : sidoNames) {
            try {
                List<AirQualityResponseDTO> sidoData = airQualityService.getAirQualityBySido(sidoName);
                if (!sidoData.isEmpty()) {
                    results.add(sidoData.get(0));
                }
            } catch (Exception e) {
                // 특정 지역 조회 실패 시 로그만 남기고 계속 진행
                log.warn("지역별 비교 조회 실패 (지역: {}): {}", sidoName, e.getMessage());
            }
        }

        return ApiResponse.success(results);
    }

    @GetMapping("/sido/{sidoName}")
    public ApiResponse<List<AirQualityResponseDTO>> getAirQualityBySido(@PathVariable String sidoName) {
        return ApiResponse.success(airQualityService.getAirQualityBySido(sidoName));
    }

    @GetMapping("/station/{stationName}")
    public ApiResponse<AirQualityResponseDTO> getAirQualityByStation(@PathVariable String stationName) {
        return ApiResponse.success(airQualityService.getAirQualityByStation(stationName));
    }

    @PostMapping("/search")
    public ApiResponse<AirQualityResponseDTO> searchAirQuality(@RequestBody AirQualityRequestDTO requestDto) {
        return ApiResponse.success(airQualityService.getAirQuality(requestDto));
    }

    @GetMapping("/health-check")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("OK");
    }
}