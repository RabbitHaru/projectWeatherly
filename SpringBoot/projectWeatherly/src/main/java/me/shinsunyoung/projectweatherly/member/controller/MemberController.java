package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateAgreementRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateMemberRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdatePasswordRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "회원 관리 컨트롤러", description = "회원 정보 조회, 수정, 관리 관련 페이지")
public class MemberController {

    private final MemberService memberService;
    private static final String UPLOAD_DIR = "./uploads/profile-images/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // ==================== 공통 유틸리티 메서드 ====================
    private Long getCurrentMemberId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new me.shinsunyoung.projectweatherly.member.exception.MemberException("로그인이 필요합니다.");
        }
        String email = userDetails.getUsername();
        MemberResponse memberResponse = memberService.getMemberByEmail(email);
        return memberResponse.getMemberId();
    }

    // ==================== 회원 기본 정보 관리 ====================

    // 현재 로그인한 사용자 정보 조회 페이지
    @GetMapping("/me")
    @Operation(summary = "내 정보 페이지", description = "현재 로그인한 사용자의 상세 정보를 표시합니다.")
    public String showCurrentMemberInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.getMemberById(memberId);
            model.addAttribute("member", memberResponse);
            return "members/my-info";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }

    // 특정 회원 정보 조회 페이지
    @GetMapping("/{memberId}")
    @Operation(summary = "회원 정보 페이지", description = "특정 회원의 공개 정보를 표시합니다.")
    public String showMemberById(
            @PathVariable @Parameter(description = "회원 ID") Long memberId,
            Model model) {

        try {
            MemberResponse memberResponse = memberService.getMemberById(memberId);
            MemberResponse publicResponse = MemberResponse.builder()
                    .memberId(memberResponse.getMemberId())
                    .email(memberResponse.getEmail())
                    .nickname(memberResponse.getNickname())
                    .profileImage(memberResponse.getProfileImage())
                    .createdAt(memberResponse.getCreatedAt())
                    .build();

            model.addAttribute("member", publicResponse);
            return "members/member-profile";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/not-found";
        }
    }

    // 회원 정보 수정 페이지
    @GetMapping("/me/edit")
    @Operation(summary = "회원 정보 수정 페이지", description = "회원 정보 수정 폼을 표시합니다.")
    public String showEditMemberPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.getMemberById(memberId);
            model.addAttribute("member", memberResponse);
            model.addAttribute("updateRequest", new UpdateMemberRequest());
            return "members/edit-profile";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }

    // 회원 정보 수정 처리
    @PostMapping("/me/edit")
    @Operation(summary = "회원 정보 수정 처리", description = "회원 정보를 수정합니다.")
    public String updateCurrentMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateMemberRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "members/edit-profile";
        }

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.updateMember(memberId, request);
            redirectAttributes.addFlashAttribute("message", "회원 정보가 수정되었습니다.");
            return "redirect:/members/me";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "members/edit-profile";
        }
    }

    // 비밀번호 변경 페이지
    @GetMapping("/me/password")
    @Operation(summary = "비밀번호 변경 페이지", description = "비밀번호 변경 폼을 표시합니다.")
    public String showChangePasswordPage(Model model) {
        model.addAttribute("passwordRequest", new UpdatePasswordRequest());
        return "members/change-password";
    }

    // 비밀번호 변경 처리
    @PostMapping("/me/password")
    @Operation(summary = "비밀번호 변경 처리", description = "비밀번호를 변경합니다.")
    public String updateCurrentPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdatePasswordRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "members/change-password";
        }

        try {
            Long memberId = getCurrentMemberId(userDetails);
            memberService.updatePassword(memberId, request.getCurrentPassword(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
            return "redirect:/members/me";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "members/change-password";
        }
    }

    // ==================== 파일 업로드 기능 ====================

    // 프로필 이미지 업로드 페이지
    @GetMapping("/me/profile-image")
    @Operation(summary = "프로필 이미지 업로드 페이지", description = "프로필 이미지 업로드 폼을 표시합니다.")
    public String showProfileImageUploadPage() {
        return "members/upload-profile-image";
    }

    // 프로필 이미지 업로드 처리
    @PostMapping("/me/profile-image")
    @Operation(summary = "프로필 이미지 업로드 처리", description = "프로필 이미지를 업로드합니다.")
    public String uploadProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "파일이 비어있습니다.");
                return "members/upload-profile-image";
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                model.addAttribute("error", "파일 크기는 5MB를 초과할 수 없습니다.");
                return "members/upload-profile-image";
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                model.addAttribute("error", "올바른 파일 형식이 아닙니다.");
                return "members/upload-profile-image";
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!fileExtension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
                model.addAttribute("error", "지원하지 않는 파일 형식입니다. JPG, JPEG, PNG, GIF, WebP만 업로드 가능합니다.");
                return "members/upload-profile-image";
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/members/profile-images/" + newFilename;

            Long memberId = getCurrentMemberId(userDetails);
            UpdateMemberRequest updateRequest = UpdateMemberRequest.builder()
                    .profileImage(imageUrl)
                    .build();
            memberService.updateMember(memberId, updateRequest);

            log.info("프로필 이미지 업로드 성공: memberId={}, filename={}", memberId, newFilename);

            redirectAttributes.addFlashAttribute("message", "프로필 이미지가 업로드되었습니다.");
            redirectAttributes.addFlashAttribute("imageUrl", imageUrl);
            return "redirect:/members/me";

        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("error", "파일 업로드 중 오류가 발생했습니다.");
            return "members/upload-profile-image";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }

    // 프로필 이미지 조회
    @GetMapping("/profile-images/{filename:.+}")
    @Operation(summary = "프로필 이미지 조회", description = "업로드된 프로필 이미지를 표시합니다.")
    @ResponseBody
    public Resource getProfileImage(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
        return new UrlResource(filePath.toUri());
    }

    // ==================== 약관 및 알림 설정 ====================

    // 약관 동의 수정 페이지
    @GetMapping("/me/agreement")
    @Operation(summary = "약관 동의 수정 페이지", description = "약관 동의 수정 폼을 표시합니다.")
    public String showAgreementPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.getMemberById(memberId);
            model.addAttribute("member", memberResponse);
            model.addAttribute("agreementRequest", new UpdateAgreementRequest());
            return "members/agreement";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }

    // 약관 동의 수정 처리
    @PostMapping("/me/agreement")
    @Operation(summary = "약관 동의 수정 처리", description = "약관 동의 정보를 수정합니다.")
    public String updateCurrentAgreement(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateAgreementRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "members/agreement";
        }

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.updateAgreement(memberId, request);
            redirectAttributes.addFlashAttribute("message", "약관 동의 정보가 수정되었습니다.");
            return "redirect:/members/me";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "members/agreement";
        }
    }

    // ==================== 마이페이지 기능 ====================

    // 마이페이지 정보 조회
    @GetMapping("/me/mypage")
    @Operation(summary = "마이페이지", description = "마이페이지 정보를 표시합니다.")
    public String getMyPageInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MyPageResponse myPageResponse = memberService.getMyPageInfo(memberId);
            model.addAttribute("myPage", myPageResponse);
            return "members/my-page";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }

    // 마이페이지 정보 수정 페이지
    @GetMapping("/me/mypage/edit")
    @Operation(summary = "마이페이지 수정 페이지", description = "마이페이지 정보 수정 폼을 표시합니다.")
    public String showMyPageEditPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MyPageResponse myPageResponse = memberService.getMyPageInfo(memberId);
            model.addAttribute("myPage", myPageResponse);
            model.addAttribute("updateRequest", new UpdateMemberRequest());
            return "members/edit-my-page";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }

    // 마이페이지 정보 수정 처리
    @PostMapping("/me/mypage/edit")
    @Operation(summary = "마이페이지 정보 수정 처리", description = "마이페이지 정보를 수정합니다.")
    public String updateMyPageInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute UpdateMemberRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "members/edit-my-page";
        }

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MyPageResponse myPageResponse = memberService.updateMemberForMyPage(memberId, request);
            redirectAttributes.addFlashAttribute("message", "마이페이지 정보가 수정되었습니다.");
            return "redirect:/members/me/mypage";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "members/edit-my-page";
        }
    }

    // ==================== 회원 상태 관리 ====================

    // 회원 탈퇴 확인 페이지
    @GetMapping("/me/deactivate")
    @Operation(summary = "회원 탈퇴 확인 페이지", description = "회원 탈퇴 확인 폼을 표시합니다.")
    public String showDeactivatePage() {
        return "members/deactivate-confirm";
    }

    // 회원 탈퇴 처리
    @PostMapping("/me/deactivate")
    @Operation(summary = "회원 탈퇴 처리", description = "회원을 탈퇴 처리합니다.")
    public String deactivateCurrentMember(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            memberService.deactivateMember(memberId);
            log.info("회원 탈퇴 완료: memberId={}", memberId);

            // 세션 무효화 후 로그인 페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");
            return "redirect:/auth/logout";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return "error/unauthorized";
        }
    }

    // ==================== 통계 및 모니터링 ====================

    // 사용자 통계 조회 페이지
    @GetMapping("/me/stats")
    @Operation(summary = "사용자 통계 페이지", description = "사용자 활동 통계를 표시합니다.")
    public String showUserStats(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Long memberId = getCurrentMemberId(userDetails);

            Map<String, Object> stats = new HashMap<>();
            stats.put("postCount", 0);
            stats.put("commentCount", 0);
            stats.put("likeCount", 0);
            stats.put("loginCount", 1);
            stats.put("lastLogin", java.time.LocalDateTime.now().toString());

            model.addAttribute("stats", stats);
            return "members/stats";
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            model.addAttribute("error", e.getMessage());
            return "error/unauthorized";
        }
    }
}