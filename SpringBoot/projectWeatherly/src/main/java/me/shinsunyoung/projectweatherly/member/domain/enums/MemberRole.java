package me.shinsunyoung.projectweatherly.member.domain.enums;

public enum MemberRole {
    USER("ROLE_USER", "일반 사용자"),
    REPORTER("ROLE_REPORTER", "기상 리포터"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String authority;
    private final String description;

    MemberRole(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDescription() {
        return description;
    }
}