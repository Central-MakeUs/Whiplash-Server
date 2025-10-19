package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.global.log.NoMethodLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRingingNotificationScheduler {

    private final AlarmQueryService alarmQueryService;
    private final RedisService redisService;
    private final FcmService fcmService;

    // 10초 간격으로 실행
    @Scheduled(fixedRate = 10000, zone = "Asia/Seoul")
    @NoMethodLog
    public void sendRingingAlarmNotifications() {
        log.info("알람 울림 푸시 알림 전송 스케줄러 시작");
        try {
            List<RingingPushInfo> infos = alarmQueryService.getRingingNotificationTargets();
            if (infos.isEmpty()) {
                return;
            }

            List<RingingPushTargetDto> targets = infos.stream()
                .flatMap(info -> redisService.getFcmTokens(info.memberId()).stream()
                    .map(token -> RingingPushTargetDto.builder()
                        .token(token)
                        .alarmId(info.alarmId())
                        .memberId(info.memberId())
                        .build()))
                .toList();

            if (targets.isEmpty()) {
                return;
            }

            fcmService.sendRingingNotifications(targets);
        } finally {
            log.info("알람 울림 푸시 알림 전송 스케줄러 종료");
        }
    }
}
