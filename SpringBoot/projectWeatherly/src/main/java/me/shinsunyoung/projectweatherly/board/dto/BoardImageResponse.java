package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BoardImageResponse {
    private Long imageId;
    private String imageUrl;
    private String imageName;
    private Long imageSize;
    private Integer displayOrder;
    private Boolean isThumbnail;
    private LocalDateTime createdAt;
}