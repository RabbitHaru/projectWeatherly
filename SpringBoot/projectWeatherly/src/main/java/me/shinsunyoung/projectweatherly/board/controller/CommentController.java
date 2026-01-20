package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.dto.CommentRequest;
import me.shinsunyoung.projectweatherly.board.service.CommentService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller // RestController 대신 일반 Controller 사용
@RequiredArgsConstructor
@RequestMapping("/community/boards/{boardId}/comments") // URL 구조를 게시글 하위로 잡음
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성
     * @RestController와 달리 @RequestBody(JSON)가 아닌 @ModelAttribute(Form 데이터)를 받습니다.
     * 처리가 끝나면 JSON을 리턴하지 않고, 게시글 상세 페이지로 리다이렉트합니다.
     */
    @PostMapping
    public String createComment(
            @PathVariable Long boardId,
            @ModelAttribute CommentRequest request,
            @AuthenticationPrincipal UserSecurityDTO user,
            RedirectAttributes redirectAttributes) {

        // 로그인 안 된 경우 로그인 페이지로 이동
        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            // 유효성 검사
            if (request.getContent() == null || request.getContent().trim().length() < 2) {
                redirectAttributes.addFlashAttribute("errorMessage", "댓글은 2자 이상 입력해주세요.");
                return "redirect:/community/boards/" + boardId;
            }

            // 서비스 호출
            commentService.createComment(boardId, user.getUser().getId(), request);

            // 성공 메시지 전달 (1회성)
            redirectAttributes.addFlashAttribute("message", "댓글이 등록되었습니다.");

        } catch (Exception e) {
            log.error("댓글 작성 실패", e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 작성 중 오류가 발생했습니다.");
        }

        // 처리 후 원래 게시글 페이지로 돌아감 (새로고침 효과)
        return "redirect:/community/boards/" + boardId;
    }

    /**
     * 댓글 좋아요
     * AJAX가 아닌 페이지 이동 방식으로 처리합니다.
     */
    @PostMapping("/{commentId}/like")
    public String likeComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserSecurityDTO user,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            boolean liked = commentService.toggleLike(commentId, user.getUser().getId());
            if (liked) {
                redirectAttributes.addFlashAttribute("message", "댓글에 좋아요를 눌렀습니다.");
            } else {
                redirectAttributes.addFlashAttribute("message", "좋아요를 취소했습니다.");
            }
        } catch (Exception e) {
            log.error("댓글 좋아요 처리 실패", e);
        }

        return "redirect:/community/boards/" + boardId;
    }

    /**
     * 댓글 삭제 (필요할 경우를 대비해 추가해둠)
     */
    @PostMapping("/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserSecurityDTO user,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            // 서비스에 댓글 삭제 메서드가 있다고 가정 (deleteComment)
            commentService.deleteComment(commentId, user.getUser().getId());
            redirectAttributes.addFlashAttribute("message", "댓글이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("댓글 삭제 실패", e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 삭제에 실패했습니다.");
        }

        return "redirect:/community/boards/" + boardId;
    }
}