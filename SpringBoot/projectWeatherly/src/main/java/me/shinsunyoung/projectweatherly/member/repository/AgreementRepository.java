package me.shinsunyoung.projectweatherly.member.repository;



import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    Optional<Agreement> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);
}
