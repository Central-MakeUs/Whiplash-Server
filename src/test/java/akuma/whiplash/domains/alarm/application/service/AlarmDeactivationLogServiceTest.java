package akuma.whiplash.domains.alarm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmDeactivationLogService Unit Test")
class AlarmDeactivationLogServiceTest {

    @Mock
    private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AlarmDeactivationLogRepository alarmDeactivationLogRepository;

    @InjectMocks
    private AlarmDeactivationLogService alarmDeactivationLogService;

    @Nested
    @DisplayName("saveCheckinSuccessLog - 도착 인증 성공 로그 저장")
    class SaveCheckinSuccessLogTest {

        @Test
        @DisplayName("성공: 현재 위치 좌표 원문은 저장하지 않는다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_10.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_10.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 501L, LocalDateTime.of(2026, 5, 2, 15, 30), OccurrenceStatus.CHECKIN);
            LocalDateTime processedAt = LocalDateTime.of(2026, 5, 2, 14, 30);
            AlarmCheckinCompletedEvent event = new AlarmCheckinCompletedEvent(
                occurrence.getId(),
                member.getId(),
                "device-uuid",
                processedAt,
                processedAt
            );
            given(alarmOccurrenceRepository.getReferenceById(occurrence.getId())).willReturn(occurrence);
            given(memberRepository.getReferenceById(member.getId())).willReturn(member);

            // when
            alarmDeactivationLogService.saveCheckinSuccessLog(event);

            // then
            ArgumentCaptor<AlarmDeactivationLogEntity> captor = ArgumentCaptor.forClass(AlarmDeactivationLogEntity.class);
            verify(alarmDeactivationLogRepository).save(captor.capture());
            AlarmDeactivationLogEntity savedLog = captor.getValue();
            assertThat(savedLog.getDeactivateType()).isEqualTo(DeactivateType.CHECKIN);
            assertThat(savedLog.getResult()).isEqualTo(DeactivationResult.SUCCESS);
            assertThat(savedLog.getRequestDeviceId()).isEqualTo("device-uuid");
            assertThat(Arrays.stream(AlarmDeactivationLogEntity.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("requestLatitude", "requestLongitude");
        }
    }
}
