package me.shinsunyoung.projectweatherly.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String code; // 에러코드
    private String message; // 사용자용 메세지
    private LocalDateTime times; // 발생 시간

}
