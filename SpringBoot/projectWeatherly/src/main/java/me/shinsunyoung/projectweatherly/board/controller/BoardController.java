package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.application.BoardService;
import me.shinsunyoung.projectweatherly.board.domain.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.domain.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.domain.dto.BoardUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/community/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 게시글 생성
    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(
            @AuthenticationPrincipal Long memberId,
            @RequestBody BoardRequest request) {
        BoardResponse response = boardService.createBoard(memberId, request);
        return ResponseEntity.ok(response);
    }

    // 게시글 단건 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long boardId) {
        BoardResponse response = boardService.getBoard(boardId);
        return ResponseEntity.ok(response);
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<Page<BoardResponse>> getAllBoards(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoardResponse> responses = boardService.getAllBoards(pageable);
        return ResponseEntity.ok(responses);
    }

    // 게시글 수정
    @PutMapping("/{boardId}")
    public ResponseEntity<BoardResponse> updateBoard(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId,
            @RequestBody BoardUpdateRequest request) {
        BoardResponse response = boardService.updateBoard(boardId, memberId, request);
        return ResponseEntity.ok(response);
    }

    // 게시글 삭제
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Map<String, String>> deleteBoard(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId) {
        boardService.deleteBoard(boardId, memberId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "게시글이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    // 게시글 검증
    @PatchMapping("/{boardId}/verify")
    public ResponseEntity<BoardResponse> verifyBoard(
            @PathVariable Long boardId,
            @RequestParam Boolean isVerified) {
        BoardResponse response = boardService.verifyBoard(boardId, isVerified);
        return ResponseEntity.ok(response);
    }

    // 게시글 좋아요
    @PostMapping("/{boardId}/like")
    public ResponseEntity<BoardResponse> likeBoard(@PathVariable Long boardId) {
        BoardResponse response = boardService.likeBoard(boardId);
        return ResponseEntity.ok(response);
    }

    // 게시글 검색
    @GetMapping("/search")
    public ResponseEntity<Page<BoardResponse>> searchBoards(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoardResponse> responses = boardService.searchBoards(keyword, pageable);
        return ResponseEntity.ok(responses);
    }

    // 날씨 상태별 조회
    @GetMapping("/weather/{weatherCondition}")
    public ResponseEntity<Page<BoardResponse>> getBoardsByWeatherCondition(
            @PathVariable String weatherCondition,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoardResponse> responses = boardService.getBoardsByWeatherCondition(weatherCondition, pageable);
        return ResponseEntity.ok(responses);
    }

    // 인기글 조회
    @GetMapping("/popular")
    public ResponseEntity<Page<BoardResponse>> getPopularBoards(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoardResponse> responses = boardService.getPopularBoards(pageable);
        return ResponseEntity.ok(responses);
    }

    // 최신글 조회
    @GetMapping("/recent")
    public ResponseEntity<Page<BoardResponse>> getRecentBoards(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoardResponse> responses = boardService.getRecentBoards(pageable);
        return ResponseEntity.ok(responses);
    }
}