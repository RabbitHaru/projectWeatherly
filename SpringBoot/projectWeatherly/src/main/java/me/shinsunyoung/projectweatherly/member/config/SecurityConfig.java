package me.shinsunyoung.projectweatherly.member.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import me.shinsunyoung.projectweatherly.member.service.dto.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    @Lazy // 순환 참조 방지
    private final MemberService memberService;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder; // PasswordEncoderConfig에서 주입받음

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler(objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationSuccessHandler successHandler = customAuthenticationSuccessHandler();

        http
                // ... (기존 SecurityFilterChain 코드 유지)
                .userDetailsService(memberService)
        // ... (나머지 코드)
        ;

        return http.build();
    }
}