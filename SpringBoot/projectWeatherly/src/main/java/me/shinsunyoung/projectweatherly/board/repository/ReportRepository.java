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

    // 1. 중복 신고 체크
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporter.id = :reporterId AND r.type = :type AND r.targetId = :targetId")
    boolean existsByReporterIdAndTypeAndTargetId(
            @Param("reporterId") Long reporterId,
            @Param("type") String type,
            @Param("targetId") Long targetId);

    // 2. 특정 사용자가 신고한 목록
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    // 3. 특정 게시글에 대한 신고 여부 확인
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reporter.id = :reporterId AND r.type = 'post' AND r.targetId = :boardId")
    boolean existsByReporterIdAndBoardId(
            @Param("reporterId") Long reporterId,
            @Param("boardId") Long boardId);

    // 4. 사용자가 신고한 횟수
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reporter.id = :reporterId")
    int countByReporterId(@Param("reporterId") Long reporterId);

    // 5. 특정 게시글의 신고 목록
    @Query("SELECT r FROM Report r WHERE r.targetId = :boardId AND r.type = 'post'")
    List<Report> findByBoardId(@Param("boardId") Long boardId);

    // 6. [관리자용] 전체 신고 목록 페이징 (기본 메서드지만 명시)
    Page<Report> findAll(Pageable pageable);

    // 7. [대시보드용] 상태별 신고 개수 (예: PENDING 개수 세기)
    long countByStatus(ReportStatus status);
}