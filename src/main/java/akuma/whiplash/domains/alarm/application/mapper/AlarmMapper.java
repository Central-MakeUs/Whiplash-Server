package akuma.whiplash.domains.alarm.application.mapper;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.util.List;
import java.util.Objects;

public class AlarmMapper {

    public static AlarmEntity mapToAlarmEntity(RegisterAlarmRequest request, MemberEntity member) {
        return AlarmEntity.builder()
            .member(member)
            .alarmName(request.alarmName())
            .time(request.time())
            .repeatDays(mapToWeekdays(request.repeatDays()))
            .soundType(SoundType.from(request.soundType()))
            .latitude(request.latitude())
            .longitude(request.longitude())
            .address(request.address())
            .build();
    }

    private static List<Weekday> mapToWeekdays(List<String> repeatDays) {
        if (repeatDays == null) return List.of();

        return repeatDays.stream()
            .map(Weekday::from)
            .filter(Objects::nonNull)
            .toList();
    }
}

