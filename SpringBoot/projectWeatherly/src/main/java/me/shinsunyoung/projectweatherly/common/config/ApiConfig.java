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

    @Value("${weatherly.api.ipinfo.url}")
    private String ipInfoUrl;

    @Value("${api.kakao.key}")
    private String kakaoApiKey;

    @Value("${api.kma.key}")
    private String kmaApiKey;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    public String getKmaApiKey() {
        return kmaApiKey;
    }

    public String getKakaoApiKey() {
        return kakaoApiKey;
    }

    public String getKmaApiUrl() {
        return kmaApiUrl;
    }

    public String getAirKoreaApiUrl() {
        return airKoreaApiUrl;
    }

    public String getKakaoApiUrl() {
        return kakaoApiUrl;
    }

    public String getIpInfoUrl() {
        return ipInfoUrl;
    }
}