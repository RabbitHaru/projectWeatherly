package me.shinsunyoung.projectweatherly.member.controller;



import io.swagger.annotations.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.dto.request.*;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import me.shinsunyoung.projectweatherly.member.dto.response.LoginResponse;
import me.shinsunyoung.projectweatherly.member.dto.response.MemberResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Validated
@Api(tags = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @ApiOperation(value = "회원가입", notes = "새로운 회원을 등록합니다.")
    @ApiResponses({
            @ApiResponse(code = 201, message = "회원가입 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청"),
            @ApiResponse(code = 409, message = "이미 존재하는 이메일")
    })
    public ResponseEntity<ApiResponse2<Long>> signup(
            @Valid @RequestBody SignupRequest request) {

        Long memberId = memberService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse2.success("회원가입이 완료되었습니다.", memberId));
    }

    @PostMapping("/login")
    @ApiOperation(value = "로그인", notes = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<ApiResponse2<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = memberService.login(request);

        return ResponseEntity.ok(ApiResponse2.success(response));
    }

    @GetMapping("/me")
    @ApiOperation(value = "내 정보 조회", notes = "현재 로그인한 회원의 정보를 조회합니다.")
    @ApiImplicitParam(name = "Authorization", value = "Access Token", required = true, paramType = "header")
    public ResponseEntity<ApiResponse2<MemberResponse>> getMyInfo(
            @RequestHeader("Authorization") String token) {

        // 토큰에서 memberId 추출 (실제 구현에서는 SecurityContext에서 가져옴)
        // 여기서는 간단히 구현
        Long memberId = extractMemberIdFromToken(token);

        MemberResponse response = memberService.getMemberById(memberId);

        return ResponseEntity.ok(ApiResponse2.success(response));
    }

    @PutMapping("/me")
    @ApiOperation(value = "내 정보 수정", notes = "현재 로그인한 회원의 정보를 수정합니다.")
    public ResponseEntity<ApiResponse2<MemberResponse>> updateMyInfo(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateMemberRequest request) {

        Long memberId = extractMemberIdFromToken(token);
        MemberResponse response = memberService.updateMember(memberId, request);

        return ResponseEntity.ok(ApiResponse2.success("회원 정보가 수정되었습니다.", response));
    }

    @PutMapping("/me/password")
    @ApiOperation(value = "비밀번호 변경", notes = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다.")
    public ResponseEntity<ApiResponse2<Void>> updatePassword(
            @RequestHeader("Authorization") String token,
            @RequestParam @NotBlank String currentPassword,
            @RequestParam @NotBlank String newPassword) {

        Long memberId = extractMemberIdFromToken(token);
        memberService.updatePassword(memberId, currentPassword, newPassword);

        return ResponseEntity.ok(ApiResponse2.success("비밀번호가 변경되었습니다.", null));
    }

    @PutMapping("/me/agreements")
    @ApiOperation(value = "약관 동의 수정", notes = "약관 동의 여부를 수정합니다.")
    public ResponseEntity<ApiResponse2<MemberResponse>> updateAgreements(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateAgreementRequest request) {

        Long memberId = extractMemberIdFromToken(token);
        MemberResponse response = memberService.updateAgreement(memberId, request);

        return ResponseEntity.ok(ApiResponse2.success("약관 동의가 수정되었습니다.", response));
    }

    @PutMapping("/me/notifications")
    @ApiOperation(value = "알림 설정 수정", notes = "게시판 및 기상특보 알림 설정을 수정합니다.")
    public ResponseEntity<ApiResponse2<MemberResponse>> updateNotifications(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateNotificationRequest request) {

        Long memberId = extractMemberIdFromToken(token);
        MemberResponse response = memberService.updateNotification(memberId, request);

        return ResponseEntity.ok(ApiResponse2.success("알림 설정이 수정되었습니다.", response));
    }

    @DeleteMapping("/me")
    @ApiOperation(value = "회원 탈퇴", notes = "현재 로그인한 회원을 탈퇴 처리합니다.")
    public ResponseEntity<ApiResponse2<Void>> deactivateMember(
            @RequestHeader("Authorization") String token) {

        Long memberId = extractMemberIdFromToken(token);
        memberService.deactivateMember(memberId);

        return ResponseEntity.ok(ApiResponse2.success("회원 탈퇴가 완료되었습니다.", null));
    }

    private Long extractMemberIdFromToken(String token) {
        // 실제 구현에서는 JWT 토큰에서 memberId를 추출
        // 여기서는 간단히 1L 반환 (실제로는 SecurityContext 사용)
        return 1L;
    }
}