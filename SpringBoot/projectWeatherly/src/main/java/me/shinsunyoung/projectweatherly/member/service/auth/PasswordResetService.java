package me.shinsunyoung.projectweatherly.member.service.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.shinsunyoung.projectweatherly.member.domain.model.entity.member.Member;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.ChangePasswordWithCodeDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.PasswordResetRequestDto;
import me.shinsunyoung.projectweatherly.member.dto.request.auth.VerifyCodeRequestDto;
import me.shinsunyoung.projectweatherly.member.repository.member.MemberRepository;

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
        String email = request.getEmail();

        // 회원 존재 여부 확인
        Member member = memberRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일로 가입된 회원이 없습니다."));

        // 활성 계정인지 확인
        if (!Boolean.TRUE.equals(member.getIsActive())) {
            throw new RuntimeException("비활성화된 계정입니다.");
        }

        // 인증번호 생성 및 저장
        String verificationCode = verificationService.generateAndSaveVerificationCode(email);

        // 이메일 발송
        emailService.sendVerificationCode(email, verificationCode);

        log.info("비밀번호 재설정 요청 처리 완료: {}", email);
    }

    /**
     * 인증번호 확인
     */
    public boolean verifyCode(VerifyCodeRequestDto request) {
        return verificationService.verifyCode(request.getEmail(), request.getVerificationCode());
    }

    /**
     * 인증번호로 비밀번호 변경
     */
    @Transactional
    public void changePasswordWithVerificationCode(ChangePasswordWithCodeDto request) {
        String email = request.getEmail();
        String code = request.getVerificationCode();
        String newPassword = request.getNewPassword();

        // 1. 인증번호 확인
        if (!verificationService.verifyCode(email, code)) {
            throw new RuntimeException("인증번호가 유효하지 않거나 만료되었습니다.");
        }

        // 2. 비밀번호 확인 일치 검사
        if (!request.isPasswordMatching()) {
            throw new RuntimeException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 3. 회원 조회
        Member member = memberRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 4. 새 비밀번호 설정
        String encodedPassword = passwordEncoder.encode(newPassword);
        member.setUserPassword(encodedPassword);
        memberRepository.save(member);

        // 5. 비밀번호 변경 완료 이메일 발송
        emailService.sendPasswordChangedNotification(email);

        log.info("인증번호를 통한 비밀번호 변경 완료: {}", email);
    }

    /**
     * 현재 비밀번호 확인 후 변경 (기존 방식)
     */
    @Transactional
    public void changePasswordWithCurrentPassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, member.getUserPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 설정
        String encodedPassword = passwordEncoder.encode(newPassword);
        member.setUserPassword(encodedPassword);
        memberRepository.save(member);

        log.info("현재 비밀번호 확인 후 비밀번호 변경 완료: memberId={}", memberId);
    }
}
