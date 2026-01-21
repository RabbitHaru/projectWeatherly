package me.shinsunyoung.projectweatherly.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data // Getter, Setter 자동 생성 (이게 있어야 .getPendingReports() 호출 가능)
@Builder
public class DashboardStatsDTO {
    private long totalMembers;    // 총 회원 수
    private long pendingReports;  // 처리 대기중인 신고 수
    private long todayPosts;      // 오늘 올라온 게시글 수
}