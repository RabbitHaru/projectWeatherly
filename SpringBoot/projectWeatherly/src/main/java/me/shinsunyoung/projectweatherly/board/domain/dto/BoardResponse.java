package me.shinsunyoung.projectweatherly.board.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BoardResponse {
    private Long boardId;
    private Long memberId;
    private String memberNickname;
    private String memberProfileImage;
    private String title;
    private String content;
    private String weatherCondition;
    private String imageUrl;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isVerified;
    private BoardStatus boardStatus;
    private String boardStatusDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}