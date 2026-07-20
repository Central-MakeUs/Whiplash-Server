package akuma.whiplash.domains.alarm.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlarmStatus {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    DELETED("삭제");

    private final String description;
}
