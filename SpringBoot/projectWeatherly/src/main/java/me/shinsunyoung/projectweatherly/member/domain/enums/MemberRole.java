package me.shinsunyoung.projectweatherly.member.domain.enums;

public enum MemberRole {
    USER("일반 사용자"),
    REPORTER("리포터"),
    ADMIN("관리자");

    private final String description;

    MemberRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


}
