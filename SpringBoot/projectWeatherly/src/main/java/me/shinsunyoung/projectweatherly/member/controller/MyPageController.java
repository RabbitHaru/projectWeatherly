package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.service.MyPageService;
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
@Tag(name = "마이페이지 컨트롤러", description = "마이페이지 관련 기능")
public class MyPageController {

    private final MyPageService myPageService;

    // 파일 업로드 설정
    private static final String UPLOAD_DIR = "./uploads/";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * 마이페이지 메인 조회
     * - 프로필 정보, 작성 글, 기타 설정 정보를 모두 모델에 담아 뷰로 전달
     */
    @GetMapping
    @Operation(summary = "마이페이지 메인", description = "마이페이지 메인을 표시합니다.")
    public String getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            String email = userDetails.getUsername();
            MyPageResponse memberResponse = myPageService.getMyPageInfo(email);

            model.addAttribute("myPage", memberResponse);
            model.addAttribute("requestURI", request.getRequestURI());

            // 폼 바인딩용 빈 객체들 추가 (에러 방지)
            if (!model.containsAttribute("passwordRequest")) {
                model.addAttribute("passwordRequest", new UpdatePasswordRequest());
            }
            if (!model.containsAttribute("notificationRequest")) {
                // 기존 설정값이 있다면 채워서 보냄 (Service에서 가져오는 로직 필요, 없다면 기본값)
                model.addAttribute("notificationRequest", new UpdateNotificationRequest());
            }

            return "mypage";
        } catch (Exception e) {
            log.error("마이페이지 로딩 실패", e);
            return "redirect:/";
        }
    }

    /**
     * 프로필 수정 처리 (이미지 포함)
     */
    @PostMapping("/profile/edit")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateMemberRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "file", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "입력값이 올바르지 않습니다.");
            return "redirect:/mypage";
        }

        try {
            String email = userDetails.getUsername();

            // 1. 파일 업로드 처리
            if (file != null && !file.isEmpty()) {
                String validationError = validateFile(file);
                if (validationError != null) {
                    redirectAttributes.addFlashAttribute("error", validationError);
                    return "redirect:/mypage";
                }
                String imageUrl = saveProfileImage(file);
                if (imageUrl != null) {
                    request.setProfileImage(imageUrl);
                }
            }

            // 2. 서비스 호출
            myPageService.updateMemberForMyPage(email, request);
            redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");

        } catch (Exception e) {
            log.error("프로필 수정 오류", e);
            redirectAttributes.addFlashAttribute("error", "프로필 수정 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage";
    }

    /**
     * 비밀번호 변경 처리 (요청하신 기능 유지)
     */
    @PostMapping("/password/change")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("passwordRequest") UpdatePasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "비밀번호 형식이 올바르지 않습니다.");
            return "redirect:/mypage";
        }

        try {
            myPageService.updatePassword(userDetails.getUsername(), request);
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "비밀번호 변경 실패");
        }
        return "redirect:/mypage";
    }

    /**
     * 알림 설정 변경 처리 (요청하신 기능 유지)
     */
    @PostMapping("/notifications")
    public String updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute("notificationRequest") UpdateNotificationRequest request,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) return "redirect:/login";

        try {
            myPageService.updateNotificationSettings(userDetails.getUsername(), request);
            redirectAttributes.addFlashAttribute("message", "알림 설정이 저장되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "알림 설정 저장 실패");
        }
        return "redirect:/mypage";
    }

    /**
     * 게시글 삭제 (마이페이지 내)
     */
    @PostMapping("/posts/{postId}/delete")
    public String deleteMyPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) return "redirect:/login";

        try {
            myPageService.deleteMyPost(userDetails.getUsername(), postId);
            redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "게시글 삭제 실패");
        }
        return "redirect:/mypage";
    }

    // --- Helper Methods ---

    private String validateFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) return "잘못된 파일입니다.";
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) return "지원하지 않는 이미지 형식입니다.";
        if (file.getSize() > MAX_FILE_SIZE) return "파일 크기는 5MB 이하여야 합니다.";
        return null;
    }

    private String saveProfileImage(MultipartFile file) throws IOException {
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
        String newFilename = UUID.randomUUID() + ext;
        Path uploadPath = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        return "/uploads/" + newFilename;
    }
}