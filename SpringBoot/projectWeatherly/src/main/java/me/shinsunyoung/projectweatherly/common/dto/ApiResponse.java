package me.shinsunyoung.projectweatherly.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;   // 기존 ApiResponse 필드
    private Integer statusCode; // ApiResponse2 필드
    private String timestamp;   // ApiResponse2 필드
    private String path;        // ApiResponse2 필드

    // ==================== 기존 ApiResponse 메서드 유지 ====================

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, 400);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    // ==================== ApiResponse2의 기능 통합 ====================

    /**
     * 상태 코드가 지정된 에러 응답 (필수 - 빨간줄 해결을 위해 추가)
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 경로 정보가 포함된 성공 응답
     */
    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .path(path)
                .build();
    }

    /**
     * 상태 코드가 지정된 성공 응답
     */
    public static <T> ApiResponse<T> success(String message, T data, int statusCode) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 상태 코드와 데이터가 포함된 에러 응답
     */
    public static <T> ApiResponse<T> error(String message, int statusCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    // ==================== HTTP 상태 코드별 유틸리티 메서드 ====================

    /**
     * 201 Created 응답
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return success(message, data, 201);
    }

    /**
     * 204 No Content 응답
     */
    public static <T> ApiResponse<T> noContent(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .statusCode(204)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 400 Bad Request 응답
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(message, 400);
    }

    /**
     * 401 Unauthorized 응답
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(message, 401);
    }

    /**
     * 403 Forbidden 응답
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return error(message, 403);
    }

    /**
     * 404 Not Found 응답
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(message, 404);
    }

    /**
     * 409 Conflict 응답
     */
    public static <T> ApiResponse<T> conflict(String message) {
        return error(message, 409);
    }

    /**
     * 422 Unprocessable Entity 응답 (유효성 검사 실패 등)
     */
    public static <T> ApiResponse<T> unprocessableEntity(String message) {
        return error(message, 422);
    }

    /**
     * 429 Too Many Requests 응답
     */
    public static <T> ApiResponse<T> tooManyRequests(String message) {
        return error(message, 429);
    }

    /**
     * 500 Internal Server Error 응답
     */
    public static <T> ApiResponse<T> internalServerError(String message) {
        return error(message, 500);
    }

    // ==================== 호환성 유지를 위한 어댑터 메서드 ====================

    /**
     * 기존 ApiResponse2 -> 새로운 ApiResponse 변환
     */
    public static <T> ApiResponse<T> fromApiResponse2(ApiResponse2<T> apiResponse2) {
        if (apiResponse2 == null) return null;

        return ApiResponse.<T>builder()
                .success(apiResponse2.isSuccess())
                .message(apiResponse2.getMessage())
                .data(apiResponse2.getData())
                .statusCode(apiResponse2.getStatusCode())
                .timestamp(apiResponse2.getTimestamp())
                .path(apiResponse2.getPath())
                .build();
    }

    /**
     * 새로운 ApiResponse -> 기존 ApiResponse2 변환
     */
    public ApiResponse2<T> toApiResponse2() {
        return ApiResponse2.<T>builder()
                .success(this.success)
                .message(this.message)
                .data(this.data)
                .statusCode(this.statusCode)
                .timestamp(this.timestamp)
                .path(this.path)
                .build();
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 응답이 성공인지 확인
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * 응답이 실패인지 확인
     */
    public boolean isError() {
        return !this.success;
    }

    /**
     * 에러 코드 설정 (기존 ApiResponse 호환성)
     */
    public ApiResponse<T> withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * 경로 설정 (ApiResponse2 호환성)
     */
    public ApiResponse<T> withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * 상태 코드 설정 (ApiResponse2 호환성)
     */
    public ApiResponse<T> withStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    // ==================== 내부 클래스: 유효성 검사 에러 응답 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationErrorResponse {
        private String field;
        private String message;
        private Object rejectedValue;
        private String errorCode;
        private String suggestion; // 개선 제안 메시지

        /**
         * 필드 에러 생성
         */
        public static ValidationErrorResponse of(String field, String message) {
            return ValidationErrorResponse.builder()
                    .field(field)
                    .message(message)
                    .build();
        }

        /**
         * 필드와 거부된 값이 포함된 에러 생성
         */
        public static ValidationErrorResponse of(String field, String message, Object rejectedValue) {
            return ValidationErrorResponse.builder()
                    .field(field)
                    .message(message)
                    .rejectedValue(rejectedValue)
                    .build();
        }

        /**
         * 에러 코드가 포함된 에러 생성
         */
        public static ValidationErrorResponse of(String field, String message, String errorCode) {
            return ValidationErrorResponse.builder()
                    .field(field)
                    .message(message)
                    .errorCode(errorCode)
                    .build();
        }

        /**
         * 전체 정보가 포함된 에러 생성
         */
        public static ValidationErrorResponse of(String field, String message, Object rejectedValue,
                                                 String errorCode, String suggestion) {
            return ValidationErrorResponse.builder()
                    .field(field)
                    .message(message)
                    .rejectedValue(rejectedValue)
                    .errorCode(errorCode)
                    .suggestion(suggestion)
                    .build();
        }
    }
}