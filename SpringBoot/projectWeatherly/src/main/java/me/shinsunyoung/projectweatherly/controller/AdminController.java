package me.shinsunyoung.projectweatherly.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.admin.service.AdminService;
import me.shinsunyoung.projectweatherly.board.service.ReportService;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.member.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // 권한 체크 로직
    private void checkAdmin(UserSecurityDTO user) {
        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) {
            throw new IllegalStateException("관리자 권한이 없습니다.");
        }
    }

    // 1. 대시보드 메인
    @GetMapping({"", "/"})
    public String dashboard(Model model, @AuthenticationPrincipal UserSecurityDTO user) {

        checkAdmin(user);
        model.addAttribute("stats", adminService.getDashboardStats());

        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) {
            return "redirect:/";
        }

        Map<String, Long> stats = adminService.getDashboardStats();
        model.addAttribute("pendingReports", stats.get("pendingReports"));
        model.addAttribute("totalMembers", stats.get("totalMembers"));
        model.addAttribute("todayPosts", stats.get("todayPosts"));

        // 최근 가입 회원 (Member Entity 사용)
        Pageable top5 = Pageable.ofSize(5);
        model.addAttribute("recentMembers", adminService.getAllMembers(top5));

        // ★ [수정됨] 서비스에서 이미 DTO로 변환되어 오므로 그대로 사용!
        Page<ReportResponse> reportPage = reportService.getAllReports(top5);
        model.addAttribute("recentReports", reportPage.getContent()); // List 형태로 전달


        return "admin";
    }

    // 2. 회원 관리 페이지
    @GetMapping("/members")
    public String manageMembers(Model model,
                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        model.addAttribute("members", adminService.getAllMembers(pageable));
        return "/members";
    }

    // 3. 회원 정지/해제 API
    @PostMapping("/members/{id}/suspend")
    @ResponseBody
    public ResponseEntity<String> suspendMember(@PathVariable Long id, @RequestParam int days, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        adminService.suspendMember(id, days);
        return ResponseEntity.ok("Success");
    }

    // 4. 신고 내역 페이지
    @GetMapping("/reports")
    public String manageReports(Model model,
                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);

        // ★ [수정됨] 서비스에서 DTO로 받아서 바로 전달
        model.addAttribute("reports", reportService.getAllReports(pageable));

        return "admin/reports";
    }

    // 5. 신고 처리 API
    @PostMapping("/reports/{id}/process")
    @ResponseBody
    public ResponseEntity<String> processReport(@PathVariable Long id, @RequestParam String status, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        reportService.processReport(id, status);
        return ResponseEntity.ok("Success");
    }
}