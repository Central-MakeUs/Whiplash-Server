package akuma.whiplash.domains.alarm.application.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class BatchRetryExecutor {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 재시도용 작업을 일정 시간 후 실행
     * @param task 실행할 로직 (Runnable)
     * @param delayMillis 지연 시간 (ms)
     */
    public void scheduleRetry(Runnable task, long delayMillis) {
        scheduler.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }
}