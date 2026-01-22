package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.dto.CommentRequest;
import me.shinsunyoung.projectweatherly.board.service.CommentService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/community/boards/{boardId}/comments")
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성 (기존 유지)
    @PostMapping
    public String createComment(
            @PathVariable Long boardId,
            @ModelAttribute CommentRequest request,
            @AuthenticationPrincipal UserSecurityDTO user,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) return "redirect:/login";

        try {
            commentService.createComment(boardId, user.getUser().getId(), request);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 작성 실패");
        }
        return "redirect:/community/boards/" + boardId;
    }

    // [수정됨] 댓글 좋아요 (AJAX 대응)
    @PostMapping("/{commentId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserSecurityDTO user) {

        if (user == null || user.getUser() == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> response = new HashMap<>();
        try {
            // 토글 로직 실행 (true: 좋아요, false: 취소)
            boolean liked = commentService.toggleLike(commentId, user.getUser().getId());

            // 최신 개수 가져오기
            int count = commentService.getLikeCount(commentId);

            response.put("success", true);
            response.put("liked", liked);       // 현재 상태 (빨간 하트 여부)
            response.put("likeCount", count);   // 갱신된 숫자

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("좋아요 오류", e);
            response.put("success", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 댓글 삭제 (기존 유지)
    @PostMapping("/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserSecurityDTO user) {
        if (user != null) {
            commentService.deleteComment(commentId, user.getUser().getId());
        }
        return "redirect:/community/boards/" + boardId;
    }
}