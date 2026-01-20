package me.shinsunyoung.projectweatherly.airquality.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.airquality.service.AirQualityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AirQualityScheduler {

    private final AirQualityService airQualityService;

    /**
     * 서버 시작 시 초기화
     * - DB에 최신 데이터(1시간 이내)가 없으면 수집
     * - 있으면 API 호출 스킵 (쿼터 절약)
     */
    @PostConstruct
    public void initData() {
        log.info("🚀 [초기화] 서버 시작! 대기질 데이터 상태 확인 중...");
        new Thread(() -> {
            try {
                // 1. 최신 데이터가 있는지 확인 (실시간 데이터 기준)
                if (airQualityService.isRealtimeDataFresh()) {
                    log.info("✅ 최신 데이터가 이미 존재합니다. 초기 수집을 건너뜁니다.");
                } else {
                    log.info("⚠️ 최신 데이터가 없습니다. 데이터를 수집합니다...");
                    // 2. 데이터가 없으면 즉시 수집 실행
                    scheduleAirQualityUpdate();
                }
            } catch (Exception e) {
                log.error("초기 데이터 확인 중 오류 발생", e);
            }
        }).start();
    }

    /**
     * 통합 대기질 정보 수집 (실시간 + 예보)
     * 매시간 30분에 실행 (0 30 * * * *)
     */
    @Scheduled(cron = "0 30 * * * *")
    public void scheduleAirQualityUpdate() {
        log.info("⏰ [스케줄러] 대기질 데이터(실시간+예보) 통합 수집 시작...");
        try {
            // 1. 실시간 대기질 수집
            airQualityService.updateRealtimeData();

            // 2. 대기질 예보 수집 (이제 매시간 같이 실행됨)
            airQualityService.updateForecastData();

            log.info("✅ [스케줄러] 모든 대기질 데이터 수집 완료");
        } catch (Exception e) {
            log.error("❌ [스케줄러] 데이터 수집 실패", e);
        }
    }
}