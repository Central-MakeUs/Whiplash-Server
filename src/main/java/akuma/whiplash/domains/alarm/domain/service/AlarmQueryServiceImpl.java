package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmQueryServiceImpl implements AlarmQueryService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<AlarmInfoPreviewResponse> getAlarms(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(memberId);
        LocalDate today = LocalDate.now();

        return alarms.stream()
            .map(alarm -> {
                Optional<AlarmOccurrenceEntity> todayOccurrenceOpt = alarmOccurrenceRepository
                    .findByAlarmIdAndDate(alarm.getId(), today);

                boolean isToggleOn = todayOccurrenceOpt
                    .map(
                        occurrence -> !DeactivateType.OFF.equals(occurrence.getDeactivateType())
                    )
                    .orElse(true); // 오늘 발생 내역 없으면 기본 true

                return AlarmMapper.mapToAlarmInfoPreviewResponse(alarm, isToggleOn);
            })
            .toList();
    }
}
