package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportType;
import me.shinsunyoung.projectweatherly.member.dto.request.*;

import me.shinsunyoung.projectweatherly.member.service.MyPageService;

import me.shinsunyoung.projectweatherly.member.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Tag(name = "마이페이지 컨트롤러", description = "마이페이지 관련 기능 페이지")
public class MyPageController {

    private final MyPageService myPageService;
    private final ReportService reportService;
    private static final String UPLOAD_DIR = "./uploads/";

    // 마이페이지 메인
    @GetMapping("/me")
    @Operation(summary = "마이페이지 메인", description = "마이페이지 메인을 표시합니다.")
    public String getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        String email = userDetails.getUsername();
        var memberResponse = myPageService.getMyPageInfo(email);
        model.addAttribute("myPage", memberResponse);
        return "mypage/main";
    }

    // 프로필 수정 페이지
    @GetMapping("/profile/edit")
    @Operation(summary = "프로필 수정 페이지", description = "프로필 수정 폼을 표시합니다.")
    public String showEditProfilePage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        String email = userDetails.getUsername();
        var response = myPageService.getMyPageInfo(email);

        UpdateMemberRequest updateRequest = new UpdateMemberRequest();
        updateRequest.setNickname(response.getNickname());
        updateRequest.setProfileImage(response.getProfileImage());

        model.addAttribute("myPage", response);
        model.addAttribute("updateRequest", updateRequest);
        return "mypage/edit-profile";
    }

    // 프로필 수정 처리
    @PostMapping("/profile/edit")
    @Operation(summary = "프로필 수정 처리", description = "프로필을 수정합니다.")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateMemberRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        if (bindingResult.hasErrors()) {
            String email = userDetails.getUsername();
            var response = myPageService.getMyPageInfo(email);
            model.addAttribute("myPage", response);
            return "mypage/edit-profile";
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.updateMemberForMyPage(email, request);

            redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/profile/edit";
        }

        return "redirect:/mypage/me";
    }

    // 비밀번호 변경 페이지
    @GetMapping("/password/change")
    @Operation(summary = "비밀번호 변경 페이지", description = "비밀번호 변경 폼을 표시합니다.")
    public String showChangePasswordPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        model.addAttribute("passwordRequest", new UpdatePasswordRequest());
        return "mypage/change-password";
    }

    // 비밀번호 변경 처리
    @PostMapping("/password/change")
    @Operation(summary = "비밀번호 변경 처리", description = "비밀번호를 변경합니다.")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdatePasswordRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        if (bindingResult.hasErrors()) {
            return "mypage/change-password";
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.updatePassword(email, request);

            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
            return "redirect:/mypage/me";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "mypage/change-password";
        }
    }

    // 알림 설정 페이지
    @GetMapping("/notifications")
    @Operation(summary = "알림 설정 페이지", description = "알림 설정 폼을 표시합니다.")
    public String showNotificationsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        String email = userDetails.getUsername();
        var response = myPageService.getMyPageInfo(email);

        UpdateNotificationRequest notificationRequest = new UpdateNotificationRequest();
        notificationRequest.setBoardNotificationAgree(response.getBoardNotificationAgree());
        notificationRequest.setWeatherAlertAgree(response.getWeatherAlertAgree());

        model.addAttribute("notificationRequest", notificationRequest);
        model.addAttribute("myPage", response);
        return "mypage/notifications";
    }

    // 알림 설정 처리
    @PostMapping("/notifications")
    @Operation(summary = "알림 설정 처리", description = "알림 설정을 업데이트합니다.")
    public String updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateNotificationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        if (bindingResult.hasErrors()) {
            return "mypage/notifications";
        }

        String email = userDetails.getUsername();
        var response = myPageService.updateNotificationSettings(email, request);

        redirectAttributes.addFlashAttribute("message", "알림 설정이 업데이트되었습니다.");
        return "redirect:/mypage/me";
    }

    // 프로필 이미지 업로드 처리
    @PostMapping("/profile-image/upload")
    @Operation(summary = "프로필 이미지 업로드 처리", description = "프로필 이미지를 업로드합니다.")
    public String uploadProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "파일이 비어있습니다.");
            return "redirect:/mypage/profile/edit";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            redirectAttributes.addFlashAttribute("error", "올바른 파일 형식이 아닙니다.");
            return "redirect:/mypage/profile/edit";
        }

        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        boolean isValidExtension = false;
        for (String ext : allowedExtensions) {
            if (ext.equals(fileExtension)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            redirectAttributes.addFlashAttribute("error",
                    "지원하지 않는 파일 형식입니다. JPG, JPEG, PNG, GIF, WebP만 업로드 가능합니다.");
            return "redirect:/mypage/profile/edit";
        }

        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxFileSize) {
            redirectAttributes.addFlashAttribute("error", "파일 크기는 5MB를 초과할 수 없습니다.");
            return "redirect:/mypage/profile/edit";
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                log.error("디렉토리 생성 실패: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("error", "디렉토리 생성 중 오류가 발생했습니다.");
                return "redirect:/mypage/profile/edit";
            }
        }

        String newFilename = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(newFilename);

        try {
            Files.copy(file.getInputStream(), filePath);
            log.info("프로필 이미지 저장 성공: {}", newFilename);
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "파일 저장 중 오류가 발생했습니다.");
            return "redirect:/mypage/profile/edit";
        }

        // 이미지 URL 생성 및 멤버 정보 업데이트
        String imageUrl = "/uploads/" + newFilename;
        String email = userDetails.getUsername();

        UpdateMemberRequest updateRequest = new UpdateMemberRequest();
        updateRequest.setProfileImage(imageUrl);
        myPageService.updateMemberForMyPage(email, updateRequest);

        redirectAttributes.addFlashAttribute("message", "프로필 이미지가 업로드되었습니다.");
        return "redirect:/mypage/me";
    }

    // 약관 동의 수정 페이지
    @GetMapping("/agreements")
    @Operation(summary = "약관 동의 수정 페이지", description = "약관 동의 수정 폼을 표시합니다.")
    public String showAgreementsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        String email = userDetails.getUsername();
        var response = myPageService.getMyPageInfo(email);

        UpdateAgreementRequest agreementRequest = new UpdateAgreementRequest();
        agreementRequest.setTermsOfServiceAgree(response.getTermsOfServiceAgree());
        agreementRequest.setPrivacyPolicyAgree(response.getPrivacyPolicyAgree());

        model.addAttribute("agreementRequest", agreementRequest);
        model.addAttribute("myPage", response);
        return "mypage/agreements";
    }

    // 약관 동의 수정 처리
    @PostMapping("/agreements")
    @Operation(summary = "약관 동의 수정 처리", description = "약관 동의 여부를 수정합니다.")
    public String updateAgreements(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateAgreementRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        if (bindingResult.hasErrors()) {
            return "mypage/agreements";
        }

        String email = userDetails.getUsername();
        var response = myPageService.updateAgreementForMyPage(email, request);

        redirectAttributes.addFlashAttribute("message", "약관 동의가 수정되었습니다.");
        return "redirect:/mypage/me";
    }

    // 내 게시물 보기
    @GetMapping("/posts")
    @Operation(summary = "내 게시물 보기", description = "내가 작성한 커뮤니티 게시물을 표시합니다.")
    public String showMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        String email = userDetails.getUsername();
        var response = myPageService.getMyPageInfo(email);

        model.addAttribute("myPosts", response.getMyCommunityPosts());
        model.addAttribute("postCount", response.getPostCount());
        return "mypage/my-posts";
    }

    // 게시물 삭제
    @PostMapping("/posts/{postId}/delete")
    @Operation(summary = "내 게시물 삭제", description = "내가 작성한 게시물을 삭제합니다.")
    public String deleteMyPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "error/unauthorized";
        }

        try {
            String email = userDetails.getUsername();
            myPageService.deleteMyPost(email, postId);

            redirectAttributes.addFlashAttribute("message", "게시물이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("게시물 삭제 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "게시물 삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/posts";
    }

    // 신고한 게시물 보기
    @GetMapping("/reports")
    @Operation(summary = "신고한 게시물 보기", description = "내가 신고한 게시물을 표시합니다.")
    public String showMyReports(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        String email = userDetails.getUsername();
        var response = myPageService.getMyPageInfo(email);

        model.addAttribute("reportedPosts", response.getReportedPosts());
        model.addAttribute("reportCount", response.getReportCount());
        return "mypage/my-reports";
    }

    // 게시물 신고하기 페이지
    @GetMapping("/report/{postId}")
    @Operation(summary = "게시물 신고하기 페이지", description = "게시물 신고 폼을 표시합니다.")
    public String showReportPostPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            Model model) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        model.addAttribute("postId", postId);
        model.addAttribute("reportTypes", ReportType.values());
        model.addAttribute("reportRequest", new ReportRequest());
        return "mypage/report-post";
    }

    // 게시물 신고 처리
    @PostMapping("/report/{postId}")
    @Operation(summary = "게시물 신고 처리", description = "게시물을 신고합니다.")
    public String reportPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            @Valid @ModelAttribute ReportRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "error/unauthorized";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("postId", postId);
            model.addAttribute("reportTypes", ReportType.values());
            return "mypage/report-post";
        }

        try {
            String email = userDetails.getUsername();
            var member = myPageService.getMyPageInfo(email);

            // 게시물 ID 설정
            request.setBoardId(postId);

            reportService.createReport(member.getMemberId(), request);

            redirectAttributes.addFlashAttribute("message", "게시물이 신고되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("신고 처리 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "신고 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/reports";
    }

    // 신고 취소
    @PostMapping("/reports/{reportId}/cancel")
    @Operation(summary = "신고 취소", description = "대기 중인 신고를 취소합니다.")
    public String cancelReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reportId,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "error/unauthorized";
        }

        try {
            String email = userDetails.getUsername();
            myPageService.cancelMyReport(email, reportId);

            redirectAttributes.addFlashAttribute("message", "신고가 취소되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("신고 취소 중 오류 발생: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "신고 취소 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/reports";
    }
}