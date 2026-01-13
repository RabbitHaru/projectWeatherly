package me.shinsunyoung.projectweatherly.member.repository;




import me.shinsunyoung.projectweatherly.member.domain.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByMemberId(Long memberId);

    List<NotificationSetting> findByWeatherAlertAgreeTrue();

    List<NotificationSetting> findByBoardNotificationAgreeTrue();
}