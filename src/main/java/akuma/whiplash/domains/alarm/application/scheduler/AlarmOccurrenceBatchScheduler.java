package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.AlarmOccurrenceCreateBatchResult;
import akuma.whiplash.domains.alarm.domain.service.AlarmOccurrenceBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AlarmOccurrenceBatchScheduler {

    private final AlarmOccurrenceBatchService alarmOccurrenceBatchService;

    private static final int MAX_RETRY = 3;
    private int retryCount = 0;

    // 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 00:00:00
    public void scheduleDailyAlarmOccurrenceCreation() {
        log.info("[AlarmOccurrence Create Batch] 스케줄 시작");
        runWithRetry();
        log.info("[AlarmOccurrence Create Batch] 스케줄 종료");
    }

    /**
     * 재시도 포함한 실행
     */
    private void runWithRetry() {
        while (retryCount < MAX_RETRY) {
            AlarmOccurrenceCreateBatchResult result = alarmOccurrenceBatchService.createTodayAlarmOccurrences();

            if (result.failedCount() == 0) {
                log.info("[AlarmOccurrence Create Batch] 성공적으로 완료 (재시도 {}회)", retryCount);
                retryCount = 0;
                break;
            }

            retryCount++;
            log.warn("[AlarmOccurrence Create Batch] 일부 실패 발생 - 재시도 {}/{}", retryCount, MAX_RETRY);

            try {
                Thread.sleep(5000L); // 5초 대기 후 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry sleep interrupted", e);
                break;
            }
        }

        if (retryCount >= MAX_RETRY) {
            log.error("[AlarmOccurrence Create Batch] 최대 재시도 횟수 초과 - 일부 알람은 생성되지 않았을 수 있음");
            retryCount = 0; // 다음 날을 위해 초기화
        }
    }
}