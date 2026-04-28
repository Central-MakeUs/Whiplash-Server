package akuma.whiplash.domains.alarm.application.service;

import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmDeactivationLogService {

    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final MemberRepository memberRepository;
    private final AlarmDeactivationLogRepository alarmDeactivationLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCheckinSuccessLog(AlarmCheckinCompletedEvent event) {
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.getReferenceById(event.occurrenceId());
        MemberEntity member = memberRepository.getReferenceById(event.memberId());

        AlarmDeactivationLogEntity deactivationLog = AlarmMapper.mapToAlarmDeactivationLogEntity(
            occurrence,
            member,
            event.deviceId(),
            event.latitude(),
            event.longitude(),
            event.requestedAt(),
            event.processedAt()
        );
        alarmDeactivationLogRepository.save(deactivationLog);
    }
}
