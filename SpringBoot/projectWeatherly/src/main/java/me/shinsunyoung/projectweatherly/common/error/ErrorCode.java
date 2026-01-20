package me.shinsunyoung.projectweatherly.common.error;

public enum ErrorCode {
    INVALID_REQUEST("잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류"),
    NOT_FOUND("리소스를 찾을 수 없습니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("권한이 없습니다."),
    API_CALL_FAILED("API 호출에 실패했습니다."),
    API_PARSE_ERROR("API 응답 파싱에 실패했습니다."),
    LOCATION_NOT_FOUND("위치 정보를 찾을 수 없습니다."),
    WEATHER_DATA_UNAVAILABLE("날씨 정보를 불러올 수 없습니다."),
    AIR_QUALITY_DATA_UNAVAILABLE("대기질 정보를 불러올 수 없습니다."),
    BOARD_NOT_FOUND("게시글을 찾을 수 없습니다."),
    BOARD_CATEGORY_NOT_FOUND("카테고리를 찾을 수 없습니다."),
    BOARD_IMAGE_UPLOAD_FAILED("이미지 업로드에 실패했습니다."),
    BOARD_UNAUTHORIZED_ACCESS("게시글 접근 권한이 없습니다.");



    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }


    public String getMessage() {
        return message;
    }
}
