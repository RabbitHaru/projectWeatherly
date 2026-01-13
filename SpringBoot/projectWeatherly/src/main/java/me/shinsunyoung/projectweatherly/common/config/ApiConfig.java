package me.shinsunyoung.projectweatherly.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApiConfig {

    @Value("${weatherly.api.kma.url}")
    private String kmaApiUrl;

    @Value("${weatherly.api.airkorea.url}")
    private String airKoreaApiUrl;

    @Value("${weatherly.api.kakao.url}")
    private String kakaoApiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient kmaWebClient() {
        return WebClient.builder()
                .baseUrl(kmaApiUrl)
                .build();
    }

    @Bean
    public WebClient airKoreaWebClient() {
        return WebClient.builder()
                .baseUrl(airKoreaApiUrl)
                .build();
    }

    @Bean
    public WebClient kakaoWebClient(@Value("${api.kakao.key}") String kakaoApiKey) {
        return WebClient.builder()
                .baseUrl(kakaoApiUrl)
                .defaultHeader("Authorization", "KakaoAK " + kakaoApiKey)  // 변수 사용 수정
                .build();
    }
}