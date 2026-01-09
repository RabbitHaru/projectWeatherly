// src/main/java/me/shinsunyoung/projectweatherly/config/SecurityConfig.java
package me.shinsunyoung.projectweatherly.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                // 홈페이지와 정적 리소스는 인증 없이 접근 허용
                    http.authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 기본 로그인 페이지 사용 (필요시 커스텀 가능)
                .formLogin((form) -> form
                        .loginPage("/login").permitAll()
                )
                .logout((logout) -> logout.permitAll());

        return http.build();
    }
}