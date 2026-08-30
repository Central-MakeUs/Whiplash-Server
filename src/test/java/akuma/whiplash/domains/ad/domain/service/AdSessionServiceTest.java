package akuma.whiplash.domains.ad.domain.service;

import static akuma.whiplash.domains.ad.exception.AdErrorCode.AD_SESSION_ALREADY_CONSUMED;
import static akuma.whiplash.domains.ad.exception.AdErrorCode.AD_SESSION_EXPIRED;
import static akuma.whiplash.domains.ad.exception.AdErrorCode.AD_SESSION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import akuma.whiplash.domains.ad.application.dto.request.AdMobRewardCallbackRequest;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.constant.AdSessionStatus;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdSessionService Unit Test")
class AdSessionServiceTest {

    @Mock
    private AdSessionRepository adSessionRepository;
    @Mock
    private AdMobRewardVerifier adMobRewardVerifier;
    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private AdSessionServiceImpl adSessionService;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 7, 8, 10, 0);

    @Nested
    @DisplayName("verifyRewardCallback - AdMob SSV 콜백 검증")
    class VerifyRewardCallbackTest {

        @Test
        @DisplayName("성공: 유효한 콜백이면 광고 세션을 검증 완료로 변경한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AdSessionEntity adSession = issuedAdSession(member, alarm, "ad-session-id");
            AdMobRewardCallback callback = callback("ad-session-id", "transaction-id-001");
            AdMobRewardCallbackRequest request = request();

            given(adMobRewardVerifier.verify(request)).willReturn(callback);
            given(adSessionRepository.existsByTransactionId(callback.transactionId())).willReturn(false);
            given(adSessionRepository.findByAdSessionId(callback.customData())).willReturn(Optional.of(adSession));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            adSessionService.verifyRewardCallback(request);

            // then
            assertThat(adSession.getStatus()).isEqualTo(AdSessionStatus.VERIFIED);
            assertThat(adSession.getTransactionId()).isEqualTo(callback.transactionId());
            assertThat(adSession.getVerifiedAt()).isEqualTo(FIXED_NOW);
        }

        @Test
        @DisplayName("성공: 이미 처리한 transaction이면 세션 조회 없이 성공한다")
        void success_duplicateTransactionIgnored() {
            // given
            AdMobRewardCallback callback = callback("ad-session-id", "transaction-id-001");
            AdMobRewardCallbackRequest request = request();
            given(adMobRewardVerifier.verify(request)).willReturn(callback);
            given(adSessionRepository.existsByTransactionId(callback.transactionId())).willReturn(true);

            // when
            adSessionService.verifyRewardCallback(request);

            // then
            verify(adSessionRepository, never()).findByAdSessionId(callback.customData());
        }

        @Test
        @DisplayName("실패: custom_data에 해당하는 세션이 없으면 예외를 던진다")
        void fail_sessionNotFound() {
            // given
            AdMobRewardCallback callback = callback("unknown-session-id", "transaction-id-001");
            AdMobRewardCallbackRequest request = request();
            given(adMobRewardVerifier.verify(request)).willReturn(callback);
            given(adSessionRepository.existsByTransactionId(callback.transactionId())).willReturn(false);
            given(adSessionRepository.findByAdSessionId(callback.customData())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adSessionService.verifyRewardCallback(request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AD_SESSION_NOT_FOUND)
                );
        }

        @Test
        @DisplayName("실패: 세션이 만료됐으면 예외를 던진다")
        void fail_sessionExpired() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AdSessionEntity adSession = AdSessionEntity.builder()
                .adSessionId("ad-session-id")
                .member(member)
                .alarm(alarm)
                .deviceId("device-uuid")
                .purpose(AdPurpose.DELETE_ALARM)
                .status(AdSessionStatus.ISSUED)
                .expiresAt(FIXED_NOW.minusSeconds(1))
                .build();
            AdMobRewardCallback callback = callback("ad-session-id", "transaction-id-001");
            AdMobRewardCallbackRequest request = request();

            given(adMobRewardVerifier.verify(request)).willReturn(callback);
            given(adSessionRepository.existsByTransactionId(callback.transactionId())).willReturn(false);
            given(adSessionRepository.findByAdSessionId(callback.customData())).willReturn(Optional.of(adSession));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when & then
            assertThatThrownBy(() -> adSessionService.verifyRewardCallback(request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AD_SESSION_EXPIRED)
                );
        }

        @Test
        @DisplayName("실패: 이미 소비된 세션이면 예외를 던진다")
        void fail_sessionConsumed() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AdSessionEntity adSession = issuedAdSession(member, alarm, "ad-session-id");
            adSession.consume(FIXED_NOW.minusMinutes(1));
            AdMobRewardCallback callback = callback("ad-session-id", "transaction-id-001");
            AdMobRewardCallbackRequest request = request();

            given(adMobRewardVerifier.verify(request)).willReturn(callback);
            given(adSessionRepository.existsByTransactionId(callback.transactionId())).willReturn(false);
            given(adSessionRepository.findByAdSessionId(callback.customData())).willReturn(Optional.of(adSession));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when & then
            assertThatThrownBy(() -> adSessionService.verifyRewardCallback(request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AD_SESSION_ALREADY_CONSUMED)
                );
        }
    }

    @Nested
    @DisplayName("getVerifiedSessionForConsume - 광고 세션 소비 검증")
    class GetVerifiedSessionForConsumeTest {

        @Test
        @DisplayName("실패: 알람 끄기 세션을 다른 발생 건에 사용할 수 없다")
        void fail_occurrenceMismatch() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_01
                .toMockEntity(alarm, 100L, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            AdSessionEntity adSession = AdSessionEntity.builder()
                .adSessionId("ad-session-id")
                .member(member)
                .alarm(alarm)
                .alarmOccurrence(occurrence)
                .deviceId("device-uuid")
                .purpose(AdPurpose.STOP_ALARM)
                .status(AdSessionStatus.VERIFIED)
                .expiresAt(FIXED_NOW.plusMinutes(10))
                .build();
            given(adSessionRepository.findByAdSessionIdForUpdate("ad-session-id")).willReturn(Optional.of(adSession));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when & then
            assertThatThrownBy(() -> adSessionService.getVerifiedSessionForConsume(
                "ad-session-id", member.getId(), alarm.getId(), "device-uuid", AdPurpose.STOP_ALARM, 101L
            )).isInstanceOfSatisfying(ApplicationException.class, e ->
                assertThat(e.getCode()).isEqualTo(akuma.whiplash.domains.ad.exception.AdErrorCode.AD_SESSION_MISMATCH)
            );
        }
    }

    private AdSessionEntity issuedAdSession(MemberEntity member, AlarmEntity alarm, String adSessionId) {
        return AdSessionEntity.builder()
            .adSessionId(adSessionId)
            .member(member)
            .alarm(alarm)
            .deviceId("device-uuid")
            .purpose(AdPurpose.DELETE_ALARM)
            .status(AdSessionStatus.ISSUED)
            .expiresAt(FIXED_NOW.plusMinutes(10))
            .build();
    }

    private AdMobRewardCallback callback(String adSessionId, String transactionId) {
        return new AdMobRewardCallback(
            adSessionId,
            transactionId,
            "ad-unit-id",
            1,
            "coin"
        );
    }

    private AdMobRewardCallbackRequest request() {
        return new AdMobRewardCallbackRequest(
            "custom_data=ad-session-id&transaction_id=transaction-id-001",
            "ad-session-id",
            "transaction-id-001",
            "ad-unit-id",
            "1",
            "coin"
        );
    }
}
