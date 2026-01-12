package me.shinsunyoung.projectweatherly.member.service.verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(10); // 10분 유효
    private static final String CODE_PREFIX = "VERIFICATION:";

    /**
     * 인증번호 생성 및 저장
     */
    public String generateAndSaveVerificationCode(String email, String purpose) {
        // 6자리 랜덤 숫자 생성
        String code = String.format("%06d", new Random().nextInt(1000000));

        // Redis에 저장 (key: VERIFICATION:PASSWORD_RESET:email@example.com)
        String key = CODE_PREFIX + purpose + ":" + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION);

        log.info("인증번호 생성 및 저장: email={}, code={}", email, code);
        return code;
    }

    /**
     * 인증번호 확인
     */
    public boolean verifyCode(String email, String code, String purpose) {
        String key = CODE_PREFIX + purpose + ":" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        boolean isValid = code.equals(storedCode);
        log.info("인증번호 확인: email={}, valid={}", email, isValid);

        return isValid;
    }

    /**
     * 인증번호 사용 처리 (삭제)
     */
    public void markCodeAsUsed(String email, String code, String purpose) {
        String key = CODE_PREFIX + purpose + ":" + email;
        redisTemplate.delete(key);
        log.info("인증번호 사용 처리 완료: email={}", email);
    }
}