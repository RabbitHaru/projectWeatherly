package me.shinsunyoung.projectweatherly.member.domain.member;

import lombok.Getter;

@Getter
public enum AuthProvider {
    LOCAL("로컬"),
    KAKAO("카카오"),
    NAVER("네이버");

    private final String description;

    AuthProvider(String description) {
        this.description = description;
    }

    public static AuthProvider fromString(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return LOCAL;
        }

        // 테이블에 "mayer"라고 적혀있지만 "naver"로 처리
        if ("mayer".equalsIgnoreCase(provider)) {
            return NAVER;
        }

        try {
            return AuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOCAL; // 기본값
        }
    }

    public static boolean isValidProvider(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return false;
        }

        if ("mayer".equalsIgnoreCase(provider)) {
            return true;
        }

        try {
            AuthProvider.valueOf(provider.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}