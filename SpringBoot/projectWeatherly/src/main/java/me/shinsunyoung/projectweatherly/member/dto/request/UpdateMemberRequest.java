package me.shinsunyoung.projectweatherly.member.dto.request;



import jakarta.validation.constraints.Size;
import lombok.Data;



@Data
public class UpdateMemberRequest {

    @Size(min = 2, max = 50, message = "닉네임은 2~50자 이내로 입력해주세요.")
    private String nickname;

    private String profileImage;
}
