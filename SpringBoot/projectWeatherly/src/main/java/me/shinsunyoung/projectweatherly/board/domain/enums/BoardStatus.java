package me.shinsunyoung.projectweatherly.board.domain.enums;

public enum BoardStatus {
    ACTIVE("ACTIVE", "활성"),
    REPORTED("REPORTED", "신고됨"),
    DELETED("DELETED", "삭제됨");

    private final String code;
    private final String description;

    BoardStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}