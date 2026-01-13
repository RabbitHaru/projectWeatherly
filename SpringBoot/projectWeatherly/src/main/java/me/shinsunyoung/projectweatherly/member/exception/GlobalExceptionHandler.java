package me.shinsunyoung.projectweatherly.member.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 비즈니스 예외 처리 ====================

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse2<Object>> handleMemberException(MemberException e) {
        log.error("MemberException 발생: {}", e.getMessage(), e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", "MemberException");
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse2.builder()
                        .success(false)
                        .message(e.getMessage())
                        .data(errorDetails)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now().toString())
                        .build());
    }

    // ==================== 인증/인가 예외 처리 ====================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse2<Object>> handleAuthenticationException(AuthenticationException e) {
        log.error("AuthenticationException 발생: {}", e.getMessage(), e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", "AuthenticationException");
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse2.unauthorized("인증이 필요합니다. 로그인 후 다시 시도해주세요."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse2<Object>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("AccessDeniedException 발생: {}", e.getMessage(), e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", "AccessDeniedException");
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("requiredAuthority", e.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse2.error("접근 권한이 없습니다.", HttpStatus.FORBIDDEN.value(), errorDetails));
    }

    // ==================== 요청 유효성 예외 처리 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse2<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::buildFieldError)
                .collect(Collectors.toList());

        List<Map<String, Object>> globalErrors = ex.getBindingResult().getGlobalErrors()
                .stream()
                .map(this::buildGlobalError)
                .collect(Collectors.toList());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fieldErrors", fieldErrors);
        errorDetails.put("globalErrors", globalErrors);
        errorDetails.put("errorCount", fieldErrors.size() + globalErrors.size());
        errorDetails.put("timestamp", LocalDateTime.now());

        log.error("유효성 검사 실패 - 필드 에러: {}, 글로벌 에러: {}", fieldErrors.size(), globalErrors.size());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse2.builder()
                        .success(false)
                        .message("입력 값이 유효하지 않습니다.")
                        .data(errorDetails)
                        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                        .timestamp(LocalDateTime.now().toString())
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse2<Object>> handleConstraintViolationException(
            ConstraintViolationException e) {

        List<Map<String, Object>> violations = e.getConstraintViolations()
                .stream()
                .map(this::buildConstraintViolation)
                .collect(Collectors.toList());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("violations", violations);
        errorDetails.put("violationCount", violations.size());
        errorDetails.put("timestamp", LocalDateTime.now());

        log.error("제약 조건 위반: {}개", violations.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse2.builder()
                        .success(false)
                        .message("요청 데이터의 제약 조건이 위반되었습니다.")
                        .data(errorDetails)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now().toString())
                        .build());
    }

    // ==================== 요청 매개변수 예외 처리 ====================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse2<Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e) {

        String message = String.format("파라미터 '%s'의 값 '%s'이(가) 유효하지 않습니다. 요구되는 타입: %s",
                e.getName(),
                e.getValue(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "알 수 없음");

        log.error("파라미터 타입 불일치: {}", message, e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("parameterName", e.getName());
        errorDetails.put("parameterValue", e.getValue());
        errorDetails.put("requiredType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : null);
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse2.badRequest(message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse2<Object>> handleMissingParameterException(
            MissingServletRequestParameterException e) {

        String message = String.format("필수 파라미터 '%s'이(가) 누락되었습니다. (타입: %s)",
                e.getParameterName(), e.getParameterType());

        log.error("필수 파라미터 누락: {}", message, e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("parameterName", e.getParameterName());
        errorDetails.put("parameterType", e.getParameterType());
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse2.error(message, HttpStatus.BAD_REQUEST.value(), errorDetails));
    }

    // ==================== HTTP 메서드 및 경로 예외 처리 ====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse2<Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {

        // 수정된 부분: 람다 표현식 사용
        String supportedMethods = e.getSupportedHttpMethods() != null ?
                String.join(", ", e.getSupportedHttpMethods().stream()
                        .map(httpMethod -> httpMethod.name())  // 람다 표현식으로 변경
                        .collect(Collectors.toList())) : "알 수 없음";

        String message = String.format("지원되지 않는 HTTP 메서드입니다. 요청 메서드: %s, 지원 메서드: [%s]",
                e.getMethod(), supportedMethods);

        log.error("지원되지 않는 HTTP 메서드: {}", message, e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("requestedMethod", e.getMethod());
        errorDetails.put("supportedMethods", supportedMethods);
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse2.error(message, HttpStatus.METHOD_NOT_ALLOWED.value(), errorDetails));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse2<Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {

        String message = String.format("요청한 경로를 찾을 수 없습니다. [%s %s]",
                e.getHttpMethod(), e.getRequestURL());

        log.error("요청 경로 없음: {}", message, e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("httpMethod", e.getHttpMethod());
        errorDetails.put("requestURL", e.getRequestURL());
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse2.notFound(message));
    }

    // ==================== 파일 업로드 예외 처리 ====================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse2<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e) {

        String message = "업로드 파일 크기가 제한을 초과했습니다.";
        log.error("파일 크기 초과: {}", e.getMessage(), e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("maxUploadSize", e.getMaxUploadSize() + " bytes");
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse2.error(message, HttpStatus.PAYLOAD_TOO_LARGE.value(), errorDetails));
    }

    // ==================== 시스템 예외 처리 ====================

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse2<Object>> handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException 발생: ", e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", "NullPointerException");
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("stackTrace", getStackTraceAsString(e));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse2.internalServerError("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse2<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException 발생: {}", e.getMessage(), e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", "IllegalArgumentException");
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse2.badRequest(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse2<Object>> handleIllegalStateException(IllegalStateException e) {
        log.error("IllegalStateException 발생: {}", e.getMessage(), e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", "IllegalStateException");
        errorDetails.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse2.conflict(e.getMessage()));
    }

    // ==================== 최종 예외 처리 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse2<Object>> handleAllExceptions(Exception e) {
        log.error("예기치 않은 오류 발생: ", e);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorType", e.getClass().getSimpleName());
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("errorMessage", e.getMessage());

        // 개발 환경에서만 스택 트레이스 포함
        if (isDevelopmentEnvironment()) {
            errorDetails.put("stackTrace", getStackTraceAsString(e));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse2.builder()
                        .success(false)
                        .message("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                        .data(errorDetails)
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now().toString())
                        .build());
    }

    // ==================== 내부 헬퍼 메서드 ====================

    private Map<String, Object> buildFieldError(FieldError fieldError) {
        Map<String, Object> error = new HashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", fieldError.getDefaultMessage());
        error.put("rejectedValue", fieldError.getRejectedValue());
        error.put("code", fieldError.getCode());
        error.put("objectName", fieldError.getObjectName());
        return error;
    }

    private Map<String, Object> buildGlobalError(org.springframework.validation.ObjectError globalError) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", globalError.getDefaultMessage());
        error.put("code", globalError.getCode());
        error.put("objectName", globalError.getObjectName());
        return error;
    }

    private Map<String, Object> buildConstraintViolation(ConstraintViolation<?> violation) {
        Map<String, Object> error = new HashMap<>();
        error.put("propertyPath", violation.getPropertyPath().toString());
        error.put("message", violation.getMessage());
        error.put("invalidValue", violation.getInvalidValue());
        error.put("rootBeanClass", violation.getRootBeanClass().getSimpleName());
        return error;
    }

    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private boolean isDevelopmentEnvironment() {
        String env = System.getProperty("spring.profiles.active", "production");
        return "dev".equals(env) || "development".equals(env);
    }
}