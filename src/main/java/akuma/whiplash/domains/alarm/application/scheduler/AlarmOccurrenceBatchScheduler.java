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
    private final BatchRetryExecutor batchRetryExecutor;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MILLIS = 5000;

    // 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 00:00:00
    public void scheduleDailyAlarmOccurrenceCreation() {
        log.info("알람 발생 내역 배치 생성 스케줄러 시작");
        runWithRetry(1);
        log.info("알람 발생 내역 배치 생성 스케줄러 종료");
    }

    /**
     * 재시도 포함한 실행
     */
    private void runWithRetry(int attempt) {
        AlarmOccurrenceCreateBatchResult result = alarmOccurrenceBatchService.createTodayAlarmOccurrences();

        if (result.failedCount() == 0) {
            log.info("[AlarmOccurrence Create Batch] 성공적으로 완료됨 (재시도 {}회)", attempt - 1);
            return;
        }

        if (attempt < MAX_RETRY) {
            log.warn("[AlarmOccurrence Create Batch] 일부 실패 - 재시도 {}/{} 예정", attempt, MAX_RETRY);
            batchRetryExecutor.scheduleRetry(() -> runWithRetry(attempt + 1), RETRY_DELAY_MILLIS);
        } else {
            log.error("[AlarmOccurrence Create Batch] 최대 재시도 초과 - 일부 알람 생성 실패 가능성 있음");
        }
    }
}