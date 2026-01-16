package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Getter;
import lombok.Setter;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;

import java.util.List;

@Getter
@Setter
public class BoardUpdateRequest {
    private String title;
    private String content;
    private String category;
    private List<Long> imageIdsToDelete; // 삭제할 이미지 ID 목록
    private BoardStatus boardStatus;
    private Boolean isVerified;
}