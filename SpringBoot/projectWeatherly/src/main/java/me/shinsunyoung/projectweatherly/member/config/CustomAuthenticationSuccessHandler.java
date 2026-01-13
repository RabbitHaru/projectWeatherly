package me.shinsunyoung.projectweatherly.member.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("로그인 성공: {}", authentication.getName());

        // 요청 경로 확인
        boolean isApiRequest = request.getRequestURI().startsWith("/api/");

        if (isApiRequest) {
            // API 요청인 경우 JSON 응답
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(200);

            Map<String, Object> data = new HashMap<>();
            data.put("username", authentication.getName());
            data.put("authorities", authentication.getAuthorities());

            ApiResponse2<Map<String, Object>> apiResponse = ApiResponse2.success("로그인 성공", data);

            objectMapper.writeValue(response.getWriter(), apiResponse);
        } else {
            // 웹 페이지 요청인 경우 리다이렉트
            String redirectUrl = request.getParameter("redirect");
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect("/");
            }
        }
    }
}