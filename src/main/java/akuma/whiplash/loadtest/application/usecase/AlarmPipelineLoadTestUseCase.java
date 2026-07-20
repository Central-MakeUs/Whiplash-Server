package akuma.whiplash.loadtest.application.usecase;

import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.loadtest.domain.util.LoadTestMemberHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사례 2. 알람 파이프라인 — 배치 멱등성 및 재시도 부하 테스트 UseCase
 *
 * AS-IS: 중복 체크 없이 INSERT → unique constraint 충돌 시 예외 발생
 * TO-BE: existsByAlarmIdAndDate 스킵 로직 → 중복 없이 안전하게 재실행 가능
 *
 * 두 엔드포인트를 동일 alarm 데이터에 동일 횟수(iterations)로 실행하면
 * AS-IS.failed == TO-BE.skipped 가 되어 같은 "중복 방어" 건수가 메커니즘만 다름을 보여준다.
 */
@Profile("!prod")
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmPipelineLoadTestUseCase {

    private final MemberRepository memberRepository;
    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository occurrenceRepository;
    private final LoadTestMemberHelper memberHelper;

    // ===== Response Records =====

    public record SetupResponse(
        int memberCount,
        List<Long> memberIds,
        List<Long> alarmIds,
        long durationMs
    ) {}

    public record BatchResult(
        String scenario,
        int alarmCount,
        int iterations,
        int totalAttempts,
        int created,
        int skipped,
        int failed,
        long durationMs
    ) {}

    // ===== Setup =====

    public SetupResponse setup(int memberCount) {
        long start = System.currentTimeMillis();

        List<LoadTestMemberHelper.TestMemberInfo> members =
            memberHelper.createTestMembers("lt-pipeline", memberCount);

        List<Long> memberIds = members.stream().map(LoadTestMemberHelper.TestMemberInfo::memberId).toList();
        List<Long> alarmIds = new ArrayList<>();

        for (LoadTestMemberHelper.TestMemberInfo m : members) {
            MemberEntity member = memberRepository.findById(m.memberId()).orElseThrow();
            AlarmEntity alarm = AlarmEntity.builder()
                .member(member)
                .alarmPurpose("부하테스트 알람 - " + m.memberId())
                .time(LocalTime.of(8, 0))
                .repeatDays(List.of(
                    Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY,
                    Weekday.THURSDAY, Weekday.FRIDAY, Weekday.SATURDAY, Weekday.SUNDAY))
                .soundType(SoundType.KARINA_SCOLDING)
                .latitude(37.5665)
                .longitude(126.9780)
                .address("서울특별시 중구 세종대로 110")
                .build();
            alarm = alarmRepository.save(alarm);
            alarmIds.add(alarm.getId());
        }

        return new SetupResponse(memberCount, memberIds, alarmIds, System.currentTimeMillis() - start);
    }

    // ===== AS-IS: 중복 체크 없는 INSERT =====

    /**
     * unique constraint를 믿지 않고 매번 INSERT를 시도한다.
     * 2번째 이후 시도는 DataIntegrityViolationException으로 실패한다.
     * → created + failed = totalAttempts, failed = (iterations-1) × alarmCount
     *
     * @Transactional 없음: 각 save()가 독립 트랜잭션 → 예외가 전체 롤백을 일으키지 않음
     */
    public BatchResult createOccurrencesWithoutGuard(List<Long> memberIds, LocalDate date, int iterations) {
        List<AlarmEntity> alarms = fetchAlarms(memberIds);
        int created = 0, failed = 0;
        long start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            for (AlarmEntity alarm : alarms) {
                try {
                    AlarmOccurrenceEntity occ = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, date);
                    occurrenceRepository.save(occ);
                    created++;
                } catch (DataIntegrityViolationException e) {
                    failed++;
                }
            }
        }

        int totalAttempts = alarms.size() * iterations;
        log.info("[Pipeline AS-IS] alarms={}, iterations={}, created={}, failed={}",
            alarms.size(), iterations, created, failed);

        return new BatchResult("as-is-no-guard", alarms.size(), iterations,
            totalAttempts, created, 0, failed, System.currentTimeMillis() - start);
    }

    // ===== TO-BE: 스킵 로직으로 멱등성 보장 =====

    /**
     * 매 이터레이션마다 기존 occurrence를 조회하여 이미 존재하면 skip한다.
     * 2번째 이후 시도는 skipped로 처리되며 예외 없이 완료된다.
     * → created + skipped = totalAttempts, skipped = (iterations-1) × alarmCount
     */
    @Transactional
    public BatchResult createOccurrencesWithGuard(List<Long> memberIds, LocalDate date, int iterations) {
        List<AlarmEntity> alarms = fetchAlarms(memberIds);
        int created = 0, skipped = 0, failed = 0;
        long start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            Set<Long> existingIds = occurrenceRepository.findAlarmIdsByDate(date);

            for (AlarmEntity alarm : alarms) {
                if (existingIds.contains(alarm.getId())) {
                    skipped++;
                    continue;
                }
                try {
                    AlarmOccurrenceEntity occ = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, date);
                    occurrenceRepository.save(occ);
                    created++;
                    existingIds.add(alarm.getId());
                } catch (Exception e) {
                    failed++;
                    log.error("[Pipeline TO-BE] 생성 실패 alarmId={}: {}", alarm.getId(), e.getMessage());
                }
            }
        }

        int totalAttempts = alarms.size() * iterations;
        log.info("[Pipeline TO-BE] alarms={}, iterations={}, created={}, skipped={}, failed={}",
            alarms.size(), iterations, created, skipped, failed);

        return new BatchResult("to-be-with-guard", alarms.size(), iterations,
            totalAttempts, created, skipped, failed, System.currentTimeMillis() - start);
    }

    // ===== Cleanup =====

    @Transactional
    public void cleanup(List<Long> memberIds) {
        for (Long memberId : memberIds) {
            occurrenceRepository.deleteByMemberId(memberId);
            alarmRepository.deleteByMemberId(memberId);
        }
        memberHelper.deleteTestMembers(memberIds);
        log.info("[Pipeline Cleanup] members={}", memberIds.size());
    }

    // ===== Private Helpers =====

    private List<AlarmEntity> fetchAlarms(List<Long> memberIds) {
        List<AlarmEntity> alarms = new ArrayList<>();
        for (Long memberId : memberIds) {
            alarms.addAll(alarmRepository.findAllByMemberId(memberId));
        }
        return alarms;
    }
}
