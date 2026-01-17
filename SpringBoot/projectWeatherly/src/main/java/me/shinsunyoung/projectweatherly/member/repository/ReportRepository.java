package me.shinsunyoung.projectweatherly.report.repository;

import me.shinsunyoung.projectweatherly.report.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    // ✅ 누락된 메서드들 추가
    boolean existsByReporterIdAndBoardId(Long reporterId, Long boardId);

    int countByReporterId(Long reporterId);

    List<Report> findByBoardId(Long boardId);

    Optional<Report> findByIdAndReporterId(Long id, Long reporterId);

    List<Report> findByStatus(String status);
}