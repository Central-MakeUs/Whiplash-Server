package akuma.whiplash.domains.member.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.constant.AdSessionStatus;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.domain.constant.DeleteType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeleteLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.domain.constant.PaymentStatus;
import akuma.whiplash.domains.payment.domain.constant.PaymentType;
import akuma.whiplash.domains.payment.persistence.entity.PaymentEntity;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.infrastructure.redis.RedisService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("MemberCommandService MySQL Persistence Test")
@PersistenceTest
@Import(MemberCommandServiceImpl.class)
class MemberCommandServicePersistenceTest {

    @Autowired private MemberCommandService memberCommandService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberDeviceRepository memberDeviceRepository;
    @Autowired private AlarmRepository alarmRepository;
    @Autowired private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Autowired private AlarmRingingLogRepository alarmRingingLogRepository;
    @Autowired private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Autowired private AlarmDeleteLogRepository alarmDeleteLogRepository;
    @Autowired private AlarmOffLogRepository alarmOffLogRepository;
    @Autowired private AdSessionRepository adSessionRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private RedisService redisService;

    @Nested
    @DisplayName("deleteMember - FK 참조 데이터 즉시 삭제")
    class DeleteMemberTest {

        @Test
        @DisplayName("성공: 회원 소유 알람을 참조하는 모든 자식 행을 삭제한 뒤 회원을 삭제한다")
        void success() {
            // given
            MemberEntity withdrawingMember = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            MemberEntity otherMember = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(withdrawingMember));
            AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.save(
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toEntity(alarm)
            );
            memberDeviceRepository.save(MemberDeviceEntity.builder()
                .member(withdrawingMember)
                .deviceId("withdraw-device")
                .platform("IOS")
                .isLoggedIn(true)
                .timeZone("Asia/Seoul")
                .build());
            alarmRingingLogRepository.save(AlarmRingingLogEntity.builder()
                .alarmOccurrence(occurrence)
                .ringIndex(1)
                .ringedAt(LocalDateTime.of(2026, 8, 22, 10, 0))
                .build());
            alarmDeactivationLogRepository.save(AlarmDeactivationLogEntity.builder()
                .alarmOccurrence(occurrence)
                .member(otherMember)
                .deactivateType(DeactivateType.CHECKIN)
                .requestDeviceId("other-device")
                .requestedAt(LocalDateTime.of(2026, 8, 22, 10, 0))
                .processedAt(LocalDateTime.of(2026, 8, 22, 10, 1))
                .result(DeactivationResult.SUCCESS)
                .failReason("NONE")
                .build());
            alarmDeleteLogRepository.save(AlarmDeleteLogEntity.builder()
                .alarm(alarm)
                .member(otherMember)
                .deleteType(DeleteType.AD)
                .reason("test")
                .build());
            alarmOffLogRepository.save(AlarmOffLogEntity.builder()
                .alarm(alarm)
                .member(otherMember)
                .build());
            paymentRepository.save(PaymentEntity.builder()
                .member(otherMember)
                .alarm(alarm)
                .paymentId("payment-for-withdrawing-alarm")
                .paymentType(PaymentType.DELETE_ALARM)
                .amount(100)
                .status(PaymentStatus.SUCCESS)
                .build());
            adSessionRepository.save(AdSessionEntity.builder()
                .adSessionId("ad-session-for-withdrawing-alarm")
                .member(otherMember)
                .alarm(alarm)
                .deviceId("other-device")
                .purpose(AdPurpose.DELETE_ALARM)
                .status(AdSessionStatus.CONSUMED)
                .expiresAt(LocalDateTime.of(2026, 8, 22, 11, 0))
                .build());
            entityManager.flush();
            entityManager.clear();

            // when
            memberCommandService.deleteMember(withdrawingMember.getId(), "withdraw-device");
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(memberRepository.findById(withdrawingMember.getId())).isEmpty();
            assertThat(memberDeviceRepository.findAll()).isEmpty();
            assertThat(alarmRepository.findAll()).isEmpty();
            assertThat(alarmOccurrenceRepository.findAll()).isEmpty();
            assertThat(alarmRingingLogRepository.findAll()).isEmpty();
            assertThat(alarmDeactivationLogRepository.findAll()).isEmpty();
            assertThat(alarmDeleteLogRepository.findAll()).isEmpty();
            assertThat(alarmOffLogRepository.findAll()).isEmpty();
            assertThat(paymentRepository.findAll()).isEmpty();
            assertThat(adSessionRepository.findAll()).isEmpty();
            assertThat(memberRepository.findById(otherMember.getId())).isPresent();
        }
    }
}
