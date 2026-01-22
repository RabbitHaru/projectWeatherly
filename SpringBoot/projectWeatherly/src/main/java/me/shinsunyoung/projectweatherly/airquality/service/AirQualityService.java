package me.shinsunyoung.projectweatherly.airquality.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDTO;
import me.shinsunyoung.projectweatherly.airquality.entity.AirQualityEntity;
import me.shinsunyoung.projectweatherly.airquality.entity.AirQualityForecastEntity;
import me.shinsunyoung.projectweatherly.airquality.repository.AirQualityForecastRepository;
import me.shinsunyoung.projectweatherly.airquality.repository.AirQualityRepository;
import me.shinsunyoung.projectweatherly.common.dto.LocationDTO;
import me.shinsunyoung.projectweatherly.common.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private final AirQualityApiService airQualityApiService;
    private final LocationService locationService;
    private final AirQualityRepository airQualityRepository;
    private final AirQualityForecastRepository airQualityForecastRepository;

    @Transactional(readOnly = true)
    public boolean isRealtimeDataFresh() {
        List<AirQualityEntity> list = airQualityRepository.findBySidoNameOrderByDataTimeDesc("서울");
        if (list.isEmpty()) return false;
        AirQualityEntity latest = list.get(0);
        return latest.getRecordedAt() != null && latest.getRecordedAt().isAfter(LocalDateTime.now().minusHours(1));
    }

    @Transactional
    public void updateRealtimeData() {
        try {
            List<AirQualityResponseDTO> dtos = airQualityApiService.getAirQualityBySido("전국");
            if (dtos == null || dtos.isEmpty()) return;

            String advice = dtos.get(0).getHealthAdvice();
            if (advice != null && advice.contains("[테스트 모드]")) return;

            List<AirQualityEntity> entities = dtos.stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());

            airQualityRepository.saveAll(entities);
            log.info("✅ 전국 실시간 대기질 데이터 {}건 DB 저장 완료", entities.size());
        } catch (Exception e) {
            log.error("전국 데이터 업데이트 실패: {}", e.getMessage());
        }
    }

    // ⭐ [핵심 변경] 중복 데이터는 아예 저장하지 않고 건너뜀 (Skip)
    @Transactional
    public void updateForecastData() {
        List<AirQualityResponseDTO.AirQualityForecast> forecasts = airQualityApiService.getAirQualityForecast("서울");
        if (forecasts == null || forecasts.isEmpty()) return;

        String advice = forecasts.get(0).getAdvice();
        if (advice != null && advice.contains("[가상 예보]")) return;

        int savedCount = 0;
        for (AirQualityResponseDTO.AirQualityForecast dto : forecasts) {
            // 1. 중복 확인 (시간 기준)
            boolean exists = airQualityForecastRepository.findByDataTime(dto.getDataTime()).isPresent();

            if (exists) {
                // 2-A. 이미 있으면 -> 저장 안 하고 통과 (Skip)
                log.info("♻️ 중복된 예보 데이터 발견 (시간: {}) -> 저장 건너뜀", dto.getDataTime());
                continue;
            }

            // 2-B. 없으면 -> 새로 저장 (Insert)
            airQualityForecastRepository.save(AirQualityForecastEntity.builder()
                    .dataTime(dto.getDataTime())
                    .informData(dto.getDate())
                    .informCode(dto.getInformCode())
                    .informOverall(dto.getAdvice())
                    .informCause(dto.getCause())
                    .informGrade(dto.getOverallGrade())
                    .build());
            savedCount++;
        }

        if (savedCount > 0) {
            log.info("✅ 새로운 대기질 예보 {}건 저장 완료", savedCount);
        } else {
            log.info("👌 새로운 예보 데이터 없음 (모두 중복)");
        }
    }

    public AirQualityResponseDTO getAirQualityByIp(HttpServletRequest request) {
        String clientIp = locationService.getClientIp(request);
        String regionName = "서울특별시";
        if (!clientIp.equals("127.0.0.1") && !clientIp.equals("0:0:0:0:0:0:0:1")) {
            LocationDTO location = locationService.getLocationByIp(clientIp);
            if (location != null && location.getRegionName() != null) regionName = location.getRegionName();
        }
        return getLatestDataByRegion(regionName);
    }

    public AirQualityResponseDTO getAirQualityByGps(Double lat, Double lon) {
        LocationDTO location = locationService.getLocationByGps(lat, lon);
        String regionName = (location != null) ? location.getRegionName() : "서울특별시";
        return getLatestDataByRegion(regionName);
    }

    public List<AirQualityResponseDTO> getAirQualityBySido(String sidoName) {
        String shortName = extractSidoName(sidoName);
        List<AirQualityEntity> entities = airQualityRepository.findBySidoNameOrderByDataTimeDesc(shortName);
        if (!entities.isEmpty())
            return entities.stream().limit(100).map(this::convertToDTO).collect(Collectors.toList());

        return airQualityApiService.getAirQualityBySido(shortName);
    }

    public List<AirQualityResponseDTO.AirQualityForecast> getAirQualityForecast(String sidoName) {
        List<AirQualityForecastEntity> entities = airQualityForecastRepository.findTop2ByOrderByRecordedAtDesc();
        String targetSido = extractSidoName(sidoName);

        if (!entities.isEmpty()) {
            return entities.stream().map(e -> {
                String parsedGrade = parseGradeForSido(e.getInformGrade(), targetSido);
                return AirQualityResponseDTO.AirQualityForecast.builder()
                        .date(e.getInformData())
                        .advice(e.getInformOverall())
                        .cause(e.getInformCause())
                        .overallGrade(convertTextToGrade(parsedGrade))
                        .dataTime(e.getDataTime())
                        .informCode(e.getInformCode())
                        .build();
            }).collect(Collectors.toList());
        }
        return airQualityApiService.getAirQualityForecast(sidoName);
    }

    // 헬퍼 메서드들
    private AirQualityResponseDTO getLatestDataByRegion(String regionName) {
        String shortName = extractSidoName(regionName);
        List<AirQualityEntity> list = airQualityRepository.findBySidoNameOrderByDataTimeDesc(shortName);

        if (!list.isEmpty()) {
            return list.stream()
                    .filter(e -> e.getKhaiValue() != null && e.getKhaiValue() > 0)
                    .findFirst()
                    .map(this::convertToDTO)
                    .orElseGet(() -> convertToDTO(list.get(0)));
        }

        List<AirQualityResponseDTO> apiResult = airQualityApiService.getAirQualityBySido(shortName);
        if (apiResult != null && !apiResult.isEmpty()) return apiResult.get(0);
        return null;
    }

    public AirQualityResponseDTO getAirQualityByStation(String stationName) {
        return airQualityApiService.getAirQualityByStation(stationName);
    }

    private String parseGradeForSido(String allGrades, String sido) {
        if (allGrades == null) return "보통";
        String[] parts = allGrades.split(",");
        for (String part : parts) if (part.contains(sido)) return part.substring(part.lastIndexOf(":") + 1).trim();
        return "보통";
    }

    private String convertTextToGrade(String text) {
        if (text == null) return "2";
        if (text.contains("좋음")) return "1";
        if (text.contains("보통")) return "2";
        if (text.contains("나쁨")) return "3";
        if (text.contains("매우나쁨")) return "4";
        return "2";
    }

    private AirQualityEntity convertToEntity(AirQualityResponseDTO dto) {
        return AirQualityEntity.builder()
                .sidoName(dto.getSidoName()).stationName(dto.getStationName())
                .dataTime((LocalDateTime) dto.getDataTime())
                .pm10Value(dto.getPm10().getValue()).pm10Grade(dto.getPm10().getGrade())
                .pm25Value(dto.getPm25().getValue()).pm25Grade(dto.getPm25().getGrade())
                .o3Value(dto.getO3() != null ? (double) dto.getO3().getValue() : 0.0).o3Grade(dto.getO3() != null ? dto.getO3().getGrade() : "2")
                .no2Value(dto.getNo2() != null ? (double) dto.getNo2().getValue() : 0.0).no2Grade(dto.getNo2() != null ? dto.getNo2().getGrade() : "2")
                .coValue(dto.getCo() != null ? (double) dto.getCo().getValue() : 0.0).coGrade(dto.getCo() != null ? dto.getCo().getGrade() : "2")
                .so2Value(dto.getSo2() != null ? (double) dto.getSo2().getValue() : 0.0).so2Grade(dto.getSo2() != null ? dto.getSo2().getGrade() : "2")
                .khaiValue(dto.getKhai() != null ? dto.getKhai().getValue() : 0).khaiGrade(dto.getKhai() != null ? dto.getKhai().getGrade() : "2")
                .build();
    }

    private AirQualityResponseDTO convertToDTO(AirQualityEntity entity) {
        return AirQualityResponseDTO.builder()
                .sidoName(entity.getSidoName()).stationName(entity.getStationName()).dataTime(entity.getDataTime())
                .pm10(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getPm10Value()).grade(entity.getPm10Grade()).status(convertGradeToStatus(entity.getPm10Grade())).unit("µg/m³").build())
                .pm25(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getPm25Value()).grade(entity.getPm25Grade()).status(convertGradeToStatus(entity.getPm25Grade())).unit("µg/m³").build())
                .o3(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getO3Value() != null ? entity.getO3Value().intValue() : 0).grade(entity.getO3Grade()).status(convertGradeToStatus(entity.getO3Grade())).unit("ppm").build())
                .no2(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getNo2Value() != null ? entity.getNo2Value().intValue() : 0).grade(entity.getNo2Grade()).status(convertGradeToStatus(entity.getNo2Grade())).unit("ppm").build())
                .co(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getCoValue() != null ? entity.getCoValue().intValue() : 0).grade(entity.getCoGrade()).status(convertGradeToStatus(entity.getCoGrade())).unit("ppm").build())
                .so2(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getSo2Value() != null ? entity.getSo2Value().intValue() : 0).grade(entity.getSo2Grade()).status(convertGradeToStatus(entity.getSo2Grade())).unit("ppm").build())
                .khai(AirQualityResponseDTO.AirQualityIndex.builder().value(entity.getKhaiValue()).grade(entity.getKhaiGrade()).status(convertGradeToStatus(entity.getKhaiGrade())).unit("점").build())
                .overallGrade(entity.getPm10Grade()).overallStatus(convertGradeToStatus(entity.getPm10Grade()))
                .healthAdvice(generateHealthAdvice(entity.getPm10Grade()))
                .build();
    }

    private String generateHealthAdvice(String grade) {
        if (grade == null) return "대기질 정보를 확인해주세요.";
        return switch (grade.trim()) {
            case "1" -> "대기질이 상쾌합니다! 환기하기 좋아요.";
            case "2" -> "대기질이 무난합니다. 평범한 하루네요.";
            case "3" -> "공기가 탁해요. 마스크를 챙기세요.";
            case "4" -> "매우 나쁩니다! 가급적 외출을 삼가세요.";
            default -> "대기질 정보를 확인해주세요.";
        };
    }

    private String convertGradeToStatus(String grade) {
        if (grade == null) return "보통";
        return switch (grade.trim()) {
            case "1" -> "좋음";
            case "2" -> "보통";
            case "3" -> "나쁨";
            case "4" -> "매우나쁨";
            default -> "보통";
        };
    }

    private String extractSidoName(String regionName) {
        if (regionName == null) return "서울";
        Map<String, String> map = new HashMap<>();
        map.put("Seoul", "서울");
        map.put("Jeju", "제주");
        map.put("Dokdo", "경북");
        map.put("서울특별시", "서울");
        for (String key : map.keySet()) if (regionName.contains(key)) return map.get(key);
        return (regionName.length() >= 2) ? regionName.substring(0, 2) : regionName;
    }
}