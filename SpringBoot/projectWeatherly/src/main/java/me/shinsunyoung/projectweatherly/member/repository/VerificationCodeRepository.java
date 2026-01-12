package me.shinsunyoung.projectweatherly.member.repository;

import me.shinsunyoung.projectweatherly.member.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    // 이메일로 가장 최근의 사용되지 않은 인증번호 찾기
    Optional<VerificationCode> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    // 이메일로 모든 사용되지 않은 인증번호 찾기
    List<VerificationCode> findByEmailAndUsedFalse(String email);

    // 이메일과 코드로 인증번호 찾기
    Optional<VerificationCode> findByEmailAndCode(String email, String code);

    // 만료시간 이전의 인증번호 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);

    // 특정 이메일의 모든 인증번호 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCode v WHERE v.email = :email")
    int deleteByEmail(@Param("email") String email);

    // 특정 시간 이후 생성된 인증번호 카운트
    @Query("SELECT COUNT(v) FROM VerificationCode v WHERE v.email = :email AND v.createdAt > :after")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("after") LocalDateTime after);

    // 사용되지 않고 만료된 인증번호 찾기
    @Query("SELECT v FROM VerificationCode v WHERE v.used = false AND v.expiresAt < :now")
    List<VerificationCode> findExpiredCodes(@Param("now") LocalDateTime now);

    // 이메일로 모든 인증번호 찾기 (최신순)
    List<VerificationCode> findByEmailOrderByCreatedAtDesc(String email);
}