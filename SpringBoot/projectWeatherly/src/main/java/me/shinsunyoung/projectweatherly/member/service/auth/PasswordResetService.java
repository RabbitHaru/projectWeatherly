package me.shinsunyoung.projectweatherly.member.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.domain.member.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.ChangePasswordWithCodeDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.PasswordResetRequestDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.VerifyCodeRequestDto;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import me.shinsunyoung.projectweatherly.member.service.email.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 재설정 요청 - 인증번호 발송
     */
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto request) {
        String email = request.getUserEmail();

        // 회원 존재 여부 확인
        Member member = memberRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 회원이 없습니다."));

        // 활성 계정인지 확인
        if (!Boolean.TRUE.equals(member.getIsActive())) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // 인증번호 생성 및 저장 (6자리 숫자)
        String verificationCode = verificationService.generateAndSaveVerificationCode(email, "PASSWORD_RESET");

        // 이메일 발송
        emailService.sendPasswordResetVerificationCode(email, verificationCode, member.getUserName());

        log.info("비밀번호 재설정 요청 처리 완료: {}", email);
    }

    /**
     * 인증번호 확인
     */
    public boolean verifyCode(VerifyCodeRequestDto request) {
        return verificationService.verifyCode(
                request.getUserEmail(),
                request.getVerificationCode(),
                "PASSWORD_RESET"
        );
    }

    /**
     * 인증번호로 비밀번호 변경
     */
    @Transactional
    public void changePasswordWithVerificationCode(ChangePasswordWithCodeDto request) {
        String email = request.getUserEmail();
        String code = request.getVerificationCode();
        String newPassword = request.getNewPassword();

        // 1. 인증번호 확인
        if (!verificationService.verifyCode(email, code, "PASSWORD_RESET")) {
            throw new IllegalArgumentException("인증번호가 유효하지 않거나 만료되었습니다.");
        }

        // 2. 비밀번호 확인 일치 검사
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 3. 회원 조회
        Member member = memberRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 4. 비밀번호 길이 및 보안 검증
        validatePasswordStrength(newPassword);

        // 5. 이전 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(newPassword, member.getUserPassword())) {
            throw new IllegalArgumentException("이전 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        // 6. 새 비밀번호 설정
        String encodedPassword = passwordEncoder.encode(newPassword);
        member.setUserPassword(encodedPassword);

        // 7. 인증번호 사용 처리 (만료시키기)
        verificationService.markCodeAsUsed(email, code, "PASSWORD_RESET");

        memberRepository.save(member);

        // 8. 비밀번호 변경 완료 이메일 발송
        emailService.sendPasswordChangedNotification(email, member.getUserName());

        log.info("인증번호를 통한 비밀번호 변경 완료: {}", email);
    }

    /**
     * 현재 비밀번호 확인 후 변경 (로그인 상태에서)
     */
    @Transactional
    public void changePasswordWithCurrentPassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, member.getUserPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 길이 및 보안 검증
        validatePasswordStrength(newPassword);

        // 이전 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(newPassword, member.getUserPassword())) {
            throw new IllegalArgumentException("이전 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        // 새 비밀번호 설정
        String encodedPassword = passwordEncoder.encode(newPassword);
        member.setUserPassword(encodedPassword);
        memberRepository.save(member);

        // 비밀번호 변경 알림 이메일 발송
        emailService.sendPasswordChangedNotification(member.getUserEmail(), member.getUserName());

        log.info("현재 비밀번호 확인 후 비밀번호 변경 완료: memberId={}", memberId);
    }

    /**
     * 비밀번호 보안 강도 검증
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }

        // 영문, 숫자, 특수문자 포함 여부 검증
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[@$!%*#?&].*");

        if (!hasLetter || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 포함해야 합니다.");
        }
    }
}