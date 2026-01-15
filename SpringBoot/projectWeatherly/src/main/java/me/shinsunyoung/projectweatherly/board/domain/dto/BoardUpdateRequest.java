package me.shinsunyoung.projectweatherly.board.domain.dto;

import lombok.Getter;
import lombok.Setter;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;

@Getter
@Setter
public class BoardUpdateRequest {
    private String title;
    private String content;
    private String weatherCondition;
    private String imageUrl;
    private BoardStatus boardStatus;
    private Boolean isVerified;
}