package akuma.whiplash.domains.alarm.application.listener;

import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.application.service.AlarmDeactivationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmCheckinCompletedEventListener {

    private final AlarmDeactivationLogService alarmDeactivationLogService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AlarmCheckinCompletedEvent event) {
        try {
            alarmDeactivationLogService.saveCheckinSuccessLog(event);
        } catch (Exception e) {
            log.warn("Failed to save alarm deactivation log after checkin. occurrenceId={}", event.occurrenceId(), e);
        }
    }
}
