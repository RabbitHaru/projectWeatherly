package me.shinsunyoung.projectweatherly.member.domain.member;

import lombok.Getter;

@Getter
public enum MemberRole {
    USER("일반 사용자"),
    REPORTER("리포터"),
    ADMIN("관리자");

    private final String description;

    MemberRole(String description) {
        this.description = description;
    }


    public static MemberRole fromString(String role) {
        if (role == null || role.trim().isEmpty()) {
            return USER;
        }

        try {
            return MemberRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER; // 기본값
        }
    }
}