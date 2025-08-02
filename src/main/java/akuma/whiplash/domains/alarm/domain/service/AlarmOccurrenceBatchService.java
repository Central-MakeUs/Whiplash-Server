package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.etc.AlarmOccurrenceCreateBatchResult;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmOccurrenceBatchService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;

    /**
     * 오늘 울릴 알람 중 아직 alarm_occurrence가 없는 알람에 대해 이력을 생성합니다.
     * - 이미 생성된 알람은 건너뜀
     * - 중간에 실패한 알람은 오류만 로깅하고 전체 배치에는 영향 없음
     */
    @Transactional
    public AlarmOccurrenceCreateBatchResult createTodayAlarmOccurrences() {
        LocalDate today = LocalDate.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();

        // 1. 오늘 반복 요일에 해당하는 알람만 DB에서 조회 (native query + LIKE)
        String likeKeyword = "\"" + todayDayOfWeek.name() + "\""; // ex: "MONDAY"
        List<AlarmEntity> todayAlarms = alarmRepository.findByRepeatDaysLike(likeKeyword);

        log.info("[AlarmOccurrence Create Batch] 오늘({}) 울릴 알람 수: {}", todayDayOfWeek, todayAlarms.size());

        // 2. 이미 alarm_occurrence가 생성된 알람 ID 목록 조회
        Set<Long> existingAlarmIds = alarmOccurrenceRepository.findAlarmIdsByDate(today);

        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        // 3. 생성되지 않은 알람에 대해서만 alarm_occurrence 생성
        for (AlarmEntity alarm : todayAlarms) {
            if (existingAlarmIds.contains(alarm.getId())) {
                skippedCount++;
                continue;
            }

            try {
                AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, today);
                alarmOccurrenceRepository.save(occurrence);
                createdCount++;

                log.info("[AlarmOccurrence Create Batch] 생성 완료: alarmId={}, date={}", alarm.getId(), today);

            } catch (Exception e) {
                failedCount++;
                log.error("[AlarmOccurrence Create Batch] 생성 실패: alarmId={}, error={}", alarm.getId(), e.getMessage());
            }
        }

        log.info("[AlarmOccurrence Create Batch] 완료 - 생성: {}, 건너뜀: {}, 실패: {}",
            createdCount, skippedCount, failedCount);

        return AlarmOccurrenceCreateBatchResult.builder()
            .createdCount(createdCount)
            .skippedCount(skippedCount)
            .failedCount(failedCount)
            .build();
    }
}