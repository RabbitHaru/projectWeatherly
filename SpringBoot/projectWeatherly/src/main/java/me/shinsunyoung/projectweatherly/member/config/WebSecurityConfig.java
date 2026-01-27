package me.shinsunyoung.projectweatherly.member.config;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.service.CustomOAuth2UserService;
import me.shinsunyoung.projectweatherly.member.service.UserDetailService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration // 설정파일 어노테이션
@EnableWebSecurity // 보안 설정 어노테이션
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final UserDetailService userService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final DataSource dataSource; // 데이터베이스 연결 객체

    @Bean // 자동 로그인 데이터 저장시 사용하는 레포지토리
    public PersistentTokenRepository persistentTokenRepository(){
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        // persistent_logins 테이블 생성 SQL 실행 => 이미 테이블 있으면 에러를 발생시킴
        repo.setCreateTableOnStartup(false);
        return repo;
    }

    @Bean
    public WebSecurityCustomizer configure(){
        return (web)-> web.ignoring()
                // 정적 파일(html, css, js, 이미지파일 등) 스프링 시큐리티 해제
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                // 특정 폴더 접근의 스프링 시큐리티 해제
                .requestMatchers("/static/**");
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http
                // 인가가 필요한 페이지 설정
                .authorizeHttpRequests(auth -> auth
                        // [수정됨] /auth/api/** 대신 /auth/** 로 변경하여 아이디 찾기 페이지도 접근 허용
                        .requestMatchers("/","/login","/signup","/user"
                                ,"/articles","/articles/{id}","/file/**", "/auth/**", "/S3/**")
                        .permitAll()
                        // 관리자 페이지는 'ADMIN' 권한이 있는 사람만 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/members/**", "/mypage/**").authenticated()
                        // 위의 url이외에 모든 url은 로그인이 필요하도록 설정
                        .anyRequest().permitAll())
                // form태그를 사용한 로그인 관련 설정
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")// 로그인 페이지 주소
                        .defaultSuccessUrl("/")// 로그인 성공시 출력할 주소

                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")// 로그아웃 성공시 주소
                        .invalidateHttpSession(true))//로그아웃 시 모든 세션데이터 삭제
                .csrf(AbstractHttpConfigurer::disable) // CSRF 사용안함 설정
                // .csrf(csrf -> csrf.disable()) // 람다식
                .rememberMe(remember -> remember
                        .key("123456789") // 암호화용 키값
                        .tokenRepository(persistentTokenRepository()) // DB연동 레포지토리 설정
                        .userDetailsService(userService) // 사용자 정보 조회용 서비스
                        .tokenValiditySeconds(60*60*24*7) // 토큰의 유지 시간(초단위)
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .defaultSuccessUrl("/")
                )
                .build();
    }
    @Bean
    // 로그인 방식 설정
    public AuthenticationManager authenticationManager(HttpSecurity http
            , BCryptPasswordEncoder bCryptPasswordEncoder
            , UserDetailService userDetailService) throws Exception{
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
        // 비밀번호 암호화 방식 설정
        authProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return new ProviderManager(authProvider);
    }
    @Bean
    // 암호화 방식을 Bean으로 설정 , 여러군데에서 쓰이기 때문에 Bean설정
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }
}