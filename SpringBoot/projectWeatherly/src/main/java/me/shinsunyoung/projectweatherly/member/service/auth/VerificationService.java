package me.shinsunyoung.projectweatherly.member.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.entity.VerificationCode;
import me.shinsunyoung.projectweatherly.member.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;

    @Value("${app.verification.code.expiration-minutes:10}")
    private int expirationMinutes;

    @Value("${app.verification.max-attempts:5}")
    private int maxAttempts;

    /**
     * 인증번호 생성 및 MariaDB 저장
     */
    public String generateAndSaveVerificationCode(String email) {
        // 1. 기존 인증번호 확인 및 만료 처리
        cleanupOldCodes(email);

        // 2. 요청 횟수 체크
        if (isRateLimited(email)) {
            throw new RuntimeException("너무 많은 요청입니다. 잠시 후 다시 시도해주세요.");
        }

        // 3. 6자리 랜덤 숫자 생성
        String verificationCode = generateRandomCode();

        // 4. MariaDB에 저장
        VerificationCode code = VerificationCode.builder()
                .email(email)
                .code(verificationCode)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .used(false)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        verificationCodeRepository.save(code);

        log.info("인증번호 생성 및 MariaDB 저장: email={}, code={}, 만료시간={}",
                email, verificationCode, code.getExpiresAt());

        return verificationCode;
    }

    /**
     * 인증번호 검증 (MariaDB에서 확인)
     */
    @Transactional(readOnly = true)
    public boolean verifyCode(String email, String inputCode) {
        Optional<VerificationCode> codeOpt = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (codeOpt.isEmpty()) {
            log.warn("인증번호가 존재하지 않음: email={}", email);
            return false;
        }

        VerificationCode code = codeOpt.get();

        // 1. 만료 체크
        if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("인증번호가 만료됨: email={}, 만료시간={}",
                    email, code.getExpiresAt());
            code.markAsUsed();  // 만료된 코드는 사용 처리
            verificationCodeRepository.save(code);
            return false;
        }

        // 2. 이미 사용된 코드인지 확인
        if (code.isUsed()) {
            log.warn("이미 사용된 인증번호: email={}", email);
            return false;
        }

        // 3. 시도 횟수 체크
        if (code.getAttempts() >= maxAttempts) {
            log.warn("최대 시도 횟수 초과: email={}, attempts={}",
                    email, code.getAttempts());
            code.markAsUsed();
            verificationCodeRepository.save(code);
            return false;
        }

        // 4. 코드 일치 확인
        boolean isValid = code.getCode().equals(inputCode);

        // 5. 시도 횟수 증가
        code.incrementAttempts();

        if (isValid) {
            // 인증 성공 시 사용 처리
            code.markAsUsed();
            log.info("인증번호 검증 성공: email={}", email);
        } else {
            log.warn("인증번호 불일치: email={}, 입력코드={}, 저장코드={}",
                    email, inputCode, code.getCode());
        }

        verificationCodeRepository.save(code);
        return isValid;
    }

    /**
     * 인증번호 삭제
     */
    public void deleteVerificationCode(String email) {
        int deletedCount = verificationCodeRepository.deleteByEmail(email);
        log.info("인증번호 삭제: email={}, 삭제된 레코드={}개", email, deletedCount);
    }

    /**
     * 남은 유효시간 확인 (초 단위)
     */
    @Transactional(readOnly = true)
    public Long getRemainingTime(String email) {
        Optional<VerificationCode> codeOpt = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (codeOpt.isEmpty()) {
            return 0L;
        }

        VerificationCode code = codeOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (code.isUsed() || code.getExpiresAt().isBefore(now)) {
            return 0L;
        }

        return java.time.Duration.between(now, code.getExpiresAt()).getSeconds();
    }

    /**
     * 인증번호 재전송 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean canResendCode(String email) {
        Optional<VerificationCode> codeOpt = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (codeOpt.isEmpty()) {
            return true; // 기존 코드 없음 → 재전송 가능
        }

        VerificationCode code = codeOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 마지막 생성 시간으로부터 1분 이상 지났는지 확인
        return code.getCreatedAt().plusMinutes(1).isBefore(now);
    }

    /**
     * 6자리 랜덤 숫자 생성
     */
    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * 이메일별 기존 코드 정리
     */
    private void cleanupOldCodes(String email) {
        // 사용되지 않은 기존 코드들을 사용 처리
        verificationCodeRepository.findByEmailAndUsedFalse(email)
                .forEach(code -> {
                    code.markAsUsed();
                    verificationCodeRepository.save(code);
                });
    }

    /**
     * 요청 제한 체크
     */
    private boolean isRateLimited(String email) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentRequests = verificationCodeRepository
                .countByEmailAndCreatedAtAfter(email, oneMinuteAgo);

        return recentRequests >= 3; // 1분당 3회 제한
    }

    /**
     * 정기적으로 만료된 인증번호 정리 (매시간 실행)
     */
    @Scheduled(cron = "0 0 * * * *")  // 매시간
    @Transactional
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = verificationCodeRepository.deleteByExpiresAtBefore(now);

        if (deletedCount > 0) {
            log.info("만료된 인증번호 {}개 정리 완료", deletedCount);
        }
    }

    /**
     * 특정 이메일의 인증 상태 확인
     */
    @Transactional(readOnly = true)
    public VerificationStatus checkVerificationStatus(String email) {
        Optional<VerificationCode> codeOpt = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (codeOpt.isEmpty()) {
            return VerificationStatus.NO_CODE;
        }

        VerificationCode code = codeOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (code.isUsed()) {
            return VerificationStatus.USED;
        }

        if (code.getExpiresAt().isBefore(now)) {
            return VerificationStatus.EXPIRED;
        }

        if (code.getAttempts() >= maxAttempts) {
            return VerificationStatus.MAX_ATTEMPTS_EXCEEDED;
        }

        return VerificationStatus.VALID;
    }

    /**
     * 인증 상태 Enum
     */
    public enum VerificationStatus {
        NO_CODE,           // 인증번호 없음
        VALID,            // 유효함
        EXPIRED,          // 만료됨
        USED,             // 이미 사용됨
        MAX_ATTEMPTS_EXCEEDED  // 최대 시도 횟수 초과
    }
}