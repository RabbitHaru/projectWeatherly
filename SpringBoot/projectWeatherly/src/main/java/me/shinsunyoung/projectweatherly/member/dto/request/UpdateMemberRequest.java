package me.shinsunyoung.projectweatherly.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // ✅ @Builder 어노테이션 추가
public class UpdateMemberRequest {
    private String nickname;
    private String profileImage;
}