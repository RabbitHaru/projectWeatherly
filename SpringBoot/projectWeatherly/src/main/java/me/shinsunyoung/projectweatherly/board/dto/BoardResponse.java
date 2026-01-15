package me.shinsunyoung.projectweatherly.board.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;



import java.time.LocalDateTime;
import java.util.List;

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
    private String thumbnailUrl; // 대표 이미지 URL
    private List<BoardImageResponse> images; // 모든 이미지 목록
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isVerified;
    private BoardStatus boardStatus;
    private String boardStatusDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}