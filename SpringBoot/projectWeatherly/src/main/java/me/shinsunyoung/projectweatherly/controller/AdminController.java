package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.admin.service.AdminService;
import me.shinsunyoung.projectweatherly.board.service.ReportService;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.member.dto.response.ReportResponse;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;

import org.springframework.data.domain.Page;
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

    private void checkAdmin(UserSecurityDTO user) {
        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) {
            throw new IllegalStateException("관리자 권한이 없습니다.");
        }
    }

    private void addCsrfToken(HttpServletRequest request, Model model) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) csrfToken = (CsrfToken) request.getAttribute("_csrf");
        if (csrfToken != null) model.addAttribute("_csrf", csrfToken);
    }

    // 1. 대시보드 메인
    @GetMapping({"", "/"})
    public String dashboard(Model model, @AuthenticationPrincipal UserSecurityDTO user, HttpServletRequest request) {
        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) return "redirect:/";
        addCsrfToken(request, model);

        // 통계 데이터 (pendingReports 등)
        Map<String, Long> stats = adminService.getDashboardStats();
        model.addAttribute("pendingReports", stats.getOrDefault("pendingReports", 0L));
        model.addAttribute("totalMembers", stats.getOrDefault("totalMembers", 0L));
        model.addAttribute("todayPosts", stats.getOrDefault("todayPosts", 0L));

        Pageable top5 = Pageable.ofSize(5);
        model.addAttribute("recentMembers", adminService.getAllMembers(top5));
        model.addAttribute("recentReports", reportService.getAllReports(top5).getContent());

        // ★ [필수] HTML에서 ${history}를 쓰므로 반드시 넣어줘야 함
        model.addAttribute("history", reportService.getProcessedReports(top5).getContent());

        return "admin";
    }

    @GetMapping("/members")
    public String manageMembers(Model model, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal UserSecurityDTO user, HttpServletRequest request) {
        checkAdmin(user);
        addCsrfToken(request, model);
        model.addAttribute("members", adminService.getAllMembers(pageable));
        return "admin/members";
    }

    @PostMapping("/members/{id}/suspend")
    @ResponseBody
    public ResponseEntity<String> suspendMember(@PathVariable Long id, @RequestParam int days, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        adminService.suspendMember(id, days);
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/reports")
    public String manageReports(Model model,
                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal UserSecurityDTO user,
                                HttpServletRequest request) {
        checkAdmin(user);
        addCsrfToken(request, model);

        model.addAttribute("reports", reportService.getAllReports(pageable));
        model.addAttribute("history", reportService.getProcessedReports(pageable));

        return "admin/reports";
    }

    @PostMapping("/reports/{id}/process")
    @ResponseBody
    public ResponseEntity<String> processReport(@PathVariable Long id, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int banDays, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        if (status == null || status.isEmpty()) status = ReportStatus.RESOLVED.name();
        reportService.processReport(id, status, banDays);
        return ResponseEntity.ok("Success");
    }
}