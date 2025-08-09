package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmReminderScheduler {

    private final AlarmQueryService alarmQueryService;
    private final RedisService redisService;
    private final FcmService fcmService;

//    @Scheduled(cron = "0 * * * * *") // 매 분마다 실행
    public void sendPreAlarmNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDate date = now.toLocalDate();
        LocalTime start = now.toLocalTime().plusMinutes(59);
        LocalTime end = now.toLocalTime().plusMinutes(61);
				
        // 1. DB에서 알림 대상 필터링
        List<OccurrencePushInfo> infos = alarmQueryService.findPushTargetsByTimeRange(date, start, end);

        // 2. Redis에서 FCM 토큰 조회 후 PushTargetDto로 변환
        List<PushTargetDto> targets = infos.stream()
            .flatMap(info -> redisService.getFcmTokens(info.memberId()).stream()
                .map(token -> new PushTargetDto(
                    token, info.address(), info.memberId(), info.occurrenceId()
                ))
            ).toList();

        // 3. FCM 전송
        fcmService.sendBulkNotification(targets);
    }

}
