package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmSendResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmReminderScheduler {

    private final AlarmQueryService alarmQueryService;
    private final RedisService redisService;
    private final FcmService fcmService;
    private final AlarmCommandService alarmCommandService;

    // @Scheduled(cron = "0 * * * * *") // 매 분
    public void sendPreAlarmNotifications() {
        log.info("[AlarmReminderScheduler.sendPreAlarmNotifications] 알람 울리기 1시간 전 푸시 알림 전송 스케줄러 시작");
        // 0. 시간 기준 (분 단위 정렬)
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime windowStart = now.plusMinutes(59);
        LocalDateTime windowEnd = now.plusMinutes(61);

        // 1. DB에서 알림 대상 필터링 (자정 크로스 안전)
        List<OccurrencePushInfo> infos = alarmQueryService.getPreNotificationTargets(windowStart, windowEnd);

        if (infos.isEmpty())
            return;

        // 2. Redis에서 FCM 토큰 조회 → PushTargetDto 변환
        List<PushTargetDto> targets = infos.stream()
            .flatMap(info ->
                redisService.getFcmTokens(info.memberId()).stream()
                    .map(token -> PushTargetDto.builder()
                        .token(token)
                        .address(info.address())
                        .memberId(info.memberId())
                        .occurrenceId(info.occurrenceId())
                        .build()
                    )
            )
            .toList(); // <-- 여기서 한 번만 호출

        if (targets.isEmpty())
            return;

        FcmSendResult result = fcmService.sendBulkNotification(targets);

        // 3. 성공한 occurrence만 reminderSent=true 벌크 업데이트
        if (!result.getSuccessOccurrenceIds().isEmpty()) {
            alarmCommandService.markReminderSent(result.getSuccessOccurrenceIds());
        }

        // 4. 무효 토큰 정리(멤버 매핑 정보가 있으면 거기서 제거)
        for (Map.Entry<Long, List<String>> e : result.getMemberToTokens().entrySet()) {
            Long memberId = e.getKey();
            for (String bad : result.getInvalidTokens()) {
                redisService.removeInvalidToken(memberId, bad);
            }
        }

        log.info("[AlarmReminderScheduler.sendPreAlarmNotifications] 알람 울리기 1시간 전 푸시 알림 전송 스케줄러 종료");

    }

}
