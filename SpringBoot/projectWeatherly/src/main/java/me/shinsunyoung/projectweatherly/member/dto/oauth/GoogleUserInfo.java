package me.shinsunyoung.projectweatherly.member.dto.oauth;

import lombok.Data;

@Data
public class GoogleUserInfo {
    private String sub;          // Google 고유 ID
    private String email;
    private String name;
    private String given_name;   // 이름
    private String family_name;  // 성
    private String picture;      // 프로필 이미지 URL
    private String locale;
    private boolean email_verified;
}