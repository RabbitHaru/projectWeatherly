package me.shinsunyoung.projectweatherly.member.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse2<T> {

    private boolean success;
    private String message;
    private T data;

    // 성공 응답
    public static <T> ApiResponse2<T> success(T data) {
        return ApiResponse2.<T>builder()
                .success(true)
                .message("요청이 성공적으로 처리되었습니다.")
                .data(data)
                .build();
    }

    public static <T> ApiResponse2<T> success(String message, T data) {
        return ApiResponse2.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // 에러 응답
    public static ApiResponse2<Void> error(String message) {
        return ApiResponse2.<Void>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }

    // ✅ 이것이 핵심! 데이터와 함께 에러 응답
    public static <T> ApiResponse2<T> error(String message, T data) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }

    // 편의 메서드들
    public static ApiResponse2<String> errorString(String message) {
        return ApiResponse2.<String>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse2<T> errorWithType(String message, Class<T> type) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}