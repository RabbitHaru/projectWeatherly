package me.shinsunyoung.projectweatherly.member.repository;

import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ==================== 기본 조회 메서드 ====================
    Optional<Member> findByEmail(String email);

    Optional<Member> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByProviderId(String providerId);

    // ==================== OAuth2 관련 메서드 ====================
    Optional<Member> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    @Query("SELECT m FROM Member m WHERE m.email = :email AND m.authProvider = :authProvider")
    Optional<Member> findByEmailAndAuthProvider(@Param("email") String email,
                                                @Param("authProvider") AuthProvider authProvider);

    // ==================== 연관 엔티티 조회 ====================
    @Query("SELECT DISTINCT m FROM Member m " +
            "LEFT JOIN FETCH m.agreement " +
            "WHERE m.id = :id")
    Optional<Member> findByIdWithAgreementAndNotification(@Param("id") Long id);


    // ==================== 회원 상태 관리 ====================
    List<Member> findByIsActiveTrue();

    List<Member> findByRole(MemberRole role);

    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.isActive = :isActive WHERE m.id = :memberId")
    void updateMemberStatus(@Param("memberId") Long memberId,
                            @Param("isActive") Boolean isActive);

    // ==================== 프로필 정보 업데이트 ====================
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.profileImage = :profileImage WHERE m.id = :memberId")
    void updateProfileImage(@Param("memberId") Long memberId,
                            @Param("profileImage") String profileImage);

    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.nickname = :nickname WHERE m.id = :memberId")
    void updateNickname(@Param("memberId") Long memberId,
                        @Param("nickname") String nickname);

    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.password = :password WHERE m.id = :memberId")
    void updatePassword(@Param("memberId") Long memberId,
                        @Param("password") String password);

    // ==================== OAuth2 정보 업데이트 ====================
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.authProvider = :authProvider, m.providerId = :providerId WHERE m.id = :memberId")
    void updateAuthProvider(@Param("memberId") Long memberId,
                            @Param("authProvider") AuthProvider authProvider,
                            @Param("providerId") String providerId);

    // ==================== 검색 및 통계 메서드 ====================

    // [기존 리스트 반환 메서드는 유지하거나, 필요 없으면 삭제하셔도 됩니다]
    @Query("SELECT m FROM Member m WHERE " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Member> searchMembers(@Param("keyword") String keyword);

    // ★ [NEW] 페이징 지원 검색 (닉네임 또는 이메일에 포함)
    Page<Member> findByNicknameContainingOrEmailContaining(String nickname, String email, Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.createdAt BETWEEN :startDate AND :endDate")
    List<Member> findMembersByJoinDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.role = :role")
    Long countByRole(@Param("role") MemberRole role);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.isActive = :isActive")
    Long countByIsActive(@Param("isActive") Boolean isActive);

    // ==================== 관리자용 통계 메서드 ====================
    @Query("SELECT COUNT(m) FROM Member m WHERE m.authProvider = :authProvider")
    Long countByAuthProvider(@Param("authProvider") AuthProvider authProvider);

    @Query("SELECT COUNT(m) FROM Member m WHERE DATE(m.createdAt) = CURRENT_DATE")
    Long countNewMembersToday();

    // [관리자용] 가장 최근에 가입한 회원 5명 조회
    List<Member> findTop5ByOrderByCreatedAtDesc();

    // 전체 조회 (페이징)
    Page<Member> findAll(Pageable pageable);
}