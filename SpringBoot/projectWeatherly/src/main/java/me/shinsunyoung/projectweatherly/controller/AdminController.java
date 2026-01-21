package me.shinsunyoung.projectweatherly.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.shinsunyoung.projectweatherly.board.service.ReportService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.service.AdminService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    // 관리자 권한 체크
    private void checkAdmin(UserSecurityDTO user) {
        if (user == null || user.getUser().getRole() != MemberRole.ADMIN) {
            throw new IllegalStateException("관리자 권한이 없습니다.");
        }
    }

    // 1. 대시보드 메인 (통계)
    @GetMapping({"", "/"})
    public String dashboard(Model model, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        model.addAttribute("stats", adminService.getDashboardStats());
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

    // 3. 회원 정지/해제 처리 (POST)
    @PostMapping("/members/{id}/status")
    public String updateMemberStatus(@PathVariable Long id, @RequestParam boolean isActive, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        adminService.updateMemberStatus(id, isActive);
        return "redirect:/admin/members";
    }

    // 4. 신고 내역 페이지
    @GetMapping("/reports")
    public String manageReports(Model model,
                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);

        // ReportService에서 전체 신고 목록(페이징) 가져오기
        model.addAttribute("reports", reportService.getAllReports(pageable));

        return "admin/reports";
    }

    // 5. 신고 처리 (승인/반려)
    @PostMapping("/reports/{id}/process")
    public String processReport(@PathVariable Long id, @RequestParam String status, @AuthenticationPrincipal UserSecurityDTO user) {
        checkAdmin(user);
        // ReportService의 processReport 호출 (승인 시 콘텐츠 삭제 로직 포함)
        reportService.processReport(id, status);
        return "redirect:/admin/reports";
    }
}