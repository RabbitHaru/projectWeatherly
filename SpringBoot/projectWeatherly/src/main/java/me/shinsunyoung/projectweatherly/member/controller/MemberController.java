package me.shinsunyoung.projectweatherly.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import me.shinsunyoung.projectweatherly.member.RequestDto.MemberJoinRequest;
import me.shinsunyoung.projectweatherly.member.UserResponseDto.MemberResponseDto;
import me.shinsunyoung.projectweatherly.member.dto.request.member.PasswordChangeRequest;
import me.shinsunyoung.projectweatherly.member.dto.user.MemberUpdateRequest;
import me.shinsunyoung.projectweatherly.member.service.member.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    // 회원 가입
    @PostMapping("/register")
    public ResponseEntity<MemberResponseDto > register(@Valid @RequestBody MemberJoinRequest request) {
        MemberResponseDto response = memberService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto > getMyInfo() {
        // TODO: SecurityContext에서 현재 로그인한 사용자 ID 가져오기
        Long currentMemberId = 1L; // 임시 값
        MemberResponseDto  response = memberService.getMemberById(currentMemberId);
        return ResponseEntity.ok(response);
    }

    // 회원 정보 조회 (ID로)
    @GetMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN') or #memberId == authentication.principal.memberId")
    public ResponseEntity<MemberResponseDto > getMemberById(@PathVariable Long memberId) {
        MemberResponseDto  response = memberService.getMemberById(memberId);
        return ResponseEntity.ok(response);
    }

    // 회원 정보 수정
    @PutMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN') or #memberId == authentication.principal.memberId")
    public ResponseEntity<MemberResponseDto > updateMember(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberUpdateRequest request) {
        MemberResponseDto  response = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(response);
    }

    // 비밀번호 변경
    @PatchMapping("/{memberId}/password")
    @PreAuthorize("#memberId == authentication.principal.memberId")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long memberId,
            @Valid @RequestBody PasswordChangeRequest request) {
        memberService.changePassword(memberId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    // 프로필 이미지 업데이트
    @PatchMapping("/{memberId}/profile-image")
    @PreAuthorize("hasRole('ADMIN') or #memberId == authentication.principal.memberId")
    public ResponseEntity<MemberResponseDto > updateProfileImage(
            @PathVariable Long memberId,
            @RequestParam String imageUrl) {
        MemberResponseDto  response = memberService.updateProfileImage(memberId, imageUrl);
        return ResponseEntity.ok(response);
    }

    // 회원 탈퇴 (비활성화)
    @DeleteMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN') or #memberId == authentication.principal.memberId")
    public ResponseEntity<Void> deactivateMember(@PathVariable Long memberId) {
        memberService.deactivateMember(memberId);
        return ResponseEntity.ok().build();
    }

    // 관리자용: 모든 회원 조회
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberResponseDto >> getAllMembers() {
        List<MemberResponseDto > responses = memberService.getAllMembers();
        return ResponseEntity.ok(responses);
    }

    // 관리자용: 역할별 회원 조회
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MemberResponseDto >> getMembersByRole(@PathVariable String role) {
        List<MemberResponseDto > responses = memberService.getMembersByRole(role);
        return ResponseEntity.ok(responses);
    }
}