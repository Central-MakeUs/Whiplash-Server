package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlarmCommandServiceImpl implements AlarmCommandService {

    private final AlarmRepository alarmRepository;
    private final MemberRepository memberRepository;

    @Override
    public void registerAlarm(RegisterAlarmRequest request, Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(CommonErrorCode.NOT_FOUND));

        AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, member);
        alarmRepository.save(alarm);
    }
}
