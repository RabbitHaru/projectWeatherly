package me.shinsunyoung.projectweatherly.controller.api;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.service.ReportService; // ★ 서비스 추가
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ReportService reportService; // ★ 레포지토리 대신 서비스 사용

    // 1. 회원 상태 변경
    @PostMapping("/members/{memberId}/status")
    public ResponseEntity<String> updateMemberStatus(@PathVariable Long memberId, @RequestParam Boolean isActive) {
        memberRepository.updateMemberStatus(memberId, isActive);
        return ResponseEntity.ok(isActive ? "회원 정지가 해제되었습니다." : "회원이 정지되었습니다.");
    }

    // 2. 게시글 삭제
    @PostMapping("/boards/{boardId}/delete")
    public ResponseEntity<String> deleteBoard(@PathVariable Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        board.setBoardStatus(BoardStatus.DELETED);
        boardRepository.save(board);

        return ResponseEntity.ok("게시글이 삭제 처리되었습니다.");
    }

    // ★ 3. [최종 수정] 신고 처리 API (정지 기능 연동)
    @PostMapping("/reports/{reportId}/process")
    public ResponseEntity<String> processReport(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "0") int banDays // ★ 자바스크립트가 보내는 banDays 받기
    ) {
        // 서비스에게 위임 (상태 변경 + 회원 정지 + 글 삭제 모두 처리)
        reportService.processReport(reportId, ReportStatus.RESOLVED.name(), banDays);

        return ResponseEntity.ok("신고가 처리되었습니다.");
    }
}