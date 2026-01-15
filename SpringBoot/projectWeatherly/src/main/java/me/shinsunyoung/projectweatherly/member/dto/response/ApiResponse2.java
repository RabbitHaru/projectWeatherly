package me.shinsunyoung.projectweatherly.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse2<T> {

    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;
    private String timestamp;
    private String path;

    // ==================== 성공 응답 팩토리 메서드 ====================

    /**
     * 데이터와 기본 메시지를 포함한 성공 응답
     */
    public static <T> ApiResponse2<T> success(T data) {
        return ApiResponse2.<T>builder()
                .success(true)
                .message("요청이 성공적으로 처리되었습니다.")
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 커스텀 메시지와 데이터를 포함한 성공 응답
     */
    public static <T> ApiResponse2<T> success(String message, T data) {
        return ApiResponse2.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 메시지만 있는 성공 응답 (데이터 없음)
     */
    public static ApiResponse2<Void> success(String message) {
        return ApiResponse2.<Void>builder()
                .success(true)
                .message(message)
                .data(null)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 경로 정보가 포함된 성공 응답
     */
    public static <T> ApiResponse2<T> success(String message, T data, String path) {
        return ApiResponse2.<T>builder()
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
    public static <T> ApiResponse2<T> success(String message, T data, int statusCode) {
        return ApiResponse2.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 상태 코드가 지정된 성공 응답 (데이터 없음)
     */
    public static ApiResponse2<Void> success(String message, int statusCode) {
        return ApiResponse2.<Void>builder()
                .success(true)
                .message(message)
                .data(null)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    // ==================== 에러 응답 팩토리 메서드 ====================

    /**
     * 기본 에러 응답
     */
    public static <T> ApiResponse2<T> error(String message) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 상태 코드가 지정된 에러 응답
     */
    public static <T> ApiResponse2<T> error(String message, int statusCode) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 데이터가 포함된 에러 응답
     */
    public static <T> ApiResponse2<T> error(String message, T data) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 상태 코드와 데이터가 포함된 에러 응답
     */
    public static <T> ApiResponse2<T> error(String message, int statusCode, T data) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 경로 정보가 포함된 에러 응답
     */
    public static <T> ApiResponse2<T> error(String message, int statusCode, String path) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .path(path)
                .build();
    }

    /**
     * 경로와 데이터가 포함된 에러 응답
     */
    public static <T> ApiResponse2<T> error(String message, int statusCode, T data, String path) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now().toString())
                .path(path)
                .build();
    }

    // ==================== HTTP 상태 코드별 유틸리티 메서드 ====================

    /**
     * 201 Created 응답
     */
    public static <T> ApiResponse2<T> created(String message, T data) {
        return success(message, data, 201);
    }

    public static ApiResponse2<Void> created(String message) {
        return success(message, 201);
    }

    /**
     * 204 No Content 응답
     */
    public static ApiResponse2<Void> noContent(String message) {
        return success(message, 204);
    }

    /**
     * 400 Bad Request 응답
     */
    public static <T> ApiResponse2<T> badRequest(String message) {
        return error(message, 400);
    }

    /**
     * 401 Unauthorized 응답
     */
    public static <T> ApiResponse2<T> unauthorized(String message) {
        return error(message, 401);
    }

    /**
     * 403 Forbidden 응답
     */
    public static <T> ApiResponse2<T> forbidden(String message) {
        return error(message, 403);
    }

    /**
     * 404 Not Found 응답
     */
    public static <T> ApiResponse2<T> notFound(String message) {
        return error(message, 404);
    }

    /**
     * 409 Conflict 응답
     */
    public static <T> ApiResponse2<T> conflict(String message) {
        return error(message, 409);
    }

    /**
     * 422 Unprocessable Entity 응답 (유효성 검사 실패 등)
     */
    public static <T> ApiResponse2<T> unprocessableEntity(String message) {
        return error(message, 422);
    }

    /**
     * 429 Too Many Requests 응답
     */
    public static <T> ApiResponse2<T> tooManyRequests(String message) {
        return error(message, 429);
    }

    /**
     * 500 Internal Server Error 응답
     */
    public static <T> ApiResponse2<T> internalServerError(String message) {
        return error(message, 500);
    }

    /**
     * 502 Bad Gateway 응답
     */
    public static <T> ApiResponse2<T> badGateway(String message) {
        return error(message, 502);
    }

    /**
     * 503 Service Unavailable 응답
     */
    public static <T> ApiResponse2<T> serviceUnavailable(String message) {
        return error(message, 503);
    }

    // ==================== 도메인별 에러 응답 ====================

    /**
     * 인증 실패 응답
     */
    public static <T> ApiResponse2<T> authenticationError(String message) {
        return error(message, 401);
    }

    /**
     * 인증 실패 응답 (데이터 포함)
     */
    public static <T> ApiResponse2<T> authenticationError(String message, T data) {
        return error(message, 401, data);
    }

    /**
     * 권한 부족 응답
     */
    public static <T> ApiResponse2<T> authorizationError(String message) {
        return error(message, 403);
    }

    /**
     * 권한 부족 응답 (데이터 포함)
     */
    public static <T> ApiResponse2<T> authorizationError(String message, T data) {
        return error(message, 403, data);
    }

    /**
     * 데이터 유효성 에러 응답 (필드별 에러 정보 포함)
     */
    public static ApiResponse2<ValidationErrorResponse> validationError(
            String message, ValidationErrorResponse errors) {
        return ApiResponse2.<ValidationErrorResponse>builder()
                .success(false)
                .message(message)
                .data(errors)
                .statusCode(422)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 유효성 검사 실패 응답 (간단한 메시지)
     */
    public static <T> ApiResponse2<T> validationError(String message) {
        return error(message, 422);
    }

    /**
     * 비즈니스 로직 에러 응답
     */
    public static <T> ApiResponse2<T> businessError(String message, T details) {
        return error(message, 400, details);
    }

    /**
     * 비즈니스 로직 에러 응답 (간단한 메시지)
     */
    public static <T> ApiResponse2<T> businessError(String message) {
        return error(message, 400);
    }

    /**
     * 파일 업로드 에러 응답
     */
    public static <T> ApiResponse2<T> fileUploadError(String message) {
        return error(message, 400);
    }

    /**
     * 파일 업로드 에러 응답 (데이터 포함)
     */
    public static <T> ApiResponse2<T> fileUploadError(String message, T data) {
        return error(message, 400, data);
    }

    // ==================== 편의 메서드 (기존 호환성 유지) ====================

    /**
     * 문자열 에러 응답 (레거시 호환성)
     */
    @Deprecated
    public static ApiResponse2<String> errorString(String message) {
        return ApiResponse2.<String>builder()
                .success(false)
                .message(message)
                .data(null)
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 타입 지정 에러 응답 (레거시 호환성)
     */
    @Deprecated
    public static <T> ApiResponse2<T> errorWithType(String message, Class<T> type) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    // ==================== 빌더 직접 사용을 위한 헬퍼 메서드 ====================

    /**
     * 성공 응답 빌더 생성
     */
    public static <T> ApiResponse2Builder<T> successBuilder() {
        return ApiResponse2.<T>builder()
                .success(true)
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString());
    }

    /**
     * 에러 응답 빌더 생성
     */
    public static <T> ApiResponse2Builder<T> errorBuilder() {
        return ApiResponse2.<T>builder()
                .success(false)
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString());
    }

    /**
     * 사용자 정의 빌더 생성
     */
    public static <T> ApiResponse2Builder<T> builder() {
        return new ApiResponse2Builder<T>()
                .timestamp(LocalDateTime.now().toString());
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 현재 타임스탬프 생성
     */
    private static String currentTimestamp() {
        return LocalDateTime.now().toString();
    }

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

    // ==================== 내부 클래스: 페이지네이션 응답 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PagedResponse<T> {
        private T content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;

        /**
         * 페이지네이션 응답 생성
         */
        public static <T> ApiResponse2<PagedResponse<T>> pagedSuccess(String message, T content,
                                                                      int page, int size, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);

            PagedResponse<T> pagedResponse = PagedResponse.<T>builder()
                    .content(content)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(page == 0)
                    .last(page >= totalPages - 1)
                    .hasNext(page < totalPages - 1)
                    .hasPrevious(page > 0)
                    .build();

            return ApiResponse2.success(message, pagedResponse);
        }

        /**
         * 기본 메시지가 포함된 페이지네이션 응답
         */
        public static <T> ApiResponse2<PagedResponse<T>> pagedSuccess(T content,
                                                                      int page, int size, long totalElements) {
            return pagedSuccess("데이터 조회 성공", content, page, size, totalElements);
        }
    }
}