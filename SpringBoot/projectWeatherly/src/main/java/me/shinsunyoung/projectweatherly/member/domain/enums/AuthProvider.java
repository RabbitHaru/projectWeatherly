package me.shinsunyoung.projectweatherly.member.domain.enums;



public enum AuthProvider {
    LOCAL("로컬"),
    KAKAO("카카오"),
    NAVER("네이버"),
    GOOGLE("구글");

    private final String description;

    AuthProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
