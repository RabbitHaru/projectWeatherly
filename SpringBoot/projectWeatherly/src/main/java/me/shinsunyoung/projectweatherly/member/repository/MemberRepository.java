package me.shinsunyoung.projectweatherly.member.repository;

import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
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
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderId(String providerId);

    Optional<Member> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.agreement LEFT JOIN FETCH m.notificationSetting WHERE m.id = :id")
    Optional<Member> findByIdWithAgreementAndNotification(@Param("id") Long id);

    // ✅ Refresh Token 관련 메서드들 추가

    // Refresh Token으로 회원 찾기
    Optional<Member> findByRefreshToken(String refreshToken);

    // 이메일과 인증 제공자로 회원 찾기
    Optional<Member> findByEmailAndAuthProvider(String email, AuthProvider authProvider);

    // 활성화된 회원들 조회
    List<Member> findByIsActiveTrue();

    // 특정 권한을 가진 회원들 조회
    List<Member> findByRole(String role);

    // Refresh Token 업데이트 메서드
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.refreshToken = :refreshToken, m.refreshTokenExpiry = :expiry WHERE m.id = :memberId")
    void updateRefreshToken(@Param("memberId") Long memberId,
                            @Param("refreshToken") String refreshToken,
                            @Param("expiry") LocalDateTime expiry);

    // Refresh Token 만료시키기
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.refreshToken = NULL, m.refreshTokenExpiry = NULL WHERE m.id = :memberId")
    void clearRefreshToken(@Param("memberId") Long memberId);

    // 회원 상태 변경 (활성/비활성)
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.isActive = :isActive WHERE m.id = :memberId")
    void updateMemberStatus(@Param("memberId") Long memberId,
                            @Param("isActive") Boolean isActive);

    // 프로필 이미지 업데이트
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.profileImage = :profileImage WHERE m.id = :memberId")
    void updateProfileImage(@Param("memberId") Long memberId,
                            @Param("profileImage") String profileImage);

    // 닉네임 업데이트 및 중복 확인
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.nickname = :nickname WHERE m.id = :memberId")
    void updateNickname(@Param("memberId") Long memberId,
                        @Param("nickname") String nickname);

    // 비밀번호 업데이트
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.password = :password WHERE m.id = :memberId")
    void updatePassword(@Param("memberId") Long memberId,
                        @Param("password") String password);

    // 소셜 로그인 정보 연결
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.authProvider = :authProvider, m.providerId = :providerId WHERE m.id = :memberId")
    void updateAuthProvider(@Param("memberId") Long memberId,
                            @Param("authProvider") AuthProvider authProvider,
                            @Param("providerId") String providerId);

    // 회원 검색 기능
    @Query("SELECT m FROM Member m WHERE m.email LIKE %:keyword% OR m.nickname LIKE %:keyword%")
    List<Member> searchMembers(@Param("keyword") String keyword);

    // 특정 기간 내 가입한 회원 조회
    @Query("SELECT m FROM Member m WHERE m.createdAt BETWEEN :startDate AND :endDate")
    List<Member> findMembersByJoinDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // 권한별 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role = :role")
    Long countByRole(@Param("role") String role);

    // 활성/비활성 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.isActive = :isActive")
    Long countByIsActive(@Param("isActive") Boolean isActive);

    // Refresh Token이 만료되지 않은 회원 찾기
    @Query("SELECT m FROM Member m WHERE m.refreshToken IS NOT NULL AND m.refreshTokenExpiry > :currentTime")
    List<Member> findMembersWithValidRefreshToken(@Param("currentTime") LocalDateTime currentTime);
}