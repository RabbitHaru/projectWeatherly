package me.shinsunyoung.projectweatherly.member.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // OAuth2User에서 추출한 정보
        String accessToken = (String) oAuth2User.getAttribute("access_token");
        String refreshToken = (String) oAuth2User.getAttribute("refresh_token");
        Long memberId = oAuth2User.getAttribute("member_id");
        String userEmail = oAuth2User.getAttribute("email");
        String nickname = oAuth2User.getAttribute("nickname");

        log.info("OAuth2 로그인 성공: memberId={}, email={}", memberId, userEmail);

        // 프론트엔드로 리디렉션하면서 토큰과 사용자 정보 전달
        String redirectUrl = String.format(
                "http://localhost:3000/login-success?" +
                        "access_token=%s&" +
                        "refresh_token=%s&" +
                        "member_id=%s&" +
                        "email=%s&" +
                        "nickname=%s",
                accessToken,
                refreshToken,
                memberId,
                URLEncoder.encode(userEmail != null ? userEmail : "", StandardCharsets.UTF_8),
                URLEncoder.encode(nickname != null ? nickname : "", StandardCharsets.UTF_8)
        );

        response.sendRedirect(redirectUrl);
    }
}