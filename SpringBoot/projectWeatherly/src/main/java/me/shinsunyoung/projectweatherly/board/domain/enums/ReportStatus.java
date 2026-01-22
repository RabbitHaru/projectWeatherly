package me.shinsunyoung.projectweatherly.board.domain.enums;

public enum ReportStatus {
    PENDING,    // 대기 중 (접수됨)
    PROCESSING, // 처리 중
    ACCEPTED,   // 승인됨 (신고 내용이 맞아서 제재 처리됨) ★ 추가됨
    RESOLVED,   // 처리 완료 (단순 삭제 등)
    REJECTED,   // 반려됨 (신고 사유 불충분)
    CANCELLED   // 취소됨
}