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
     */
    @PostConstruct
    public void initData() {
        log.info("🚀 [초기화] 서버 시작! 대기질 데이터 상태 확인 중...");
        new Thread(() -> {
            try {
                if (airQualityService.isRealtimeDataFresh()) {
                    log.info("✅ 최신 데이터가 이미 존재합니다. 초기 수집을 건너뜁니다.");
                } else {
                    log.info("⚠️ 최신 데이터가 없습니다. 데이터를 수집합니다...");
                    scheduleRealtimeAirQuality(); // 실시간 수집
                    scheduleForecastAirQuality(); // 예보 수집 (초기 1회)
                }
            } catch (Exception e) {
                log.error("초기 데이터 확인 중 오류 발생", e);
            }
        }).start();
    }

    /**
     * 1. 실시간 대기질 정보 저장
     * 매시간 30분에 실행 (0 30 * * * *)
     */
    @Scheduled(cron = "0 30 * * * *")
    public void scheduleRealtimeAirQuality() {
        log.info("⏰ [스케줄러] 실시간 대기질 데이터 수집 시작 (전국)...");
        try {
            airQualityService.updateRealtimeData();
            log.info("✅ [스케줄러] 실시간 대기질 데이터 수집 완료");
        } catch (Exception e) {
            log.error("❌ [스케줄러] 실시간 데이터 수집 실패", e);
        }
    }

    /**
     * 2. 대기질 예보 정보 저장
     * 하루 2번 실행 (오전 9시 30분, 오후 2시 30분)
     * Cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 30 9,14 * * *")
    public void scheduleForecastAirQuality() {
        log.info("⏰ [스케줄러] 대기질 예보 데이터 수집 시작...");
        try {
            // Service 내부에서 중복(날짜+내용) 체크 후 저장함
            airQualityService.updateForecastData();
            log.info("✅ [스케줄러] 대기질 예보 데이터 수집 완료");
        } catch (Exception e) {
            log.error("❌ [스케줄러] 예보 데이터 수집 실패", e);
        }
    }
}