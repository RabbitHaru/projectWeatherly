package me.shinsunyoung.projectweatherly.member.domain.enums;



public enum NotificationType {
    BOARD_NOTIFICATION("게시판 알림", "새로운 게시글, 댓글, 좋아요 등 게시판 활동 알림"),
    WEATHER_ALERT("기상특보 알림", "날씨 경보, 기상 특보, 이상 기후 알림"),
    PROMOTION("프로모션 알림", "이벤트, 할인, 신규 서비스 안내"),
    SYSTEM("시스템 알림", "공지사항, 시스템 점검, 약관 변경 안내");

    private final String title;
    private final String description;

    NotificationType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationType fromString(String type) {
        for (NotificationType notificationType : NotificationType.values()) {
            if (notificationType.name().equalsIgnoreCase(type)) {
                return notificationType;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + type);
    }
}