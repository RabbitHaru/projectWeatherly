package me.shinsunyoung.projectweatherly.member.exception;



import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse2<Void>> handleMemberException(
            MemberException e, HttpServletRequest request) {

        log.error("MemberException: {} - {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse2.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse2<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest()
                .body(ApiResponse2.error("유효성 검사 실패", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse2<Void>> handleGeneralException(
            Exception e, HttpServletRequest request) {

        log.error("Unexpected error: {} - {}", request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity.internalServerError()
                .body(ApiResponse2.error("서버 내부 오류가 발생했습니다."));
    }
}
