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

    // 작성자 정보
    private Long memberId;
    private String memberNickname;  // 닉네임 필드 추가
    private String memberEmail;     // 이메일 (선택사항)

    // 게시글 정보
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isVerified;
    private String boardStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 이미지 정보
    private List<String> imageUrls;
    private String thumbnailUrl;

    // 닉네임 getter (Lombok이 자동 생성하지만 명시적으로 추가)
    public String getMemberNickname() {
        return memberNickname;
    }

    public void setMemberNickname(String memberNickname) {
        this.memberNickname = memberNickname;
    }
}