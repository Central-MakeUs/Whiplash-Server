package akuma.whiplash.domains.ad.domain.service;

import akuma.whiplash.domains.ad.application.dto.request.AdMobRewardCallbackRequest;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.time.LocalDateTime;

public interface AdSessionService {

    AdSessionEntity createSession(
        MemberEntity member,
        AlarmEntity alarm,
        String deviceId,
        AdPurpose purpose,
        LocalDateTime expiresAt
    );

    void verifyRewardCallback(AdMobRewardCallbackRequest request);

    AdSessionEntity getVerifiedSessionForConsume(
        String adSessionId,
        Long memberId,
        Long alarmId,
        String deviceId,
        AdPurpose purpose
    );
}
