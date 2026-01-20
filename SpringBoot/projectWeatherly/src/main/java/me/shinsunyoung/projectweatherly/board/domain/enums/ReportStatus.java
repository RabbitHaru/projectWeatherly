// ReportStatus.java (열거형)
package me.shinsunyoung.projectweatherly.board.domain.enums;

public enum ReportStatus {
    PENDING,    // 대기 중
    PROCESSING, // 처리 중
    RESOLVED,   // 처리 완료
    REJECTED,   // 기각
    CANCELLED   // 취소
}