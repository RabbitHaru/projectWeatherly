package me.shinsunyoung.projectweatherly.common.error;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 잘못된 요청
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        return new ErrorResponse(
                "BAD_REQUEST",
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    // 나중에 추가용 ( 예외 못 잡았을 때)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        return new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "서버에 오류가 발생했습니다.",
                LocalDateTime.now()
        );
    }
}
