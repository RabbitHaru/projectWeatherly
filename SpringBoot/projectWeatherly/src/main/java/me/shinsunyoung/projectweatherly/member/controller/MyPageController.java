package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateAgreementRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateMemberRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateNotificationRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Tag(name = "마이페이지 API", description = "마이페이지 관련 기능 API")
public class MyPageController {

    private final MemberService memberService;
    private static final String UPLOAD_DIR = "./uploads/";

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.error("로그인이 필요합니다."));
        }

        String email = userDetails.getUsername();
        me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse memberResponse =
                memberService.getMemberByEmail(email);
        MyPageResponse response = memberService.getMyPageInfo(memberResponse.getMemberId());

        return ResponseEntity.ok(ApiResponse2.success(response));
    }

    @PutMapping("/profile")
    @Operation(summary = "프로필 수정", description = "닉네임 및 프로필 이미지를 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMemberRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.error("로그인이 필요합니다."));
        }

        String email = userDetails.getUsername();
        me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse memberResponse =
                memberService.getMemberByEmail(email);
        MyPageResponse response = memberService.updateMemberForMyPage(
                memberResponse.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse2.success("프로필이 수정되었습니다.", response));
    }

    @PostMapping("/profile-image")
    @Operation(summary = "프로필 이미지 업로드",
            description = "프로필 이미지를 업로드하고 URL을 반환합니다.")
    public ResponseEntity<ApiResponse2<String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse2.error("파일이 비어있습니다."));
        }

        // 파일 유효성 검사
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse2.error("올바른 파일 형식이 아닙니다."));
        }

        // 허용되는 이미지 확장자
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif"};
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        boolean isValidExtension = false;
        for (String ext : allowedExtensions) {
            if (ext.equals(fileExtension)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse2.error("지원하지 않는 파일 형식입니다. JPG, JPEG, PNG, GIF만 업로드 가능합니다."));
        }

        // 파일 크기 제한 (5MB)
        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxFileSize) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse2.error("파일 크기는 5MB를 초과할 수 없습니다."));
        }

        // 파일 저장 디렉토리 생성
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 고유한 파일명 생성
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        Path filePath = uploadPath.resolve(newFilename);
        try {
            Files.copy(file.getInputStream(), filePath);
            log.info("파일 저장 성공: {}", newFilename);
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse2.error("파일 저장 중 오류가 발생했습니다."));
        }

        // 이미지 URL 생성
        String imageUrl = "/uploads/" + newFilename;

        return ResponseEntity.ok(ApiResponse2.success("이미지 업로드 성공", imageUrl));
    }

    @PutMapping("/agreements")
    @Operation(summary = "약관 동의 수정", description = "약관 동의 여부를 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateAgreements(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateAgreementRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.error("로그인이 필요합니다."));
        }

        String email = userDetails.getUsername();
        me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse memberResponse =
                memberService.getMemberByEmail(email);
        MyPageResponse response = memberService.updateAgreementForMyPage(
                memberResponse.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse2.success("약관 동의가 수정되었습니다.", response));
    }

    @PutMapping("/notifications")
    @Operation(summary = "알림 설정 수정",
            description = "게시판 및 기상특보 알림 설정을 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNotificationRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.error("로그인이 필요합니다."));
        }

        String email = userDetails.getUsername();
        me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse memberResponse =
                memberService.getMemberByEmail(email);
        MyPageResponse response = memberService.updateNotificationForMyPage(
                memberResponse.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse2.success("알림 설정이 수정되었습니다.", response));
    }

    @DeleteMapping("/deactivate")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 회원을 탈퇴 처리합니다.")
    public ResponseEntity<ApiResponse2<Void>> deactivateMember(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.error("로그인이 필요합니다."));
        }

        String email = userDetails.getUsername();
        me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse memberResponse =
                memberService.getMemberByEmail(email);
        memberService.deactivateMember(memberResponse.getMemberId());

        return ResponseEntity.ok(ApiResponse2.success("회원 탈퇴가 완료되었습니다."));
    }
}