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

    @PostConstruct
    public void initData() {
        log.info("🚀 [초기화] 대기질 데이터 상태 확인...");
        new Thread(() -> {
            try {
                if (airQualityService.isRealtimeDataFresh()) {
                    log.info("✅ 최신 데이터 존재. 초기 수집 생략.");
                } else {
                    log.info("⚠️ 최신 데이터 없음. 수집 시작.");
                    scheduleRealtimeAirQuality();
                    scheduleForecastAirQuality();
                }
            } catch (Exception e) {
                log.error("초기 데이터 확인 실패", e);
            }
        }).start();
    }

    @Scheduled(cron = "0 30 * * * *")
    public void scheduleRealtimeAirQuality() {
        log.info("⏰ [스케줄러] 실시간 대기질 데이터 수집...");
        try {
            airQualityService.updateRealtimeData();
        } catch (Exception e) {
        }
    }

    @Scheduled(cron = "0 30 9,14 * * *")
    public void scheduleForecastAirQuality() {
        log.info("⏰ [스케줄러] 예보 데이터 수집...");
        try {
            airQualityService.updateForecastData();
        } catch (Exception e) {
        }
    }
}