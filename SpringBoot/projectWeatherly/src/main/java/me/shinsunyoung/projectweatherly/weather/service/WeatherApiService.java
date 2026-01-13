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
import org.springframework.web.reactive.function.client.WebClient;
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
    private final WebClient kmaWebClient;

    @Value("${api.kma.key}")
    private String apiKey;

    /**
     * 초단기예보 조회 (6시간)
     */
    @Cacheable(value = "ultraShortForecast", key = "#regionCode + '_' + #baseDate + '_' + #baseTime")
    public WeatherResponseDto getUltraShortForecast(String regionCode, String baseDate, String baseTime) {
        try {
            // API 호출 URL 생성
            String url = UriComponentsBuilder.newInstance()
                    .path("/getUltraSrtFcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 60)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", getNxFromRegionCode(regionCode))
                    .queryParam("ny", getNyFromRegionCode(regionCode))
                    .toUriString();

            log.info("초단기예보 API 호출: {}, {}, {}", regionCode, baseDate, baseTime);

            // WebClient를 사용한 실제 API 호출 시도
            try {
                JsonNode response = kmaWebClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                // 실제 API 응답 파싱 (여기서는 간단히 로그만 출력)
                log.info("API 응답 수신: {}", response != null ? "성공" : "실패");

                // 실제 구현에서는 여기서 response 파싱하여 WeatherResponseDto 생성
                // return parseUltraShortResponse(response, regionCode);

            } catch (Exception apiError) {
                log.warn("API 호출 실패, 더미 데이터 사용: {}", apiError.getMessage());
            }

            // 임시 더미 데이터 반환 (실제 API 연결 시 주석 해제하고 위의 파싱 코드 활성화)
            return createDummyUltraShortForecast(regionCode);

        } catch (Exception e) {
            log.error("초단기예보 처리 중 오류", e);
            // 에러 발생 시 기본 더미 데이터 반환
            return createFallbackUltraShortForecast(regionCode);
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

            // API 호출 URL 생성
            String url = UriComponentsBuilder.newInstance()
                    .path("/getVilageFcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 300)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", getNxFromRegionCode(regionCode))
                    .queryParam("ny", getNyFromRegionCode(regionCode))
                    .toUriString();

            log.info("단기예보 API 호출: {}", regionCode);

            // WebClient를 사용한 실제 API 호출 시도
            try {
                JsonNode response = kmaWebClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                log.info("단기예보 API 응답 수신: {}", response != null ? "성공" : "실패");

                // 실제 구현에서는 여기서 response 파싱
                // return parseShortTermResponse(response, regionCode);

            } catch (Exception apiError) {
                log.warn("단기예보 API 호출 실패, 더미 데이터 사용: {}", apiError.getMessage());
            }

            // 임시 더미 데이터 반환
            return createDummyShortTermForecast(regionCode);

        } catch (Exception e) {
            log.error("단기예보 처리 중 오류", e);
            return createFallbackShortTermForecast(regionCode);
        }
    }

    /**
     * 현재 날씨 정보 조회
     */
    @Cacheable(value = "currentWeather", key = "#regionCode")
    public WeatherResponseDto.CurrentWeather getCurrentWeather(String regionCode) {
        try {
            String baseDate = DateUtil.formatDateOnly(LocalDateTime.now());
            String baseTime = DateUtil.getBaseTime();

            // API 호출 URL 생성
            String url = UriComponentsBuilder.newInstance()
                    .path("/getUltraSrtNcst")
                    .queryParam("serviceKey", apiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 10)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", getNxFromRegionCode(regionCode))
                    .queryParam("ny", getNyFromRegionCode(regionCode))
                    .toUriString();

            log.info("현재 날씨 API 호출: {}", regionCode);

            // WebClient를 사용한 실제 API 호출 시도
            try {
                JsonNode response = kmaWebClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                log.info("현재 날씨 API 응답 수신: {}", response != null ? "성공" : "실패");

                // 실제 구현에서는 여기서 response 파싱
                // return parseCurrentWeatherResponse(response);

            } catch (Exception apiError) {
                log.warn("현재 날씨 API 호출 실패, 더미 데이터 사용: {}", apiError.getMessage());
            }

            // 임시 더미 데이터 반환
            return createDummyCurrentWeather();

        } catch (Exception e) {
            log.error("현재 날씨 처리 중 오류", e);
            return createFallbackCurrentWeather();
        }
    }

    // ===== 더미 데이터 생성 메서드들 (API 연동 전 임시 사용) =====

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
                .regionName(getRegionNameFromCode(regionCode))
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
                .regionName(getRegionNameFromCode(regionCode))
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

    // ===== 폴백 데이터 생성 메서드들 (에러 발생 시 사용) =====

    private WeatherResponseDto createFallbackUltraShortForecast(String regionCode) {
        return WeatherResponseDto.builder()
                .regionName(getRegionNameFromCode(regionCode))
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .current(createFallbackCurrentWeather())
                .summary(WeatherResponseDto.WeatherSummary.builder()
                        .ultraShortSummary("데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.")
                        .build())
                .build();
    }

    private WeatherResponseDto createFallbackShortTermForecast(String regionCode) {
        return WeatherResponseDto.builder()
                .regionName(getRegionNameFromCode(regionCode))
                .regionCode(regionCode)
                .currentTime(DateUtil.getCurrentFormattedDateTime())
                .summary(WeatherResponseDto.WeatherSummary.builder()
                        .shortSummary("데이터를 불러오는 중입니다.")
                        .midSummary("API 연결을 확인해주세요.")
                        .build())
                .build();
    }

    private WeatherResponseDto.CurrentWeather createFallbackCurrentWeather() {
        return WeatherResponseDto.CurrentWeather.builder()
                .temperature(20.0)
                .feelsLike(21.0)
                .humidity(50.0)
                .windSpeed(1.5)
                .windDirection("북서풍")
                .precipitation(0.0)
                .weatherCondition("정보 없음")
                .weatherIcon("fas fa-question")
                .updateTime(LocalDateTime.now())
                .build();
    }

    // ===== 헬퍼 메서드들 =====

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

    private String getRegionNameFromCode(String regionCode) {
        return switch (regionCode) {
            case "1100000000" -> "서울특별시";
            case "2600000000" -> "부산광역시";
            case "2800000000" -> "인천광역시";
            case "2700000000" -> "대구광역시";
            case "3000000000" -> "대전광역시";
            case "2900000000" -> "광주광역시";
            case "3100000000" -> "울산광역시";
            case "4100000000" -> "경기도";
            case "4200000000" -> "강원도";
            case "4300000000" -> "충청북도";
            case "4400000000" -> "충청남도";
            case "4500000000" -> "전라북도";
            case "4600000000" -> "전라남도";
            case "4700000000" -> "경상북도";
            case "4800000000" -> "경상남도";
            case "5000000000" -> "제주특별자치도";
            default -> "서울특별시";
        };
    }

    private String getNxFromRegionCode(String regionCode) {
        // 지역코드 → 격자 좌표 변환 (간단한 예시)
        return switch (regionCode) {
            case "1100000000" -> "60";  // 서울
            case "2600000000" -> "98";  // 부산
            case "2800000000" -> "55";  // 인천
            case "2700000000" -> "89";  // 대구
            case "3000000000" -> "67";  // 대전
            case "2900000000" -> "58";  // 광주
            case "3100000000" -> "102"; // 울산
            case "4100000000" -> "60";  // 경기
            case "4200000000" -> "73";  // 강원
            case "4300000000" -> "69";  // 충북
            case "4400000000" -> "68";  // 충남
            case "4500000000" -> "63";  // 전북
            case "4600000000" -> "51";  // 전남
            case "4700000000" -> "89";  // 경북
            case "4800000000" -> "91";  // 경남
            case "5000000000" -> "52";  // 제주
            default -> "60";
        };
    }

    private String getNyFromRegionCode(String regionCode) {
        return switch (regionCode) {
            case "1100000000" -> "127";  // 서울
            case "2600000000" -> "76";   // 부산
            case "2800000000" -> "124";  // 인천
            case "2700000000" -> "90";   // 대구
            case "3000000000" -> "100";  // 대전
            case "2900000000" -> "74";   // 광주
            case "3100000000" -> "84";   // 울산
            case "4100000000" -> "120";  // 경기
            case "4200000000" -> "134";  // 강원
            case "4300000000" -> "107";  // 충북
            case "4400000000" -> "100";  // 충남
            case "4500000000" -> "89";   // 전북
            case "4600000000" -> "67";   // 전남
            case "4700000000" -> "91";   // 경북
            case "4800000000" -> "77";   // 경남
            case "5000000000" -> "38";   // 제주
            default -> "127";
        };
    }

    /**
     * 실제 API 응답 파싱 메서드 (템플릿 - 실제 API 키가 있을 때 활성화)
     */
    /*
    private WeatherResponseDto parseUltraShortResponse(JsonNode response, String regionCode) {
        // 실제 API 응답 파싱 로직 구현
        // response.path("response").path("body").path("items").path("item") 등으로 데이터 추출

        // 임시로 더미 데이터 반환
        return createDummyUltraShortForecast(regionCode);
    }

    private WeatherResponseDto parseShortTermResponse(JsonNode response, String regionCode) {
        // 실제 API 응답 파싱 로직 구현
        return createDummyShortTermForecast(regionCode);
    }

    private WeatherResponseDto.CurrentWeather parseCurrentWeatherResponse(JsonNode response) {
        // 실제 API 응답 파싱 로직 구현
        return createDummyCurrentWeather();
    }
    */
}