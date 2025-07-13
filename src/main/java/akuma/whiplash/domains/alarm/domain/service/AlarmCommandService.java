package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;

public interface AlarmCommandService {

    void registerAlarm(RegisterAlarmRequest request, Long memberId);
}
