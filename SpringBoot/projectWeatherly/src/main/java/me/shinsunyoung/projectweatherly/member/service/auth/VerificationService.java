package me.shinsunyoung.projectweatherly.member.service.auth;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

//    private final RedisTemplate<String, String> redisTemplate;
//
//    @Value("${app.verification.code.expiration-minutes:10}")
//    private int expirationMinutes;
//
//    /**
//     * 인증번호 생성 및 저장
//     */
//    public String generateAndSaveVerificationCode(String email) {
//        // 6자리 랜덤 숫자 생성
//        String verificationCode = String.format("%06d", new Random().nextInt(999999));
//
//        // Redis에 저장 (키: verification:{email}, 값: 인증번호)
//        String key = "verification:" + email;
//        redisTemplate.opsForValue().set(key, verificationCode,
//                Duration.ofMinutes(expirationMinutes));
//
//        log.info("인증번호 생성: email={}, code={}", email, verificationCode);
//        return verificationCode;
//    }
//
//    /**
//     * 인증번호 검증
//     */
//    public boolean verifyCode(String email, String code) {
//        String key = "verification:" + email;
//        String savedCode = redisTemplate.opsForValue().get(key);
//
//        if (savedCode == null) {
//            log.warn("인증번호가 만료되었거나 존재하지 않음: {}", email);
//            return false;
//        }
//
//        boolean isValid = savedCode.equals(code);
//
//        if (isValid) {
//            // 인증 성공 시 삭제 또는 만료시간 단축
//            redisTemplate.delete(key);
//            log.info("인증번호 검증 성공: {}", email);
//        } else {
//            log.warn("인증번호 불일치: email={}, 입력코드={}, 저장코드={}",
//                    email, code, savedCode);
//        }
//
//        return isValid;
//    }
//
//    /**
//     * 인증번호 삭제
//     */
//    public void deleteVerificationCode(String email) {
//        String key = "verification:" + email;
//        redisTemplate.delete(key);
//        log.info("인증번호 삭제: {}", email);
//    }
//
//    /**
//     * 남은 유효시간 확인
//     */
//    public Long getRemainingTime(String email) {
//        String key = "verification:" + email;
//        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
//    }
}
