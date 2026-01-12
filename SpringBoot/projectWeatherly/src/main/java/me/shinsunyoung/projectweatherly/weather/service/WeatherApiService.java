package me.shinsunyoung.projectweatherly.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.util.DateUtil;
import me.shinsunyoung.projectweatherly.weather.dto.WeatherResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final ObjectMapper objectMapper;

    @Value("${weatherly.api.kma.url}")
    private String kmaApiUrl;

    @Value("${api.kma.key}")
    private String apiKey;

    /**
     * 초단기예보 조회 (6시간)
     */
    @Cacheable(value = "ultraShortForecast", key = "#regionCode + '_' + #baseDate + '_' + #baseTime")
    public WeatherResponseDto getUltraShortForecast(String regionCode, String baseDate, String baseTime) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(kmaApiUrl + "/getUltraSrtFcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 60)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", getNxFromRegionCode(regionCode))
                    .queryParam("ny", getNyFromRegionCode(regionCode))
                    .build()
                    .toUri();

            // 실제 API 호출 (주석 처리됨 - 실제 구현 시 활성화)
            // String response = restTemplate.getForObject(uri, String.class);
            // JsonNode root = objectMapper.readTree(response);

            log.info("초단기예보 API 호출: {}, {}", regionCode, baseTime);

            // 임시 더미 데이터 반환
            return createDummyUltraShortForecast(regionCode);

        } catch (Exception e) {
            log.error("초단기예보 API 호출 실패", e);
            throw new RuntimeException("날씨 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 단기예보 조회 (3일)
     */
    @Cacheable(value = "shortTermForecast", key = "#regionCode")
    public WeatherResponseDto getShortTermForecast(String regionCode) {
        try {
            String baseDate = DateUtil.formatDateOnly(LocalDateTime.now());
            String baseTime = "0500"; // 기상청 기준 시간

            URI uri = UriComponentsBuilder.fromHttpUrl(kmaApiUrl + "/getVilageFcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 300)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", getNxFromRegionCode(regionCode))
                    .queryParam("ny", getNyFromRegionCode(regionCode))
                    .build()
                    .toUri();

            log.info("단기예보 API 호출: {}", regionCode);

            // 임시 더미 데이터 반환
            return createDummyShortTermForecast(regionCode);

        } catch (Exception e) {
            log.error("단기예보 API 호출 실패", e);
            throw new RuntimeException("날씨 정보를 불러올 수 없습니다.");
        }
    }

    /**
     * 현재 날씨 정보 조회
     */
    @Cacheable(value = "currentWeather", key = "#regionCode")
    public WeatherResponseDto.CurrentWeather getCurrentWeather(String regionCode) {
        // 초단기실황 API 또는 단기예보의 첫 번째 데이터 사용
        String baseDate = DateUtil.formatDateOnly(LocalDateTime.now());
        String baseTime = DateUtil.getBaseTime();

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(kmaApiUrl + "/getUltraSrtNcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 10)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", getNxFromRegionCode(regionCode))
                    .queryParam("ny", getNyFromRegionCode(regionCode))
                    .build()
                    .toUri();

            log.info("현재 날씨 API 호출: {}", regionCode);

            // 임시 더미 데이터 반환
            return createDummyCurrentWeather();

        } catch (Exception e) {
            log.error("현재 날씨 API 호출 실패", e);
            throw new RuntimeException("현재 날씨 정보를 불러올 수 없습니다.");
        }
    }

    // 더미 데이터 생성 메서드들 (실제 구현 시 삭제)
    private WeatherResponseDto createDummyUltraShortForecast(String regionCode) {
        List<WeatherResponseDto.HourlyForecast> hourly = new ArrayList<>();
        String[] times = {"현재", "15시", "16시", "17시", "18시", "19시", "20시", "21시"};
        Double[] temps = {22.0, 23.0, 24.0, 24.0, 23.0, 22.0, 21.0, 20.0};

        for (int i = 0; i < times.length; i++) {
            hourly.add(WeatherResponseDto.HourlyForecast.builder()
                    .time(times[i])
                    .temperature(temps[i])
                    .weatherCondition(i < 2 ? "맑음" : i < 4 ? "구름조금" : i < 6 ? "흐림" : "맑음")
                    .weatherIcon(i < 2 ? "fas fa-sun" : i < 4 ? "fas fa-cloud-sun" : i < 6 ? "fas fa-cloud" : "fas fa-moon")
                    .precipitationProbability(0.0)
                    .humidity(45.0 + i)
                    .build());
        }

        return WeatherResponseDto.builder()
                .regionName("서울특별시")
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .current(createDummyCurrentWeather())
                .hourly(hourly)
                .summary(WeatherResponseDto.WeatherSummary.builder()
                        .ultraShortSummary("현재부터 6시간 후까지 맑은 날씨가 이어집니다.")
                        .build())
                .build();
    }

    private WeatherResponseDto createDummyShortTermForecast(String regionCode) {
        List<WeatherResponseDto.DailyForecast> daily = new ArrayList<>();
        String[] days = {"금", "토", "일", "월", "화", "수"};
        Double[] maxTemps = {26.0, 25.0, 24.0, 22.0, 23.0, 24.0};
        Double[] minTemps = {18.0, 17.0, 16.0, 15.0, 16.0, 17.0};
        String[] weathers = {"맑음", "구름조금", "흐림", "비", "비", "구름조금"};

        for (int i = 0; i < days.length; i++) {
            daily.add(WeatherResponseDto.DailyForecast.builder()
                    .date(LocalDate.now().plusDays(i).format(DateTimeFormatter.ofPattern("MM/dd")))
                    .dayOfWeek(days[i])
                    .maxTemp(maxTemps[i])
                    .minTemp(minTemps[i])
                    .dayWeather(weathers[i])
                    .nightWeather(weathers[i])
                    .dayIcon(getWeatherIcon(weathers[i], true))
                    .nightIcon(getWeatherIcon(weathers[i], false))
                    .precipitationProbability(i == 3 || i == 4 ? 70.0 : 20.0)
                    .build());
        }

        return WeatherResponseDto.builder()
                .regionName("서울특별시")
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .daily(daily)
                .summary(WeatherResponseDto.WeatherSummary.builder()
                        .shortSummary("금요일 맑음 → 토요일 구름 조금 → 일요일 흐림")
                        .midSummary("월요일 비 예상 후, 점차 개면서 기온 상승")
                        .build())
                .build();
    }

    private WeatherResponseDto.CurrentWeather createDummyCurrentWeather() {
        return WeatherResponseDto.CurrentWeather.builder()
                .temperature(22.0)
                .feelsLike(23.0)
                .humidity(45.0)
                .windSpeed(2.5)
                .windDirection("남서풍")
                .precipitation(0.0)
                .weatherCondition("맑음")
                .weatherIcon("fas fa-sun")
                .updateTime(LocalDateTime.now())
                .build();
    }

    private String getWeatherIcon(String condition, boolean isDay) {
        return switch (condition) {
            case "맑음" -> isDay ? "fas fa-sun" : "fas fa-moon";
            case "구름조금" -> "fas fa-cloud-sun";
            case "흐림" -> "fas fa-cloud";
            case "비" -> "fas fa-cloud-rain";
            case "눈" -> "fas fa-snowflake";
            default -> isDay ? "fas fa-sun" : "fas fa-moon";
        };
    }

    private String getNxFromRegionCode(String regionCode) {
        // 지역코드 → 격자 좌표 변환 (간단한 예시)
        return switch (regionCode) {
            case "1100000000" -> "60";  // 서울
            case "2600000000" -> "98";  // 부산
            case "2800000000" -> "55";  // 인천
            default -> "60";
        };
    }

    private String getNyFromRegionCode(String regionCode) {
        return switch (regionCode) {
            case "1100000000" -> "127";  // 서울
            case "2600000000" -> "76";   // 부산
            case "2800000000" -> "124";  // 인천
            default -> "127";
        };
    }
}