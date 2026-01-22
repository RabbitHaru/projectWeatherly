package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.admin.service.AdminService;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.service.ReportService;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    // 관리자 권한 체크 헬퍼
    private void checkAdmin(UserSecurityDTO user) {
        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) {
            throw new IllegalStateException("관리자 권한이 없습니다.");
        }
    }

    // CSRF 토큰 추가 헬퍼
    private void addCsrfToken(HttpServletRequest request, Model model) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) csrfToken = (CsrfToken) request.getAttribute("_csrf");
        if (csrfToken != null) model.addAttribute("_csrf", csrfToken);
    }

    // 1. 대시보드 메인
    @GetMapping({"", "/"})
    public String dashboard(Model model,
                            // [수정] 회원 목록 5개씩 보기 (기존 10 -> 5)
                            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                            @AuthenticationPrincipal UserSecurityDTO user,
                            HttpServletRequest request) {

        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) return "redirect:/";
        addCsrfToken(request, model);

        // 1) 통계 데이터
        Map<String, Long> stats = adminService.getDashboardStats();
        model.addAttribute("pendingReports", stats.getOrDefault("pendingReports", 0L));
        model.addAttribute("totalMembers", stats.getOrDefault("totalMembers", 0L));
        model.addAttribute("todayPosts", stats.getOrDefault("todayPosts", 0L));

        // 2) 회원 목록 (페이징 적용 - 5개씩)
        model.addAttribute("members", adminService.getAllMembers(pageable));

        // 3) 최근 신고 및 처리 내역 (5개 고정)
        Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        model.addAttribute("recentReports", reportService.getAllReports(top5).getContent());
        model.addAttribute("history", reportService.getProcessedReports(top5).getContent());

        return "admin";
    }

    // 2. 회원 정지 처리
    @PostMapping("/members/{id}/suspend")
    @ResponseBody
    public ResponseEntity<String> suspendMember(@PathVariable Long id,
                                                @RequestParam int days,
                                                @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        adminService.suspendMember(id, days);
        return ResponseEntity.ok("Success");
    }

    // 3. 회원 권한 변경
    @PostMapping("/members/{id}/role")
    @ResponseBody
    public ResponseEntity<String> changeMemberRole(@PathVariable Long id,
                                                   @RequestParam String role,
                                                   @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        adminService.changeMemberRole(id, MemberRole.valueOf(role));
        return ResponseEntity.ok("Success");
    }

    // 4. 신고 관리 페이지
    @GetMapping("/reports")
    public String manageReports(Model model,
                                // [수정] 신고 관리 목록 10개씩 보기 (기존 20 -> 10)
                                @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal UserSecurityDTO user,
                                HttpServletRequest request) {
        checkAdmin(user);
        addCsrfToken(request, model);

        model.addAttribute("reports", reportService.getAllReports(pageable));
        model.addAttribute("history", reportService.getProcessedReports(pageable));

        return "admin/reports";
    }

    // 5. 신고 처리
    @PostMapping("/reports/{id}/process")
    @ResponseBody
    public ResponseEntity<String> processReport(@PathVariable Long id,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") int banDays,
                                                @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        if (status == null || status.isEmpty()) status = ReportStatus.RESOLVED.name();

        reportService.processReport(id, status, banDays);
        return ResponseEntity.ok("Success");
    }
}