//package me.shinsunyoung.projectweatherly.member.exception;
//
//import lombok.Getter;
//
//@Getter
//public enum ErrorCode {
//
//    // 공통 에러
//    INTERNAL_SERVER_ERROR(500, "내부 서버 오류가 발생했습니다."),
//    BAD_REQUEST(400, "잘못된 요청입니다."),
//    UNAUTHORIZED(401, "인증이 필요합니다."),
//    FORBIDDEN(403, "접근 권한이 없습니다."),
//    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
//    METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP 메서드입니다."),
//    CONFLICT(409, "요청이 충돌되었습니다."),
//    VALIDATION_ERROR(422, "입력 값이 유효하지 않습니다."),
//
//    // 회원 관련 에러
//    MEMBER_NOT_FOUND(1001, "회원을 찾을 수 없습니다."),
//    EMAIL_ALREADY_EXISTS(1002, "이미 사용 중인 이메일입니다."),
//    INVALID_PASSWORD(1003, "비밀번호가 일치하지 않습니다."),
//    MEMBER_INACTIVE(1004, "비활성화된 회원입니다."),
//
//    // 파일 관련 에러
//    FILE_UPLOAD_ERROR(3001, "파일 업로드 중 오류가 발생했습니다."),
//    FILE_SIZE_EXCEEDED(3002, "파일 크기가 제한을 초과했습니다."),
//    INVALID_FILE_TYPE(3003, "지원하지 않는 파일 형식입니다."),
//    FILE_NOT_FOUND(3004, "파일을 찾을 수 없습니다.");
//
//    private final int code;
//    private final String message;
//
//    ErrorCode(int code, String message) {
//        this.code = code;
//        this.message = message;
//    }
//}