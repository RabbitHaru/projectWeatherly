package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporter.id = :reporterId AND r.type = :type AND r.targetId = :targetId")
    boolean existsByReporterIdAndTypeAndTargetId(
            @Param("reporterId") Long reporterId,
            @Param("type") String type,
            @Param("targetId") Long targetId);

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    // 대기 중인 신고 조회
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    // ★ [추가] 처리된 신고 내역 조회 (PENDING이 아닌 것들 = 처리된 것들)
    @Query("SELECT r FROM Report r WHERE r.status <> :status ORDER BY r.processedAt DESC, r.createdAt DESC")
    Page<Report> findByStatusNotOrderByProcessedAtDesc(@Param("status") ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}