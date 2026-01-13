package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.*;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원 관리 API", description = "회원 정보 조회, 수정, 관리 관련 API")
public class MemberController {

    private final MemberService memberService;
    private static final String UPLOAD_DIR = "./uploads/profile-images/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // ==================== 공통 유틸리티 메서드 ====================

    /**
     * 현재 로그인한 사용자의 ID 조회
     */
    private Long getCurrentMemberId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new me.shinsunyoung.projectweatherly.member.exception.MemberException("로그인이 필요합니다.");
        }
        String email = userDetails.getUsername();
        MemberResponse memberResponse = memberService.getMemberByEmail(email);
        return memberResponse.getMemberId();
    }

    /**
     * 현재 사용자가 요청한 회원에 접근 권한이 있는지 확인
     */
    private void validateMemberAccess(Long requestedMemberId, Long currentMemberId) {
        if (!requestedMemberId.equals(currentMemberId)) {
            throw new me.shinsunyoung.projectweatherly.member.exception.MemberException("접근 권한이 없습니다.");
        }
    }

    // ==================== 회원 기본 정보 관리 ====================

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 사용자 정보 조회",
            description = "세션에 저장된 현재 로그인한 사용자의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse2<MemberResponse>> getCurrentMemberInfo(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.getMemberById(memberId);
            return ResponseEntity.ok(ApiResponse2.success(memberResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.unauthorized(e.getMessage()));
        }
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "특정 회원 정보 조회",
            description = "특정 회원의 공개 정보를 조회합니다. (프로필 이미지, 닉네임 등)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse2<MemberResponse>> getMemberById(
            @PathVariable @Parameter(description = "회원 ID") Long memberId) {

        try {
            MemberResponse memberResponse = memberService.getMemberById(memberId);
            // 민감한 정보 필터링 (선택적)
            MemberResponse publicResponse = MemberResponse.builder()
                    .memberId(memberResponse.getMemberId())
                    .email(memberResponse.getEmail()) // 또는 마스킹 처리
                    .nickname(memberResponse.getNickname())
                    .profileImage(memberResponse.getProfileImage())
                    .createdAt(memberResponse.getCreatedAt())
                    .build();

            return ResponseEntity.ok(ApiResponse2.success(publicResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse2.notFound(e.getMessage()));
        }
    }

    @PutMapping("/me")
    @Operation(summary = "현재 사용자 정보 수정",
            description = "현재 로그인한 사용자의 닉네임과 프로필 이미지를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse2<MemberResponse>> updateCurrentMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMemberRequest request) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.updateMember(memberId, request);
            return ResponseEntity.ok(ApiResponse2.success("회원 정보가 수정되었습니다.", memberResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse2.badRequest(e.getMessage()));
        }
    }

    @PutMapping("/me/password")
    @Operation(summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인하고 새로운 비밀번호로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse2<Void>> updateCurrentPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            memberService.updatePassword(memberId, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse2.success("비밀번호가 변경되었습니다."));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse2.badRequest(e.getMessage()));
        }
    }

    // ==================== 파일 업로드 기능 ====================

    @PostMapping("/me/profile-image")
    @Operation(summary = "프로필 이미지 업로드",
            description = "프로필 이미지를 업로드하고 URL을 반환합니다. (JPG, PNG, GIF, 최대 5MB)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 크기"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse2<String>> uploadProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        try {
            // 파일 유효성 검사
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse2.badRequest("파일이 비어있습니다."));
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse2.badRequest("파일 크기는 5MB를 초과할 수 없습니다."));
            }

            // 파일 확장자 검사
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse2.badRequest("올바른 파일 형식이 아닙니다."));
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!fileExtension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse2.badRequest("지원하지 않는 파일 형식입니다. JPG, JPEG, PNG, GIF, WebP만 업로드 가능합니다."));
            }

            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유한 파일명 생성
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 이미지 URL 생성
            String imageUrl = "/api/members/profile-images/" + newFilename;

            // 회원 정보 업데이트 (선택적)
            Long memberId = getCurrentMemberId(userDetails);
            UpdateMemberRequest updateRequest = UpdateMemberRequest.builder()
                    .profileImage(imageUrl)
                    .build();
            memberService.updateMember(memberId, updateRequest);

            log.info("프로필 이미지 업로드 성공: memberId={}, filename={}", memberId, newFilename);

            return ResponseEntity.ok(ApiResponse2.success("프로필 이미지가 업로드되었습니다.", imageUrl));

        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse2.internalServerError("파일 업로드 중 오류가 발생했습니다."));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.unauthorized(e.getMessage()));
        }
    }

    @GetMapping("/profile-images/{filename:.+}")
    @Operation(summary = "프로필 이미지 조회",
            description = "업로드된 프로필 이미지를 조회합니다.")
    public ResponseEntity<Resource> getProfileImage(
            @PathVariable String filename) {

        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Content-Type 자동 감지
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("프로필 이미지 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 약관 및 알림 설정 ====================

    @PutMapping("/me/agreement")
    @Operation(summary = "약관 동의 수정",
            description = "현재 사용자의 약관 동의 정보를 수정합니다.")
    public ResponseEntity<ApiResponse2<MemberResponse>> updateCurrentAgreement(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateAgreementRequest request) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.updateAgreement(memberId, request);
            return ResponseEntity.ok(ApiResponse2.success("약관 동의 정보가 수정되었습니다.", memberResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse2.badRequest(e.getMessage()));
        }
    }

    @PutMapping("/me/notification")
    @Operation(summary = "알림 설정 수정",
            description = "현재 사용자의 알림 설정을 수정합니다.")
    public ResponseEntity<ApiResponse2<MemberResponse>> updateCurrentNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNotificationRequest request) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MemberResponse memberResponse = memberService.updateNotification(memberId, request);
            return ResponseEntity.ok(ApiResponse2.success("알림 설정이 수정되었습니다.", memberResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse2.badRequest(e.getMessage()));
        }
    }

    // ==================== 마이페이지 기능 ====================

    @GetMapping("/me/mypage")
    @Operation(summary = "마이페이지 정보 조회",
            description = "현재 사용자의 마이페이지 정보를 조회합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> getMyPageInfo(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MyPageResponse myPageResponse = memberService.getMyPageInfo(memberId);
            return ResponseEntity.ok(ApiResponse2.success(myPageResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.unauthorized(e.getMessage()));
        }
    }

    @PutMapping("/me/mypage")
    @Operation(summary = "마이페이지 정보 수정",
            description = "현재 사용자의 마이페이지 정보를 수정합니다.")
    public ResponseEntity<ApiResponse2<MyPageResponse>> updateMyPageInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMemberRequest request) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            MyPageResponse myPageResponse = memberService.updateMemberForMyPage(memberId, request);
            return ResponseEntity.ok(ApiResponse2.success("마이페이지 정보가 수정되었습니다.", myPageResponse));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse2.badRequest(e.getMessage()));
        }
    }

    // ==================== 회원 상태 관리 ====================

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴",
            description = "현재 로그인한 사용자를 탈퇴 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse2<Void>> deactivateCurrentMember(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long memberId = getCurrentMemberId(userDetails);
            memberService.deactivateMember(memberId);
            log.info("회원 탈퇴 완료: memberId={}", memberId);
            return ResponseEntity.ok(ApiResponse2.success("회원 탈퇴가 완료되었습니다."));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.unauthorized(e.getMessage()));
        }
    }

    @PostMapping("/me/reactivate")
    @Operation(summary = "회원 복구",
            description = "탈퇴한 회원을 복구합니다. (관리자 또는 본인 확인 필요)")
    public ResponseEntity<ApiResponse2<Void>> reactivateMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String email,
            @RequestParam String verificationCode) { // 이메일 인증 코드 등

        // 실제 구현에서는 이메일 인증, 관리자 확인 등의 로직 추가
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiResponse2.error("아직 구현되지 않은 기능입니다.", 501));
    }

    // ==================== 통계 및 모니터링 ====================

    @GetMapping("/me/stats")
    @Operation(summary = "사용자 통계 조회",
            description = "현재 사용자의 활동 통계를 조회합니다.")
    public ResponseEntity<ApiResponse2<Map<String, Object>>> getUserStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long memberId = getCurrentMemberId(userDetails);

            // 실제로는 통계 서비스에서 조회
            Map<String, Object> stats = new HashMap<>();
            stats.put("postCount", 0);
            stats.put("commentCount", 0);
            stats.put("likeCount", 0);
            stats.put("loginCount", 1);
            stats.put("lastLogin", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(ApiResponse2.success("통계 조회 성공", stats));
        } catch (me.shinsunyoung.projectweatherly.member.exception.MemberException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse2.unauthorized(e.getMessage()));
        }
    }

    // ==================== 관리자 기능 (선택적) ====================

    @GetMapping("/search")
    @Operation(summary = "회원 검색 (관리자)",
            description = "관리자가 회원을 검색합니다.")
    public ResponseEntity<ApiResponse2<Object>> searchMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 관리자 권한 체크 필요
        // 실제 구현에서는 MemberService.searchMembers() 메서드 필요

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiResponse2.error("관리자 기능은 아직 구현되지 않았습니다.", 501));
    }
}