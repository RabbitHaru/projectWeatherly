package me.shinsunyoung.projectweatherly.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.dto.ReportRequest;
import me.shinsunyoung.projectweatherly.board.service.ReportService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller // RestController 아님
@RequestMapping("/community/reports") // API 경로가 아닌 일반 경로 사용
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 접수 처리
     * @ModelAttribute: HTML Form 데이터를 받습니다.
     * HttpServletRequest: 이전 페이지(Referer)로 돌아가기 위해 사용합니다.
     */
    @PostMapping("/create")
    public String createReport(
            @AuthenticationPrincipal UserSecurityDTO user,
            @ModelAttribute ReportRequest request,
            HttpServletRequest httpServletRequest,
            RedirectAttributes redirectAttributes) {

        // 1. 로그인 체크 -> 로그인 페이지로 튕김
        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        // 이전 페이지 주소 가져오기 (신고 후 다시 그 글이나 목록으로 돌아가기 위함)
        String prevPage = httpServletRequest.getHeader("Referer");
        if (prevPage == null) {
            prevPage = "/community"; // 이전 페이지 정보가 없으면 커뮤니티 메인으로
        }

        try {
            // 2. 서비스 호출
            boolean success = reportService.createReport(
                    user.getUser().getId(),
                    request.getType(),
                    request.getTargetId(),
                    request.getReason(),
                    request.getDetails()
            );

            // 3. 결과 메시지 설정
            if (success) {
                redirectAttributes.addFlashAttribute("message", "신고가 정상적으로 접수되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "이미 신고 처리된 항목입니다.");
            }

        } catch (Exception e) {
            log.error("신고 생성 중 오류 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "신고 처리 중 오류가 발생했습니다.");
        }

        // 4. 원래 보던 페이지로 리다이렉트 (새로고침 효과)
        return "redirect:" + prevPage;
    }
}