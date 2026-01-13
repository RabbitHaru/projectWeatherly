package me.shinsunyoung.projectweatherly.member.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import me.shinsunyoung.projectweatherly.member.service.dto.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final MemberService memberService;

    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler(objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ==================== CSRF 설정 ====================
                .csrf(csrf -> csrf
                        .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/auth/login",           // 로그인은 CSRF 제외
                                "/auth/signup",          // 회원가입은 CSRF 제외
                                "/api/members/signup",   // REST API 회원가입
                                "/oauth2/**"             // OAuth2 콜백
                        )
                )

                // ==================== 세션 관리 ====================
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                )

                // ==================== 인가(Authorization) 설정 ====================
                .authorizeHttpRequests(auth -> auth
                        // ========== 정적 리소스 및 공개 API ==========
                        .requestMatchers(
                                // 기본 페이지
                                "/",
                                "/index",
                                "/home",

                                // 인증 관련 (공개)
                                "/auth/**",
                                "/login",
                                "/login/**",
                                "/signup",
                                "/signup/**",

                                // OAuth2 관련
                                "/oauth2/**",

                                // 회원가입 관련 API (공개)
                                "/api/members/signup",
                                "/api/members/check-email/**",
                                "/api/members/check-nickname/**",

                                // 프로필 이미지 접근 (공개)
                                "/api/members/profile-images/**",

                                // 에러 페이지
                                "/error",
                                "/error/**",

                                // 정적 리소스
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/favicon.ico",

                                // 업로드 파일 접근
                                "/uploads/**",

                                // 블로그 관련 (공개)
                                "/articles/**",
                                "/api/articles/**",
                                "/new-article",

                                // Swagger/API 문서
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ========== 인증 필요 API (일반 사용자) ==========
                        .requestMatchers(
                                // 현재 사용자 정보
                                "/api/members/me",
                                "/api/members/me/**",

                                // 마이페이지
                                "/api/members/mypage",
                                "/api/members/mypage/**",

                                // 파일 업로드 (인증 필요)
                                "/api/members/me/profile-image",

                                // 약관 및 알림 설정
                                "/api/members/me/agreement",
                                "/api/members/me/notification",

                                // 회원 탈퇴
                                "/api/members/me",

                                // 사용자 통계
                                "/api/members/me/stats"
                        ).authenticated()

                        // ========== 관리자 전용 API ==========
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/members/search",          // 회원 검색
                                "/api/members/{id}/**"          // 특정 회원 관리 (관리자)
                        ).hasRole("ADMIN")

                        // ========== 기타 회원 관련 API ==========
                        .requestMatchers(
                                "/api/members/{id}",           // 특정 회원 조회 (공개 정보)
                                "/api/members/{id}/**"         // 특정 회원 관리 (본인 또는 관리자)
                        ).authenticated()

                        // ========== 기타 모든 요청 ==========
                        .anyRequest().authenticated()
                )

                // ==================== 폼 로그인 설정 ====================
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .successHandler(customAuthenticationSuccessHandler())
                        .permitAll()
                )

                // ==================== OAuth2 로그인 설정 ====================
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .successHandler(customAuthenticationSuccessHandler())
                )

                // ==================== UserDetailsService 설정 ====================
                .userDetailsService(memberService)

                // ==================== 로그아웃 설정 ====================
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // ==================== 예외 처리 설정 ====================
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("인증 실패: {}", authException.getMessage());
                            if (request.getRequestURI().startsWith("/api/")) {
                                // API 요청인 경우 JSON 응답
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(401);
                                response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\",\"statusCode\":401}");
                            } else {
                                // 웹 페이지 요청인 경우 로그인 페이지로 리다이렉트
                                response.sendRedirect("/login?error=unauthorized");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("접근 거부: {}", accessDeniedException.getMessage());
                            if (request.getRequestURI().startsWith("/api/")) {
                                // API 요청인 경우 JSON 응답
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(403);
                                response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다.\",\"statusCode\":403}");
                            } else {
                                // 웹 페이지 요청인 경우 접근 거부 페이지로 리다이렉트
                                response.sendRedirect("/error/403");
                            }
                        })
                )

                // ==================== 기타 보안 설정 ====================
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:")
                        )
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}