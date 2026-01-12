package me.shinsunyoung.projectweatherly.member.repository;

import me.shinsunyoung.projectweatherly.member.domain.member.Member;

import me.shinsunyoung.projectweatherly.member.domain.member.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.security.AuthProvider;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이메일로 회원 찾기
    Optional<Member> findByUserEmail(String userEmail);

    // 이메일 존재 여부 확인
    boolean existsByUserEmail(String userEmail);

    // 소셜 로그인 제공자와 제공자 ID로 회원 찾기
    Optional<Member> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    // 활성 상태로 회원 찾기
    Optional<Member> findByUserEmailAndIsActiveTrue(String userEmail);

    // 특정 권한을 가진 회원들 찾기
    @Query("SELECT m FROM Member m WHERE m.userRole = :role AND m.isActive = true")
    List<Member> findActiveMembersByRole(@Param("role") String role);

    // 비활성화된 회원들 찾기
    List<Member> findByIsActiveFalse();

    // 활성화된 회원들 찾기
    List<Member> findByIsActiveTrue();

    // 활성 상태별 카운트
    long countByIsActive(boolean isActive);

    // 제공자별 회원 수
    long countByAuthProvider(AuthProvider authProvider);

    // 마지막 로그인 이후 일정 기간이 지난 회원들 찾기
    @Query("SELECT m FROM Member m WHERE m.lastLoginAt < :date AND m.isActive = true")
    List<Member> findInactiveMembersSince(@Param("date") java.time.LocalDateTime date);

    // 검색 기능 (이메일 또는 이름으로 검색)
    @Query("SELECT m FROM Member m WHERE " +
            "LOWER(m.userEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.userName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Member> searchByKeyword(@Param("keyword") String keyword);

    // 특정 권한의 회원 수
    long countByUserRole(String userRole);

    List<Member> findByUserRoleAndIsActive(MemberRole userRole, Boolean isActive);
}