package me.shinsunyoung.projectweatherly.airquality.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.dto.AirQualityResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityApiService {

    private final ObjectMapper objectMapper;

    @Value("${weatherly.api.airkorea.url}")
    private String airKoreaApiUrl;

    @Value("${api.airkorea.key}")
    private String apiKey;

    /**
     * 시도별 실시간 측정정보 조회
     */
    @Cacheable(value = "airQualityBySido", key = "#sidoName")
    public List<AirQualityResponseDto> getAirQualityBySido(String sidoName) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getCtprvnRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", 100)
                    .queryParam("pageNo", 1)
                    .queryParam("sidoName", sidoName)
                    .queryParam("ver", "1.3")
                    .build()
                    .toUri();

            log.info("대기질 API 호출: {}", sidoName);

            // 임시 더미 데이터 반환
            return createDummyAirQualityData(sidoName);

        } catch (Exception e) {
            log.error("대기질 API 호출 실패", e);
            throw new RuntimeException("대기질 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 측정소별 실시간 측정정보 조회
     */
    @Cacheable(value = "airQualityByStation", key = "#stationName")
    public AirQualityResponseDto getAirQualityByStation(String stationName) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMsrstnAcctoRltmMesureDnsty")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("stationName", stationName)
                    .queryParam("dataTerm", "DAILY")
                    .queryParam("ver", "1.3")
                    .build()
                    .toUri();

            log.info("측정소별 대기질 API 호출: {}", stationName);

            // 임시 더미 데이터 반환
            return createDummyStationAirQuality(stationName);

        } catch (Exception e) {
            log.error("측정소별 대기질 API 호출 실패", e);
            throw new RuntimeException("대기질 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 근접 측정소 조회 (TM 좌표 기준)
     */
    @Cacheable(value = "nearbyStations", key = "#tmX + '_' + #tmY")
    public String getNearbyStation(Double tmX, Double tmY) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getNearbyMsrstnList")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("tmX", tmX)
                    .queryParam("tmY", tmY)
                    .build()
                    .toUri();

            // 실제 API 호출 생략

            // 임시 더미 데이터
            return "중구";

        } catch (Exception e) {
            log.error("근접 측정소 API 호출 실패", e);
            return "중구"; // 기본값
        }
    }

    /**
     * 대기질 예보 정보 조회
     */
    @Cacheable(value = "airQualityForecast", key = "#sidoName")
    public List<AirQualityResponseDto.AirQualityForecast> getAirQualityForecast(String sidoName) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(airKoreaApiUrl + "/getMinuDustFrcstDspth")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("returnType", "json")
                    .queryParam("searchDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .queryParam("InformCode", "PM10")
                    .build()
                    .toUri();

            log.info("대기질 예보 API 호출: {}", sidoName);

            // 임시 더미 데이터 반환
            return createDummyForecastData();

        } catch (Exception e) {
            log.error("대기질 예보 API 호출 실패", e);
            throw new RuntimeException("대기질 예보 정보를 불러올 수 없습니다.");
        }
    }

    // 더미 데이터 생성 메서드들
    private List<AirQualityResponseDto> createDummyAirQualityData(String sidoName) {
        List<AirQualityResponseDto> list = new ArrayList<>();

        String[] stations = {"중구", "강남구", "서초구", "종로구", "마포구"};

        for (String station : stations) {
            list.add(createDummyStationAirQuality(station));
        }

        return list;
    }

    private AirQualityResponseDto createDummyStationAirQuality(String stationName) {
        return AirQualityResponseDto.builder()
                .stationName(stationName)
                .sidoName("서울")
                .dataTime(LocalDateTime.now())
                .khai(createAirQualityIndex(65, "2", "보통", ""))
                .pm10(createAirQualityIndex(35, "1", "좋음", "㎍/㎥"))
                .pm25(createAirQualityIndex(15, "1", "좋음", "㎍/㎥"))
                .o3(createAirQualityIndex(0.025, "1", "좋음", "ppm"))
                .no2(createAirQualityIndex(0.015, "1", "좋음", "ppm"))
                .co(createAirQualityIndex(0.5, "1", "좋음", "ppm"))
                .so2(createAirQualityIndex(0.004, "1", "좋음", "ppm"))
                .overallGrade("2")
                .overallStatus("보통")
                .healthAdvice("대기질이 양호합니다. 실외 활동에 문제 없습니다.")
                .forecasts(createDummyForecastData())
                .build();
    }

    private AirQualityResponseDto.AirQualityIndex createAirQualityIndex(Number value, String grade, String status, String unit) {
        return AirQualityResponseDto.AirQualityIndex.builder()
                .value(value.intValue())
                .grade(grade)
                .status(status)
                .unit(unit)
                .build();
    }

    private List<AirQualityResponseDto.AirQualityForecast> createDummyForecastData() {
        List<AirQualityResponseDto.AirQualityForecast> forecasts = new ArrayList<>();

        String[] dates = {"오늘", "내일", "모레"};
        String[] overallGrades = {"1", "2", "2"};
        String[] pm10Grades = {"1", "2", "2"};
        String[] pm25Grades = {"1", "2", "3"};
        String[] advices = {
                "대기질이 양호합니다.",
                "미세먼지가 조금 있으니 민감한 분들은 주의하세요.",
                "초미세먼지가 많으니 실외활동을 자제하세요."
        };

        for (int i = 0; i < dates.length; i++) {
            forecasts.add(AirQualityResponseDto.AirQualityForecast.builder()
                    .date(dates[i])
                    .overallGrade(overallGrades[i])
                    .pm10Grade(pm10Grades[i])
                    .pm25Grade(pm25Grades[i])
                    .advice(advices[i])
                    .build());
        }

        return forecasts;
    }
}