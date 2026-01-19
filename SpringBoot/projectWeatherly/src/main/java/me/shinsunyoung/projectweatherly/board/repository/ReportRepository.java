package me.shinsunyoung.projectweatherly.board.repository;

import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 중복 신고 체크 메서드
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporter.id = :reporterId AND r.type = :type AND r.targetId = :targetId")
    boolean existsByReporterIdAndTypeAndTargetId(
            @Param("reporterId") Long reporterId,
            @Param("type") String type,
            @Param("targetId") Long targetId);

    // member.repository.ReportRepository의 메서드들 추가
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    // boardId를 targetId로 변환 (type이 "post"인 경우)
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporter.id = :reporterId AND r.type = 'post' AND r.targetId = :boardId")
    boolean existsByReporterIdAndBoardId(
            @Param("reporterId") Long reporterId,
            @Param("boardId") Long boardId);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.reporter.id = :reporterId")
    int countByReporterId(@Param("reporterId") Long reporterId);

    @Query("SELECT r FROM Report r WHERE r.targetId = :boardId AND r.type = 'post'")
    List<Report> findByBoardId(@Param("boardId") Long boardId);

    Optional<Report> findByIdAndReporterId(Long id, Long reporterId);

    List<Report> findByStatus(String status);
}