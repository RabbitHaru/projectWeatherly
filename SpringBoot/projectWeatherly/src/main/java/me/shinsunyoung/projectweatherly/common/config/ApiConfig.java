package me.shinsunyoung.projectweatherly.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ApiConfig {

    @Value("${weatherly.api.kma.url}")
    private String kmaApiUrl;

    @Value("${weatherly.api.airkorea.url}")
    private String airKoreaApiUrl;

    @Value("${weatherly.api.kakao.url}")
    private String kakaoApiUrl;

    @Value("${api.kakao.key}")
    private String kakaoApiKey;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초
        factory.setReadTimeout(10000);   // 10초
        return new RestTemplate(factory);
    }

    // WebClient 빈들은 제거하거나 필요시만 유지
}