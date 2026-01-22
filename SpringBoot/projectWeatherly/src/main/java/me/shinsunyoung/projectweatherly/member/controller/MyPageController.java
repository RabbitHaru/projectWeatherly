package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.service.MyPageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     */
    @GetMapping
    @Operation(summary = "마이페이지 메인", description = "마이페이지 메인을 표시합니다.")
    public String getMyInfo(
            @AuthenticationPrincipal UserSecurityDTO user, // [변경] UserDetails -> UserSecurityDTO
            @RequestParam(defaultValue = "1") Integer page,
            Model model,
            HttpServletRequest request) {

        if (user == null) {
            return "redirect:/login";
        }

        try {
            // [★요청하신 방식 적용] 컨트롤러 내부에서 닉네임 주입
            model.addAttribute("requestURI", request.getRequestURI());
            if(user != null && user.getUser().getNickname() != null) {
                model.addAttribute("nickname", user.getUser().getNickname());
            }

            // UserSecurityDTO에서 이메일(username) 가져오기
            String email = user.getUsername();
            MyPageResponse memberResponse = myPageService.getMyPageInfo(email, page);

            model.addAttribute("myPage", memberResponse);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPosts", memberResponse.getMyCommunityPosts().getTotalPages());
            model.addAttribute("totalReports",memberResponse.getMyReports().getTotalPages());
            model.addAttribute("totalComments",memberResponse.getMyComments().getTotalPages());
            // 폼 바인딩용 빈 객체들 추가
            if (!model.containsAttribute("passwordRequest")) {
                model.addAttribute("passwordRequest", new UpdatePasswordRequest());
            }
            if (!model.containsAttribute("notificationRequest")) {
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
            @AuthenticationPrincipal UserSecurityDTO user,
            @Valid @ModelAttribute UpdateMemberRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "file", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "입력값이 올바르지 않습니다.");
            return "redirect:/mypage";
        }

        try {
            String email = user.getUsername();

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

            // 1. DB 업데이트 (기존 코드)
            myPageService.updateMemberForMyPage(email, request);

            // ============================================================
            // [추가] 2. 로그인된 세션 정보(헤더 표시용) 즉시 업데이트
            // ============================================================
            // 닉네임 변경 반영
            user.getUser().setNickname(request.getNickname());

            // 프로필 이미지 변경 반영 (이미지가 있을 경우에만)
            if (request.getProfileImage() != null) {
                user.getUser().setProfileImage(request.getProfileImage());
            }
            // ============================================================

            redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");

        } catch (Exception e) {
            log.error("프로필 수정 오류", e);
            redirectAttributes.addFlashAttribute("error", "프로필 수정 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage";
    }

    /**
     * 비밀번호 변경 처리
     */
    @PostMapping("/password/change")
    public String changePassword(
            @AuthenticationPrincipal UserSecurityDTO user,
            @Valid @ModelAttribute("passwordRequest") UpdatePasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "비밀번호 형식이 올바르지 않습니다.");
            return "redirect:/mypage";
        }

        try {
            myPageService.updatePassword(user.getUsername(), request);
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "비밀번호 변경 실패");
        }
        return "redirect:/mypage";
    }

    /**
     * 알림 설정 변경 처리
     */
    @PostMapping("/notifications")
    public String updateNotifications(
            @AuthenticationPrincipal UserSecurityDTO user,
            @ModelAttribute("notificationRequest") UpdateNotificationRequest request,
            RedirectAttributes redirectAttributes) {

        if (user == null) return "redirect:/login";

        try {
            myPageService.updateNotificationSettings(user.getUsername(), request);
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
            @AuthenticationPrincipal UserSecurityDTO user,
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {

        if (user == null) return "redirect:/login";

        try {
            myPageService.deleteMyPost(user.getUsername(), postId);
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