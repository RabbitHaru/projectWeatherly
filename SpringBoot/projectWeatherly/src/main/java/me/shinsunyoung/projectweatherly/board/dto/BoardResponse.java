package me.shinsunyoung.projectweatherly.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Integer commentCount = 0;
    private List<CommentResponse> comments;
    // 작성자 정보
    private Long memberId;
    private String memberNickname;
    private String memberEmail;

    // 게시글 정보
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isVerified;
    private String boardStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 이미지 정보
    private List<String> imageUrls;
    private List<String> images;
    private String thumbnailUrl;



    // ✅ 기본값 false로 설정
    @Builder.Default
    private Boolean isAuthor = false;

    // ✅ Thymeleaf 템플릿 호환을 위한 getImages() 메서드
    public List<String> getImages() {
        return this.images != null ? this.images : this.imageUrls;
    }
}