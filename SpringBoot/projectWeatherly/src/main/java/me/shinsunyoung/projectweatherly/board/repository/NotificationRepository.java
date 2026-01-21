package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Notification;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member; // Member import 필요
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // [기존 유지] 읽지 않은 알림 개수만 빠르게 조회 (뱃지용)
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    // [★추가됨] 읽지 않은 알림의 '상세 목록'을 최신순으로 조회 (드롭다운 목록용)
    List<Notification> findByReceiverAndIsReadFalseOrderByCreatedDateDesc(Member receiver);

    // [★추가됨] 특정 알림 1개를 '읽음' 상태로 변경 (클릭 시 실행)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);
}