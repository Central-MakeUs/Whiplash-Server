package akuma.whiplash.domains.alarm.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OccurrenceStatus {
    SCHEDULED("울리기 전"),
    RINGING("울리는 중"),
    CHECKIN("위치 인증됨"),
    WATCH_AD("광고 시청됨"),
    PAYMENT("결제됨"),
    CANCELED("서버 정책상 취소"),
    MISSED("사용자가 끄지 못한 채 지나감");

    private final String description;
}
