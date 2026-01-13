package me.shinsunyoung.projectweatherly.common.error;

import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpClientError(HttpClientErrorException e) {
        log.error("HTTP 클라이언트 에러: {}", e.getMessage());
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error("API 호출 중 오류가 발생했습니다.", "API_ERROR"));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAccess(ResourceAccessException e) {
        log.error("리소스 접근 오류 (타임아웃): {}", e.getMessage());
        return ResponseEntity.status(504)
                .body(ApiResponse.error("API 응답 시간이 초과되었습니다.", "TIMEOUT_ERROR"));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("커스텀 예외 발생: {} - {}", e.getErrorCode(), e.getDetailMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getDetailMessage(), e.getErrorCode().name()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("런타임 예외 발생: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서비스 처리 중 오류가 발생했습니다.", "INTERNAL_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예외 발생: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("예기치 않은 오류가 발생했습니다.", "UNEXPECTED_ERROR"));
    }
}