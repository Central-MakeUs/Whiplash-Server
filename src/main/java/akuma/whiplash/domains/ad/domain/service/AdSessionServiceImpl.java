package akuma.whiplash.domains.ad.domain.service;

import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.constant.AdSessionStatus;
import akuma.whiplash.domains.ad.exception.AdErrorCode;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdSessionServiceImpl implements AdSessionService {

    private final AdSessionRepository adSessionRepository;
    private final AdMobRewardVerifier adMobRewardVerifier;
    private final TimeProvider timeProvider;

    @Override
    public AdSessionEntity createSession(
        MemberEntity member,
        AlarmEntity alarm,
        String deviceId,
        AdPurpose purpose,
        LocalDateTime expiresAt
    ) {
        return adSessionRepository.save(AdSessionEntity.builder()
            .adSessionId(UUID.randomUUID().toString())
            .member(member)
            .alarm(alarm)
            .deviceId(deviceId)
            .purpose(purpose)
            .status(AdSessionStatus.ISSUED)
            .expiresAt(expiresAt)
            .build());
    }

    @Override
    public void verifyRewardCallback(HttpServletRequest request) {
        AdMobRewardCallback callback = adMobRewardVerifier.verify(request);
        if (adSessionRepository.existsByTransactionId(callback.transactionId())) {
            return;
        }

        AdSessionEntity adSession = adSessionRepository.findByAdSessionId(callback.customData())
            .orElseThrow(() -> ApplicationException.from(AdErrorCode.AD_SESSION_NOT_FOUND));
        adSession.verify(callback, timeProvider.now());
    }

    @Override
    public AdSessionEntity getVerifiedSessionForConsume(
        String adSessionId,
        Long memberId,
        Long alarmId,
        String deviceId,
        AdPurpose purpose
    ) {
        AdSessionEntity adSession = adSessionRepository.findByAdSessionId(adSessionId)
            .orElseThrow(() -> ApplicationException.from(AdErrorCode.AD_SESSION_NOT_FOUND));
        adSession.validateForConsume(memberId, alarmId, deviceId, purpose, timeProvider.now());
        return adSession;
    }
}
