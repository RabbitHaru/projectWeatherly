package me.shinsunyoung.projectweatherly.controller.api;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;

    // 1. 회원 상태 변경 (정지 <-> 활동중)
    @PostMapping("/members/{memberId}/status")
    public ResponseEntity<String> updateMemberStatus(@PathVariable Long memberId, @RequestParam Boolean isActive) {
        // B담당님이 만들어둔 updateMemberStatus 메서드 활용
        memberRepository.updateMemberStatus(memberId, isActive);
        return ResponseEntity.ok(isActive ? "회원 정지가 해제되었습니다." : "회원이 정지되었습니다.");
    }

    // 2. 게시글 삭제 (물리적 삭제 대신 상태를 DELETED로 변경)
    @PostMapping("/boards/{boardId}/delete")
    public ResponseEntity<String> deleteBoard(@PathVariable Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        board.setBoardStatus(BoardStatus.DELETED); // 상태 변경
        boardRepository.save(board);

        return ResponseEntity.ok("게시글이 삭제 처리되었습니다.");
    }

    // 3. 신고 처리 (상태 변경)
    @PostMapping("/reports/{reportId}/process")
    public ResponseEntity<String> processReport(@PathVariable Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역이 존재하지 않습니다."));

        report.setStatus(ReportStatus.RESOLVED.name()); // 처리 완료로 변경
        reportRepository.save(report);

        return ResponseEntity.ok("신고가 처리 완료되었습니다.");
    }
}