package me.shinsunyoung.projectweatherly.board.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardRequest {
    private String title;
    private String content;
    private String weatherCondition; // 날씨 상태만 포함
    private String imageUrl;
}