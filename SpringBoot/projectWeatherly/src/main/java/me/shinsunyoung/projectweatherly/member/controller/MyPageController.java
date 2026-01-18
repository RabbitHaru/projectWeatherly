package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Tag(name = "마이페이지 컨트롤러", description = "마이페이지 관련 기능 페이지")
public class MyPageController {

    private final MyPageService myPageService;
    private final ReportService reportService;

    // 상수 정의
    private static final String UPLOAD_DIR = "./uploads/";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String REDIRECT_MYPAGE = "redirect:/mypage";
    private static final String ERROR_UNAUTHORIZED = "redirect:/login";

    // 마이페이지 메인
    @GetMapping
    @Operation(summary = "마이페이지 메인", description = "마이페이지 메인을 표시합니다.")
    public String getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            var memberResponse = myPageService.getMyPageInfo(email);

            // UpdateMemberRequest가 null이 아닐 때만 생성
            UpdateMemberRequest updateRequest = null;
            if (memberResponse != null) {
                updateRequest = UpdateMemberRequest.builder()
                        .nickname(memberResponse.getNickname())
                        .profileImage(memberResponse.getProfileImage())
                        .build();
            }

            model.addAttribute("myPage", memberResponse);
            model.addAttribute("updateRequest", updateRequest);

            // requestURI 추가 (헤더에서 사용)
            String requestURI = request.getRequestURI();
            model.addAttribute("requestURI", requestURI);

            return "mypage";
        } catch (Exception e) {
            log.error("마이페이지 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("error", "정보를 불러오는 중 오류가 발생했습니다.");
            return ERROR_UNAUTHORIZED;
        }
    }

    // 프로필 수정 페이지
    @GetMapping("/profile/edit")
    @Operation(summary = "프로필 수정 페이지", description = "프로필 수정 폼을 표시합니다.")
    public String showEditProfilePage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.getMyPageInfo(email);

            UpdateMemberRequest updateRequest = UpdateMemberRequest.builder()
                    .nickname(response.getNickname())
                    .profileImage(response.getProfileImage())
                    .build();

            model.addAttribute("myPage", response);
            model.addAttribute("updateRequest", updateRequest);

            // requestURI 추가
            model.addAttribute("requestURI", request.getRequestURI());

            return "mypage/edit-profile";
        } catch (Exception e) {
            log.error("프로필 수정 페이지 로딩 중 오류 발생: {}", e.getMessage(), e);
            return REDIRECT_MYPAGE;
        }
    }

    // 프로필 수정 처리
    @PostMapping("/profile/edit")
    @Operation(summary = "프로필 수정 처리", description = "프로필을 수정합니다.")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateMemberRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest servletRequest) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        if (bindingResult.hasErrors()) {
            try {
                String email = userDetails.getUsername();
                var response = myPageService.getMyPageInfo(email);
                model.addAttribute("myPage", response);

                // requestURI 추가
                model.addAttribute("requestURI", servletRequest.getRequestURI());

                return "mypage/edit-profile";
            } catch (Exception e) {
                log.error("프로필 수정 중 오류 발생: {}", e.getMessage(), e);
                redirectAttributes.addFlashAttribute("error", "프로필 수정 중 오류가 발생했습니다.");
                return REDIRECT_MYPAGE;
            }
        }

        try {
            String email = userDetails.getUsername();
            myPageService.updateMemberForMyPage(email, request);
            redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("프로필 수정 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/profile/edit";
        } catch (Exception e) {
            log.error("프로필 수정 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "프로필 수정 중 오류가 발생했습니다.");
            return "redirect:/mypage/profile/edit";
        }

        return REDIRECT_MYPAGE;
    }

    // 비밀번호 변경 페이지
    @GetMapping("/password/change")
    @Operation(summary = "비밀번호 변경 페이지", description = "비밀번호 변경 폼을 표시합니다.")
    public String showChangePasswordPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        model.addAttribute("passwordRequest", new UpdatePasswordRequest());

        // requestURI 추가
        model.addAttribute("requestURI", request.getRequestURI());

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
            RedirectAttributes redirectAttributes,
            HttpServletRequest servletRequest) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        if (bindingResult.hasErrors()) {
            // requestURI 추가
            model.addAttribute("requestURI", servletRequest.getRequestURI());
            return "mypage/change-password";
        }

        try {
            String email = userDetails.getUsername();
            myPageService.updatePassword(email, request);
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
            return REDIRECT_MYPAGE;
        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 변경 실패: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            // requestURI 추가
            model.addAttribute("requestURI", servletRequest.getRequestURI());
            return "mypage/change-password";
        } catch (Exception e) {
            log.error("비밀번호 변경 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("error", "비밀번호 변경 중 오류가 발생했습니다.");
            // requestURI 추가
            model.addAttribute("requestURI", servletRequest.getRequestURI());
            return "mypage/change-password";
        }
    }

    // 알림 설정 페이지
    @GetMapping("/notifications")
    @Operation(summary = "알림 설정 페이지", description = "알림 설정 폼을 표시합니다.")
    public String showNotificationsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.getMyPageInfo(email);

            UpdateNotificationRequest notificationRequest = UpdateNotificationRequest.builder()
                    .boardNotificationAgree(response.getBoardNotificationAgree())
                    .weatherAlertAgree(response.getWeatherAlertAgree())
                    .build();

            model.addAttribute("notificationRequest", notificationRequest);
            model.addAttribute("myPage", response);

            // requestURI 추가
            model.addAttribute("requestURI", request.getRequestURI());

            return "mypage/notifications";
        } catch (Exception e) {
            log.error("알림 설정 페이지 로딩 중 오류 발생: {}", e.getMessage(), e);
            return REDIRECT_MYPAGE;
        }
    }

    // 알림 설정 처리
    @PostMapping("/notifications")
    @Operation(summary = "알림 설정 처리", description = "알림 설정을 업데이트합니다.")
    public String updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateNotificationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest servletRequest) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        if (bindingResult.hasErrors()) {
            // requestURI 추가
            model.addAttribute("requestURI", servletRequest.getRequestURI());
            return "mypage/notifications";
        }

        try {
            String email = userDetails.getUsername();
            myPageService.updateNotificationSettings(email, request);
            redirectAttributes.addFlashAttribute("message", "알림 설정이 업데이트되었습니다.");
        } catch (Exception e) {
            log.error("알림 설정 업데이트 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "알림 설정 업데이트 중 오류가 발생했습니다.");
        }

        return REDIRECT_MYPAGE;
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
            return ERROR_UNAUTHORIZED;
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

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            redirectAttributes.addFlashAttribute("error",
                    "지원하지 않는 파일 형식입니다. JPG, JPEG, PNG, GIF, WebP만 업로드 가능합니다.");
            return "redirect:/mypage/profile/edit";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            redirectAttributes.addFlashAttribute("error", "파일 크기는 5MB를 초과할 수 없습니다.");
            return "redirect:/mypage/profile/edit";
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.error("디렉토리 생성 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "디렉토리 생성 중 오류가 발생했습니다.");
            return "redirect:/mypage/profile/edit";
        }

        String newFilename = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(newFilename);

        try {
            Files.copy(file.getInputStream(), filePath);
            log.info("프로필 이미지 저장 성공: {}", newFilename);
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "파일 저장 중 오류가 발생했습니다.");
            return "redirect:/mypage/profile/edit";
        }

        // 이미지 URL 생성 및 멤버 정보 업데이트
        String imageUrl = "/uploads/" + newFilename;
        String email = userDetails.getUsername();

        try {
            UpdateMemberRequest updateRequest = UpdateMemberRequest.builder()
                    .profileImage(imageUrl)
                    .build();
            myPageService.updateMemberForMyPage(email, updateRequest);
            redirectAttributes.addFlashAttribute("message", "프로필 이미지가 업로드되었습니다.");
        } catch (Exception e) {
            log.error("프로필 이미지 업데이트 중 오류 발생: {}", e.getMessage(), e);
            // 업로드된 파일 삭제 시도
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                log.error("실패한 파일 삭제 중 오류: {}", ex.getMessage(), ex);
            }
            redirectAttributes.addFlashAttribute("error", "프로필 이미지 업데이트 중 오류가 발생했습니다.");
        }

        return REDIRECT_MYPAGE;
    }

    // 약관 동의 수정 페이지
    @GetMapping("/agreements")
    @Operation(summary = "약관 동의 수정 페이지", description = "약관 동의 수정 폼을 표시합니다.")
    public String showAgreementsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.getMyPageInfo(email);

            UpdateAgreementRequest agreementRequest = UpdateAgreementRequest.builder()
                    .termsOfServiceAgree(response.getTermsOfServiceAgree())
                    .privacyPolicyAgree(response.getPrivacyPolicyAgree())
                    .build();

            model.addAttribute("agreementRequest", agreementRequest);
            model.addAttribute("myPage", response);

            // requestURI 추가
            model.addAttribute("requestURI", request.getRequestURI());

            return "mypage/agreements";
        } catch (Exception e) {
            log.error("약관 동의 페이지 로딩 중 오류 발생: {}", e.getMessage(), e);
            return REDIRECT_MYPAGE;
        }
    }

    // 약관 동의 수정 처리
    @PostMapping("/agreements")
    @Operation(summary = "약관 동의 수정 처리", description = "약관 동의 여부를 수정합니다.")
    public String updateAgreements(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateAgreementRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest servletRequest) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        if (bindingResult.hasErrors()) {
            // requestURI 추가
            model.addAttribute("requestURI", servletRequest.getRequestURI());
            return "mypage/agreements";
        }

        try {
            String email = userDetails.getUsername();
            myPageService.updateAgreementForMyPage(email, request);
            redirectAttributes.addFlashAttribute("message", "약관 동의가 수정되었습니다.");
        } catch (Exception e) {
            log.error("약관 동의 수정 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "약관 동의 수정 중 오류가 발생했습니다.");
        }

        return REDIRECT_MYPAGE;
    }

    // 내 게시물 보기
    @GetMapping("/posts")
    @Operation(summary = "내 게시물 보기", description = "내가 작성한 커뮤니티 게시물을 표시합니다.")
    public String showMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.getMyPageInfo(email);

            model.addAttribute("myPosts", response.getMyCommunityPosts());
            model.addAttribute("postCount", response.getPostCount());

            // requestURI 추가
            model.addAttribute("requestURI", request.getRequestURI());

            return "mypage/my-posts";
        } catch (Exception e) {
            log.error("내 게시물 조회 중 오류 발생: {}", e.getMessage(), e);
            return REDIRECT_MYPAGE;
        }
    }

    // 게시물 삭제
    @PostMapping("/posts/{postId}/delete")
    @Operation(summary = "내 게시물 삭제", description = "내가 작성한 게시물을 삭제합니다.")
    public String deleteMyPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            myPageService.deleteMyPost(email, postId);
            redirectAttributes.addFlashAttribute("message", "게시물이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("게시물 삭제 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("게시물 삭제 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "게시물 삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/posts";
    }

    // 신고한 게시물 보기
    @GetMapping("/reports")
    @Operation(summary = "신고한 게시물 보기", description = "내가 신고한 게시물을 표시합니다.")
    public String showMyReports(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            var response = myPageService.getMyPageInfo(email);

            model.addAttribute("reportedPosts", response.getReportedPosts());
            model.addAttribute("reportCount", response.getReportCount());

            // requestURI 추가
            model.addAttribute("requestURI", request.getRequestURI());

            return "mypage/my-reports";
        } catch (Exception e) {
            log.error("신고한 게시물 조회 중 오류 발생: {}", e.getMessage(), e);
            return REDIRECT_MYPAGE;
        }
    }

    // 게시물 신고하기 페이지
    @GetMapping("/report/{postId}")
    @Operation(summary = "게시물 신고하기 페이지", description = "게시물 신고 폼을 표시합니다.")
    public String showReportPostPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            Model model,
            HttpServletRequest request) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        model.addAttribute("postId", postId);
        model.addAttribute("reportTypes", ReportType.values());
        model.addAttribute("reportRequest", new ReportRequest());

        // requestURI 추가
        model.addAttribute("requestURI", request.getRequestURI());

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
            RedirectAttributes redirectAttributes,
            HttpServletRequest servletRequest) {  // HttpServletRequest 추가

        if (userDetails == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return ERROR_UNAUTHORIZED;
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("postId", postId);
            model.addAttribute("reportTypes", ReportType.values());

            // requestURI 추가
            model.addAttribute("requestURI", servletRequest.getRequestURI());

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
            log.warn("게시물 신고 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("신고 처리 중 오류 발생: {}", e.getMessage(), e);
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
            return ERROR_UNAUTHORIZED;
        }

        try {
            String email = userDetails.getUsername();
            myPageService.cancelMyReport(email, reportId);
            redirectAttributes.addFlashAttribute("message", "신고가 취소되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("신고 취소 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("신고 취소 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "신고 취소 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/reports";
    }
}