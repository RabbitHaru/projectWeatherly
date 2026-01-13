package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateAgreementRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateMemberRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.UpdateNotificationRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import me.shinsunyoung.projectweatherly.member.dto.response.MyPageResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.http.ResponseEntity;
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
@Api(tags = "마이페이지 API")
public class MyPageController {

    private final MemberService memberService;
    private final String UPLOAD_DIR = "./uploads/";

    // ✅ 제네릭 error 메서드 유틸리티
    private <T> ApiResponse2<T> createErrorResponse(String message) {
        return ApiResponse2.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }

    @GetMapping("/me")
    @ApiOperation(value = "내 정보 조회", notes = "현재 로그인한 회원의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> getMyInfo(
            @RequestHeader("Authorization") String token) {

        Long memberId = extractMemberIdFromToken(token);
        MyPageResponse response = memberService.getMyPageInfo(memberId);

        return ResponseEntity.ok(ApiResponse2.success(response));
    }

    @PutMapping("/profile")
    @ApiOperation(value = "프로필 수정", notes = "닉네임 및 프로필 이미지를 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateMemberRequest request) {

        Long memberId = extractMemberIdFromToken(token);
        MyPageResponse response = memberService.updateMemberForMyPage(memberId, request);

        return ResponseEntity.ok(ApiResponse2.success("프로필이 수정되었습니다.", response));
    }

    @PostMapping("/profile-image")
    @ApiOperation(value = "프로필 이미지 업로드", notes = "프로필 이미지를 업로드하고 URL을 반환합니다.")
    public ResponseEntity<ApiResponse2<String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            // ✅ 유틸리티 메서드 사용
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("파일이 비어있습니다."));
        }

        // 파일 저장 디렉토리 생성
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("올바른 파일 형식이 아닙니다."));
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        // 파일 저장
        Path filePath = uploadPath.resolve(newFilename);
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("파일 저장 중 오류가 발생했습니다."));
        }

        // 이미지 URL 생성
        String imageUrl = "/uploads/" + newFilename;

        return ResponseEntity.ok(ApiResponse2.success("이미지 업로드 성공", imageUrl));
    }

    @PutMapping("/agreements")
    @ApiOperation(value = "약관 동의 수정", notes = "약관 동의 여부를 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateAgreements(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateAgreementRequest request) {

        Long memberId = extractMemberIdFromToken(token);
        MyPageResponse response = memberService.updateAgreementForMyPage(memberId, request);

        return ResponseEntity.ok(ApiResponse2.success("약관 동의가 수정되었습니다.", response));
    }

    @PutMapping("/notifications")
    @ApiOperation(value = "알림 설정 수정", notes = "게시판 및 기상특보 알림 설정을 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateNotifications(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateNotificationRequest request) {

        Long memberId = extractMemberIdFromToken(token);
        MyPageResponse response = memberService.updateNotificationForMyPage(memberId, request);

        return ResponseEntity.ok(ApiResponse2.success("알림 설정이 수정되었습니다.", response));
    }

    @DeleteMapping("/deactivate")
    @ApiOperation(value = "회원 탈퇴", notes = "현재 로그인한 회원을 탈퇴 처리합니다.")
    public ResponseEntity<ApiResponse2<Void>> deactivateMember(
            @RequestHeader("Authorization") String token) {

        Long memberId = extractMemberIdFromToken(token);
        memberService.deactivateMember(memberId);

        return ResponseEntity.ok(ApiResponse2.success("회원 탈퇴가 완료되었습니다.", null));
    }

    private Long extractMemberIdFromToken(String token) {
        // 실제 구현에서는 JWT 토큰에서 memberId를 추출
        // 간단한 예시를 위해 1L 반환
        return 1L;
    }
}