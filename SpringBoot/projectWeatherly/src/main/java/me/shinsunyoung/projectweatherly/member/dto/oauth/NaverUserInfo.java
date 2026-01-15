
package me.shinsunyoung.projectweatherly.member.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NaverUserInfo {

    @JsonProperty("resultcode")
    private String resultCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("response")
    private Response response;

    @Data
    public static class Response {
        @JsonProperty("id")
        private String id;  // 네이버 고유 ID

        @JsonProperty("email")
        private String email;

        @JsonProperty("name")
        private String name;

        @JsonProperty("nickname")
        private String nickname;

        @JsonProperty("profile_image")
        private String profileImage;

        @JsonProperty("age")
        private String age;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("birthday")
        private String birthday;

        @JsonProperty("mobile")
        private String mobile;
    }
}