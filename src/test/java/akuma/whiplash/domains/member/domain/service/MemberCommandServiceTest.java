package akuma.whiplash.domains.member.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("MemberCommandService Unit Test")
@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @InjectMocks
    private MemberCommandServiceImpl memberCommandService;

    @Mock private MemberRepository memberRepository;
    @Mock private AlarmRepository alarmRepository;
    @Mock private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock private AlarmOffLogRepository alarmOffLogRepository;
    @Mock private AlarmRingingLogRepository alarmRingingLogRepository;
    @Mock private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Mock private AlarmDeleteLogRepository alarmDeleteLogRepository;
    @Mock private AdSessionRepository adSessionRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MemberDeviceRepository memberDeviceRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisService redisService;

    @Nested
    @DisplayName("deleteMember - 회원 즉시 삭제")
    class DeleteMemberTest {

        @Test
        @DisplayName("성공: 회원과 관련 데이터를 삭제하고 Redis 토큰을 만료시킨다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_1.toMockEntity();
            given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));

            // when
            memberCommandService.deleteMember(member.getId(), "device");

            // then
            InOrder inOrder = inOrder(
                alarmRingingLogRepository,
                alarmDeactivationLogRepository,
                alarmDeleteLogRepository,
                alarmOffLogRepository,
                paymentRepository,
                adSessionRepository,
                alarmOccurrenceRepository,
                alarmRepository,
                memberDeviceRepository,
                memberRepository,
                jwtUtils,
                redisService
            );
            inOrder.verify(memberRepository).findByIdForUpdate(member.getId());
            inOrder.verify(alarmRingingLogRepository).deleteByMemberId(member.getId());
            inOrder.verify(alarmDeactivationLogRepository).deleteByMemberId(member.getId());
            inOrder.verify(alarmDeleteLogRepository).deleteByMemberId(member.getId());
            inOrder.verify(alarmOffLogRepository).deleteByMemberId(member.getId());
            inOrder.verify(paymentRepository).deleteByMemberId(member.getId());
            inOrder.verify(adSessionRepository).deleteByMemberId(member.getId());
            inOrder.verify(alarmOccurrenceRepository).deleteByMemberId(member.getId());
            inOrder.verify(alarmRepository).deleteByMemberId(member.getId());
            inOrder.verify(memberDeviceRepository).deleteByMemberId(member.getId());
            inOrder.verify(memberRepository).delete(member);
            inOrder.verify(jwtUtils).expireRefreshToken(member.getId(), "device");
            inOrder.verify(redisService).removeFcmTokenForDevice(member.getId(), "device");
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            given(memberRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.deleteMember(999L, "device"))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND)
                );
        }
    }
}
