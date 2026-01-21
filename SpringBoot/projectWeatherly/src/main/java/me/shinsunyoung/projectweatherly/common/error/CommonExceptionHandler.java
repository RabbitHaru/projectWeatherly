package me.shinsunyoung.projectweatherly.common.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice // ★ 수정됨: RestControllerAdvice -> ControllerAdvice
public class CommonExceptionHandler {

    // ★ 1. 비즈니스 로직 예외 (수정/삭제 차단, 신고 방지 등 핵심 로직)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, RuntimeException.class})
    public Object handleBusinessException(Exception e, RedirectAttributes redirectAttributes, HttpServletRequest request) {

        String requestUri = request.getRequestURI();

        // A. 관리자 페이지나 API 요청인 경우 -> 기존처럼 JSON 에러 반환
        if (requestUri.startsWith("/api/") || requestUri.startsWith("/admin")) {
            log.warn("API 예외 발생: {} (URL: {})", e.getMessage(), requestUri);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        }

        // B. 일반 사용자 화면(HTML) 요청인 경우 -> 팝업 메시지를 담아서 이전 페이지로 리다이렉트
        log.warn("View 예외 발생: {} (URL: {})", e.getMessage(), requestUri);

        // footer.html이 이 'errorMessage'를 감지해서 팝업을 띄웁니다.
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        // 이전 페이지로 돌려보냄
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    // 2. HTTP 클라이언트 에러 (API 호출 실패 등)
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpClientError(HttpClientErrorException e) {
        log.error("HTTP 클라이언트 에러: {}", e.getMessage());
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error("API 호출 중 오류가 발생했습니다.", "API_ERROR"));
    }

    // 3. 리소스 접근 에러 (타임아웃 등)
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAccess(ResourceAccessException e) {
        log.error("리소스 접근 오류 (타임아웃): {}", e.getMessage());
        return ResponseEntity.status(504)
                .body(ApiResponse.error("API 응답 시간이 초과되었습니다.", "TIMEOUT_ERROR"));
    }

    // 4. 커스텀 예외 (API용은 JSON 반환)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("커스텀 예외 발생: {} - {}", e.getErrorCode(), e.getDetailMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getDetailMessage(), e.getErrorCode().name()));
    }

    // 5. 정적 리소스(이미지 등) 없음 예외 - 로그만 남기고 무시 (불필요한 에러 방지)
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFound(NoResourceFoundException e) {
        log.debug("리소스 파일 없음: {}", e.getResourcePath());
    }

    // 6. 그 외 모든 예외 (최후의 보루)
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        log.error("예기치 않은 오류 발생", e);

        if (request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/admin")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", "UNEXPECTED_ERROR"));
        }

        redirectAttributes.addFlashAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
        return "redirect:/";
    }
}