package me.shinsunyoung.projectweatherly.member.service.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.RequestDto.MemberJoinRequest;
import me.shinsunyoung.projectweatherly.member.UserResponseDto.MemberResponseDto;
import me.shinsunyoung.projectweatherly.member.domain.member.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.member.Member;
import me.shinsunyoung.projectweatherly.member.domain.member.MemberRole;
import me.shinsunyoung.projectweatherly.member.dto.user.MemberUpdateRequest;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MemberResponseDto register(MemberJoinRequest request) {
        log.info("회원 가입 요청: {}", request.getUserEmail());

        // 이메일 중복 체크
        if (memberRepository.existsByUserEmail(request.getUserEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다: " + request.getUserEmail());
        }

        // 비밀번호 암호화 (로컬 회원인 경우)
        String encryptedPassword = null;
        boolean isSocialUser = !"local".equalsIgnoreCase(request.getAuthProvider());

        if (!isSocialUser) {
            if (request.getUserPassword() == null || request.getUserPassword().isEmpty()) {
                throw new RuntimeException("로컬 회원가입 시 비밀번호는 필수입니다.");
            }
            encryptedPassword = passwordEncoder.encode(request.getUserPassword());
        }

        // Enum으로 변환
        MemberRole userRoleEnum = MemberRole.fromString(request.getUserRole());
        AuthProvider authProviderEnum = AuthProvider.fromString(request.getAuthProvider());

        // 기본값 설정
        if (userRoleEnum == null) userRoleEnum = MemberRole.USER;
        if (authProviderEnum == null) authProviderEnum = AuthProvider.LOCAL;

        Boolean isActive = (request.getIsActive() != null) ? request.getIsActive() : true;

        // Member 엔티티 생성
        Member member = Member.builder()
                .userEmail(request.getUserEmail())
                .userPassword(encryptedPassword)
                .userName(request.getUserName())
                .profileImage(request.getProfileImage())
                .userRole(userRoleEnum)
                .authProvider(authProviderEnum)
                .providerId(request.getProviderId())
                .isActive(isActive)
                .build();

        // 저장
        Member savedMember = memberRepository.save(member);
        log.info("회원 가입 완료: ID={}, Email={}", savedMember.getUserId(), savedMember.getUserEmail());

        return toResponse(savedMember);
    }

    @Override
    public MemberResponseDto getMemberById(Long memberId) {
        log.info("회원 조회: ID={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        return toResponse(member);
    }

    @Override
    public MemberResponseDto getMemberByEmail(String email) {
        log.info("회원 조회: Email={}", email);

        Member member = memberRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. 이메일: " + email));

        return toResponse(member);
    }

    @Override
    @Transactional
    public MemberResponseDto updateMember(Long memberId, MemberUpdateRequest request) {
        log.info("회원 정보 수정: ID={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        // 이름 업데이트
        if (request.getUserName() != null && !request.getUserName().isEmpty()) {
            member.setUserName(request.getUserName());
        }

        // 프로필 이미지 업데이트
        if (request.getProfileImage() != null) {
            member.setProfileImage(request.getProfileImage());
        }

        // 권한 업데이트
        if (request.getUserRole() != null && !request.getUserRole().isEmpty()) {
            MemberRole userRole = MemberRole.fromString(request.getUserRole());
            if (userRole == null) {
                throw new RuntimeException("유효하지 않은 권한입니다: " + request.getUserRole());
            }
            member.setUserRole(userRole);
        }

        // 활성 상태 업데이트
        if (request.getIsActive() != null) {
            member.setIsActive(request.getIsActive());
        }

        Member updatedMember = memberRepository.save(member);
        log.info("회원 정보 업데이트 완료: ID={}", memberId);

        return toResponse(updatedMember);
    }

    @Override
    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경: ID={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        // 소셜 로그인 사용자 확인
        if (member.getAuthProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("소셜 로그인 회원은 비밀번호를 변경할 수 없습니다.");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, member.getUserPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 암호화 및 저장
        String encryptedNewPassword = passwordEncoder.encode(newPassword);
        member.setUserPassword(encryptedNewPassword);

        memberRepository.save(member);
        log.info("비밀번호 변경 완료: ID={}", memberId);
    }

    @Override
    @Transactional
    public MemberResponseDto updateProfileImage(Long memberId, String imageUrl) {
        log.info("프로필 이미지 업데이트: ID={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        member.setProfileImage(imageUrl);
        Member updatedMember = memberRepository.save(member);

        log.info("프로필 이미지 업데이트 완료: ID={}, URL={}", memberId, imageUrl);
        return toResponse(updatedMember);
    }

    @Override
    @Transactional
    public void deactivateMember(Long memberId) {
        log.info("회원 비활성화: ID={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        member.setIsActive(false);
        memberRepository.save(member);
        log.info("회원 비활성화 완료: ID={}", memberId);
    }

    @Override
    @Transactional
    public void activateMember(Long memberId) {
        log.info("회원 활성화: ID={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        member.setIsActive(true);
        memberRepository.save(member);
        log.info("회원 활성화 완료: ID={}", memberId);
    }

    @Override
    public List<MemberResponseDto> getAllMembers() {
        log.info("전체 회원 조회");

        List<Member> members = memberRepository.findAll();
        log.info("전체 회원 조회: {}명", members.size());

        return members.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberResponseDto> getMembersByRole(String role) {
        log.info("역할별 회원 조회: role={}", role);

        MemberRole memberRole = MemberRole.fromString(role);
        if (memberRole == null) {
            throw new RuntimeException("유효하지 않은 권한입니다: " + role);
        }

        // Repository가 Enum을 받도록 수정 필요
        List<Member> members = memberRepository.findByUserRoleAndIsActive(memberRole, true);
        log.info("역할별 회원 조회: role={}, count={}", role, members.size());

        return members.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEmailDuplicate(String email) {
        log.debug("이메일 중복 체크: email={}", email);

        boolean exists = memberRepository.existsByUserEmail(email);
        log.debug("이메일 중복 체크 결과: email={}, exists={}", email, exists);
        return exists;
    }

    @Override
    public List<MemberResponseDto> searchMembers(String keyword) {
        log.info("회원 검색: keyword={}", keyword);

        try {
            List<Member> members = memberRepository.searchByKeyword(keyword);
            return members.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Repository 검색 메서드가 없어 전체에서 필터링합니다. error: {}", e.getMessage());
            List<Member> allMembers = memberRepository.findAll();

            return allMembers.stream()
                    .filter(member ->
                            member.getUserEmail() != null && member.getUserEmail().toLowerCase().contains(keyword.toLowerCase()) ||
                                    member.getUserName() != null && member.getUserName().toLowerCase().contains(keyword.toLowerCase()))
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Member getMemberEntityById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));
    }

    @Override
    public Member getMemberEntityByEmail(String email) {
        return memberRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. 이메일: " + email));
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    @Transactional
    public void updateLastLogin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + memberId));

        member.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);
        log.debug("마지막 로그인 시간 업데이트: ID={}", memberId);
    }

    /**
     * 회원 통계 조회 (관리자용)
     */
    public MemberStats getMemberStats() {
        long totalMembers = memberRepository.count();
        long activeMembers = 0;
        long inactiveMembers = 0;

        try {
            activeMembers = memberRepository.countByIsActive(true);
            inactiveMembers = totalMembers - activeMembers;
        } catch (Exception e) {
            log.warn("countByIsActive 메서드가 없습니다. 기본값 사용");
            List<Member> allMembers = memberRepository.findAll();
            activeMembers = allMembers.stream().filter(m -> Boolean.TRUE.equals(m.getIsActive())).count();
            inactiveMembers = totalMembers - activeMembers;
        }

        return MemberStats.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .inactiveMembers(inactiveMembers)
                .build();
    }

    /**
     * Member 엔티티를 MemberResponseDto로 변환
     */
    private MemberResponseDto toResponse(Member member) {
        if (member == null) {
            return null;
        }

        return MemberResponseDto.builder()
                .userId(member.getUserId())
                .userEmail(member.getUserEmail())
                .userName(member.getUserName())
                .profileImage(member.getProfileImage())
                .userRole(member.getUserRole() != null ? member.getUserRole().name() : "USER")
                .authProvider(member.getAuthProvider() != null ? member.getAuthProvider().name() : "LOCAL")
                .isActive(member.getIsActive())
                .lastLoginAt(member.getLastLoginAt())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }

    /**
     * 회원 통계 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class MemberStats {
        private long totalMembers;
        private long activeMembers;
        private long inactiveMembers;
    }
}