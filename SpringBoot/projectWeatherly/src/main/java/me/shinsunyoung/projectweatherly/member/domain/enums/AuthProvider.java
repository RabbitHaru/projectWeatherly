package me.shinsunyoung.projectweatherly.member.domain.enums;

public enum AuthProvider {
    local("로컬"),
    kakao("카카오"),
    naver("네이버");


    private final String description;

    AuthProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}