package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberDeviceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.common.fixture.PaymentFixture;
import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.service.AuditLogRecorder;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.DeleteType;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeleteLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.persistence.entity.PaymentEntity;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.payment.PaymentVerificationPort;
import akuma.whiplash.infrastructure.redis.RingingAlarmRedisRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmCommandService Unit Test")
class AlarmCommandServiceTest {

    @Mock
    private AlarmRepository alarmRepository;
    @Mock
    private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock
    private AlarmRingingLogRepository alarmRingingLogRepository;
    @Mock
    private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Mock
    private AlarmDeleteLogRepository alarmDeleteLogRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberDeviceRepository memberDeviceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private List<PaymentVerificationPort> paymentVerificationPorts;
    @Mock
    private PaymentVerificationPort paymentVerificationPort;
    @Mock
    private RingingAlarmRedisRepository ringingAlarmRedisRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TimeProvider timeProvider;
    @Mock
    private AuditLogRecorder auditLogRecorder;

    @InjectMocks
    private AlarmCommandServiceImpl alarmCommandService;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 2, 14, 30);

    @BeforeEach
    void setUp() {
        lenient().when(timeProvider.now()).thenReturn(FIXED_NOW);
        lenient().when(timeProvider.today()).thenReturn(FIXED_NOW.toLocalDate());
    }

    @Nested
    @DisplayName("createAlarm - 알람 등록")
    class CreateAlarmTest {

        @Test
        @DisplayName("성공: 회원이 알람 등록을 요청하면 알람과 첫 발생 내역이 저장된다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_05;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
            );
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

            // when
            alarmCommandService.createAlarm(request, member.getId());

            // then
            verify(alarmRepository).save(any(AlarmEntity.class));
            verify(alarmOccurrenceRepository).save(any(AlarmOccurrenceEntity.class));
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 알람 등록 시 예외가 발생한다")
        void fail_memberNotFound() {

            // given
            AlarmFixture fixture = AlarmFixture.ALARM_06;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                    fixture.getSoundType().getDescription()
            );
            given(memberRepository.findById(MemberFixture.MEMBER_6.getId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.createAlarm(request, MemberFixture.MEMBER_6.getId()))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 같은 목적의 알람이 이미 존재하면 알람 등록 시 예외가 발생한다")
        void fail_duplicateAlarmPurpose() {
            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_05;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
            );
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(alarmRepository.existsByMemberIdAndAlarmPurpose(member.getId(), request.alarmPurpose())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.createAlarm(request, member.getId()))
                .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("checkinAlarm - 도착 인증")
    class CheckinAlarmTest {

        private AlarmEntity buildAlarm(MemberEntity member, AlarmFixture fixture) {
            return fixture.toMockEntity(member);
        }

        private AlarmOccurrenceEntity buildOccurrence(AlarmEntity alarm, Long occurrenceId, LocalDateTime scheduledAt, OccurrenceStatus status) {
            return AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toMockEntity(alarm, occurrenceId, scheduledAt, status);
        }

        private AlarmCheckinRequest buildRequest(AlarmOccurrenceEntity occurrence, Double latitude, Double longitude) {
            return new AlarmCheckinRequest(occurrence.getId(), "device-uuid", latitude, longitude);
        }

        @Test
        @DisplayName("성공: 허용 반경 내에서 도착 인증에 성공한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_10.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_10;
            AlarmEntity alarm = buildAlarm(member, fixture);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            AlarmOccurrenceEntity occurrence = buildOccurrence(alarm, 501L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            AlarmOccurrenceEntity nextOccurrence = buildOccurrence(alarm, 502L, FIXED_NOW.plusDays(1), OccurrenceStatus.SCHEDULED);
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));
            given(alarmOccurrenceRepository.findNextScheduledByAlarmIds(eq(List.of(alarm.getId())), eq(OccurrenceStatus.SCHEDULED), any(LocalDateTime.class)))
                .willReturn(List.of(nextOccurrence));

            AlarmCheckinRequest request = buildRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when
            AlarmCheckinResponse response = alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request);

            // then
            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.CHECKIN);
            assertThat(alarm.getRevision()).isEqualTo(2);
            assertThat(response.alarmId()).isEqualTo(alarm.getId());
            assertThat(response.alarmRevision()).isEqualTo(2);
            assertThat(response.nextOccurrence()).isNotNull();
            assertThat(response.nextOccurrence().occurrenceId()).isEqualTo(502L);
            verify(eventPublisher).publishEvent(any(AlarmCheckinCompletedEvent.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람이면 예외가 발생한다")
        void fail_alarmNotFound() {
            // given
            given(alarmRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(
                1L,
                1L,
                new AlarmCheckinRequest(1L, "device-uuid", 0.0, 0.0)
            ))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 알람이면 예외가 발생한다")
        void fail_permissionDenied() {
            // given
            MemberEntity owner = MemberFixture.MEMBER_11.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_11;
            AlarmEntity alarm = buildAlarm(owner, fixture);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            AlarmOccurrenceEntity occurrence = buildOccurrence(alarm, 601L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            AlarmCheckinRequest request = buildRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(999L, alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 알람 발생 회차가 없으면 예외가 발생한다")
        void fail_occurrenceNotFound() {
            // given
            MemberEntity member = MemberFixture.MEMBER_12.toMockEntity();
            AlarmEntity alarm = buildAlarm(member, AlarmFixture.ALARM_12);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByIdAndAlarmId(999L, alarm.getId())).willReturn(Optional.empty());

            AlarmCheckinRequest request = new AlarmCheckinRequest(999L, "device-uuid", alarm.getLatitude(), alarm.getLongitude());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 이미 도착 인증된 알람이면 예외가 발생한다")
        void fail_alreadyDeactivated() {
            // given
            MemberEntity member = MemberFixture.MEMBER_13.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_13;
            AlarmEntity alarm = buildAlarm(member, fixture);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            AlarmOccurrenceEntity occurrence = buildOccurrence(alarm, 701L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            occurrence.checkin(FIXED_NOW);
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));

            AlarmCheckinRequest request = buildRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 인증 가능 시간 전이면 예외가 발생한다")
        void fail_notYetAvailable() {
            // given
            MemberEntity member = MemberFixture.MEMBER_14.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_14;
            AlarmEntity alarm = buildAlarm(member, fixture);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            AlarmOccurrenceEntity occurrence = buildOccurrence(alarm, 801L, FIXED_NOW.plusHours(6), OccurrenceStatus.SCHEDULED);
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));

            AlarmCheckinRequest request = buildRequest(
                occurrence,
                alarm.getLatitude(),
                alarm.getLongitude()
            );

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 허용 반경 밖에서 도착 인증을 시도하면 예외가 발생한다")
        void fail_outOfRange() {
            // given
            MemberEntity member = MemberFixture.MEMBER_14.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_14;
            AlarmEntity alarm = buildAlarm(member, fixture);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            AlarmOccurrenceEntity occurrence = buildOccurrence(alarm, 901L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));

            AlarmCheckinRequest request = buildRequest(occurrence, alarm.getLatitude() + 1, alarm.getLongitude() + 1);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("deactivateByPayment - 결제로 알람 끄기")
    class DeactivateByPaymentTest {

        @Test
        @DisplayName("성공: 결제 검증이 완료되면 회차를 PAYMENT로 비활성화한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_15.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_15.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1001L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            AlarmOccurrenceEntity nextOccurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1002L, FIXED_NOW.plusDays(1), OccurrenceStatus.SCHEDULED);
            MemberDeviceEntity device = MemberDeviceFixture.ANDROID.toEntity(member);
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                device.getDeviceId(),
                "payment-success-001"
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(false);
            given(memberDeviceRepository.findByMember_IdAndDeviceId(member.getId(), device.getDeviceId())).willReturn(Optional.of(device));
            given(paymentVerificationPorts.stream()).willReturn(Stream.of(paymentVerificationPort));
            given(paymentVerificationPort.supportedPlatform()).willReturn(device.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(true);
            given(alarmOccurrenceRepository.findNextScheduledByAlarmIds(eq(List.of(alarm.getId())), eq(OccurrenceStatus.SCHEDULED), eq(FIXED_NOW)))
                .willReturn(List.of(nextOccurrence));

            // when
            AlarmPaymentResponse response = alarmCommandService.deactivateByPayment(member.getId(), alarm.getId(), request);

            // then
            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.PAYMENT);
            assertThat(occurrence.getDeactivatedAt()).isEqualTo(FIXED_NOW);
            assertThat(alarm.getRevision()).isEqualTo(2);
            assertThat(response.alarmId()).isEqualTo(alarm.getId());
            assertThat(response.deactivatedAt()).isEqualTo(FIXED_NOW);
            assertThat(response.nextOccurrence().occurrenceId()).isEqualTo(nextOccurrence.getId());
            verify(paymentRepository).saveAndFlush(any(PaymentEntity.class));
            verify(alarmDeactivationLogRepository).save(any(AlarmDeactivationLogEntity.class));
            verify(ringingAlarmRedisRepository).remove(alarm.getId(), member.getId());
            verify(paymentVerificationPort).consume(request.paymentId());
        }

        @Test
        @DisplayName("실패: 이미 처리된 결제 ID이면 예외가 발생한다")
        void fail_duplicatePayment() {
            // given
            MemberEntity member = MemberFixture.MEMBER_16.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_16.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1101L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                PaymentFixture.STOP_ALARM_SUCCESS.getPaymentId()
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.deactivateByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("이미 처리된 결제입니다.");
        }

        @Test
        @DisplayName("실패: 플랫폼 결제 검증이 실패하면 FAILED 결제와 실패 로그를 저장한다")
        void fail_paymentVerificationFailed() {
            // given
            MemberEntity member = MemberFixture.MEMBER_17.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_17.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1201L, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            MemberDeviceEntity device = MemberDeviceFixture.ANDROID.toEntity(member);
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                device.getDeviceId(),
                PaymentFixture.STOP_ALARM_FAILED.getPaymentId()
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(false);
            given(memberDeviceRepository.findByMember_IdAndDeviceId(member.getId(), device.getDeviceId())).willReturn(Optional.of(device));
            given(paymentVerificationPorts.stream()).willReturn(Stream.of(paymentVerificationPort));
            given(paymentVerificationPort.supportedPlatform()).willReturn(device.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.deactivateByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("결제 검증에 실패했습니다.");

            ArgumentCaptor<String> failReasonCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditLogRecorder).recordPaymentDeactivationFailure(
                eq(member),
                eq(alarm),
                eq(occurrence),
                eq(request.paymentId()),
                eq(device.getDeviceId()),
                eq(FIXED_NOW),
                failReasonCaptor.capture()
            );
            assertThat(failReasonCaptor.getValue())
                .contains("PAYMENT_VERIFICATION_FAILED")
                .contains("platform=ANDROID")
                .contains("paymentId=" + request.paymentId())
                .contains("reason=verification returned false");
        }

        @Test
        @DisplayName("실패: 결제 가능 시간 전이면 예외가 발생한다")
        void fail_notYetAvailable() {
            // given
            MemberEntity member = MemberFixture.MEMBER_18.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_18.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1301L, FIXED_NOW.plusHours(6), OccurrenceStatus.SCHEDULED);
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                "payment-not-yet-available"
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.deactivateByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("아직 결제로 알람을 끌 수 있는 시간이 아닙니다.");
        }
    }

    @Nested
    @DisplayName("removeAlarmByPayment - 결제로 알람 삭제")
    class RemoveAlarmByPaymentTest {

        @Test
        @DisplayName("성공: 결제 검증이 완료되면 알람을 소프트 삭제한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_07.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1401L, FIXED_NOW, OccurrenceStatus.RINGING);
            MemberDeviceEntity device = MemberDeviceFixture.ANDROID.toEntity(member);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest(
                device.getDeviceId(),
                PaymentFixture.DELETE_ALARM_SUCCESS.getPaymentId()
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(false);
            given(memberDeviceRepository.findByMember_IdAndDeviceId(member.getId(), device.getDeviceId())).willReturn(Optional.of(device));
            given(paymentVerificationPorts.stream()).willReturn(Stream.of(paymentVerificationPort));
            given(paymentVerificationPort.supportedPlatform()).willReturn(device.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(true);

            // when
            alarmCommandService.removeAlarmByPayment(member.getId(), alarm.getId(), request);

            // then
            assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.DELETED);
            assertThat(alarm.getDeletedAt()).isEqualTo(FIXED_NOW);
            assertThat(alarm.getRevision()).isEqualTo(2);
            verify(paymentRepository).saveAndFlush(any(PaymentEntity.class));
            verify(alarmDeleteLogRepository).save(any(AlarmDeleteLogEntity.class));
            verify(ringingAlarmRedisRepository).remove(alarm.getId(), member.getId());
            verify(paymentVerificationPort).consume(request.paymentId());
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 예외를 던진다")
        void fail_alarmNotFound() {
            // given
            given(alarmRepository.findById(1L)).willReturn(Optional.empty());
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(1L, 1L, request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 예외를 던진다")
        void fail_invalidOwner() {
            // given
            MemberEntity owner = MemberFixture.MEMBER_9.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_09.toMockEntity(owner);
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(MemberFixture.MEMBER_10.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 오늘 회차가 없으면 예외를 던진다")
        void fail_todayIsNotAlarmDay() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_07.toMockEntity(member);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("오늘은 알람이 울리는 날이 아닙니다.");
        }

        @Test
        @DisplayName("실패: 이미 비활성화된 회차이면 예외를 던진다")
        void fail_alreadyDeactivated() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_07.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1402L, FIXED_NOW, OccurrenceStatus.PAYMENT);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("이미 오늘은 비활성화된 알람입니다.");
        }

        @Test
        @DisplayName("실패: 이미 처리된 결제 ID이면 예외를 던진다")
        void fail_duplicatePayment() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_07.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1403L, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest(
                "device-uuid",
                PaymentFixture.DELETE_ALARM_SUCCESS.getPaymentId()
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("이미 처리된 결제입니다.");
        }

        @Test
        @DisplayName("실패: 결제 저장 시점에 중복 결제가 감지되면 예외를 던진다")
        void fail_duplicatePaymentOnSave() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_07.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1405L, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            MemberDeviceEntity device = MemberDeviceFixture.ANDROID.toEntity(member);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest(
                device.getDeviceId(),
                PaymentFixture.DELETE_ALARM_SUCCESS.getPaymentId()
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(false);
            given(memberDeviceRepository.findByMember_IdAndDeviceId(member.getId(), device.getDeviceId())).willReturn(Optional.of(device));
            given(paymentVerificationPorts.stream()).willReturn(Stream.of(paymentVerificationPort));
            given(paymentVerificationPort.supportedPlatform()).willReturn(device.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(true);
            given(paymentRepository.saveAndFlush(any(PaymentEntity.class)))
                .willThrow(new DataIntegrityViolationException("duplicate payment"));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("이미 처리된 결제입니다.");
        }

        @Test
        @DisplayName("실패: 플랫폼 결제 검증이 실패하면 FAILED 결제와 삭제 실패 로그를 저장한다")
        void fail_paymentVerificationFailed() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_07.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1404L, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            MemberDeviceEntity device = MemberDeviceFixture.ANDROID.toEntity(member);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest(
                device.getDeviceId(),
                PaymentFixture.DELETE_ALARM_FAILED.getPaymentId()
            );

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));
            given(paymentRepository.existsByPaymentId(request.paymentId())).willReturn(false);
            given(memberDeviceRepository.findByMember_IdAndDeviceId(member.getId(), device.getDeviceId())).willReturn(Optional.of(device));
            given(paymentVerificationPorts.stream()).willReturn(Stream.of(paymentVerificationPort));
            given(paymentVerificationPort.supportedPlatform()).willReturn(device.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByPayment(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("결제 검증에 실패했습니다.");

            ArgumentCaptor<String> failReasonCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditLogRecorder).recordPaymentDeleteFailure(
                eq(member),
                eq(alarm),
                eq(request.paymentId()),
                eq(FIXED_NOW),
                failReasonCaptor.capture()
            );
            assertThat(failReasonCaptor.getValue())
                .contains("PAYMENT_VERIFICATION_FAILED")
                .contains("platform=ANDROID")
                .contains("paymentId=" + request.paymentId())
                .contains("reason=verification returned false");
        }
    }

    @Nested
    @DisplayName("removeAlarmByAd - 광고 시청으로 알람 삭제")
    class RemoveAlarmByAdTest {

        @Test
        @DisplayName("성공: 오늘 회차가 비활성화되어 있으면 광고 시청으로 알람을 삭제한다")
        void success_todayOccurrenceDeactivated() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1501L, FIXED_NOW, OccurrenceStatus.CHECKIN);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token-001");

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));

            // when
            alarmCommandService.removeAlarmByAd(member.getId(), alarm.getId(), request);

            // then
            assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.DELETED);
            assertThat(alarm.getDeletedAt()).isEqualTo(FIXED_NOW);
            assertThat(alarm.getRevision()).isEqualTo(2);

            ArgumentCaptor<AlarmDeleteLogEntity> logCaptor = ArgumentCaptor.forClass(AlarmDeleteLogEntity.class);
            verify(alarmDeleteLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getDeleteType()).isEqualTo(DeleteType.AD);
            assertThat(logCaptor.getValue().getAdProofToken()).isEqualTo(request.adProofToken());
            verify(ringingAlarmRedisRepository).remove(alarm.getId(), member.getId());
        }

        @Test
        @DisplayName("성공: 오늘 회차가 없으면 광고 시청으로 알람을 삭제한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token-002");

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.empty());

            // when
            alarmCommandService.removeAlarmByAd(member.getId(), alarm.getId(), request);

            // then
            assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.DELETED);
            assertThat(alarm.getDeletedAt()).isEqualTo(FIXED_NOW);
            assertThat(alarm.getRevision()).isEqualTo(2);
            verify(alarmDeleteLogRepository).save(any(AlarmDeleteLogEntity.class));
            verify(ringingAlarmRedisRepository).remove(alarm.getId(), member.getId());
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 예외를 던진다")
        void fail_alarmNotFound() {
            // given
            given(alarmRepository.findById(1L)).willReturn(Optional.empty());
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token");

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByAd(1L, 1L, request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 예외를 던진다")
        void fail_permissionDenied() {
            // given
            MemberEntity owner = MemberFixture.MEMBER_9.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_09.toMockEntity(owner);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token");
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByAd(MemberFixture.MEMBER_10.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 오늘 회차가 SCHEDULED이면 결제 삭제를 사용해야 한다")
        void fail_occurrenceScheduled() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1502L, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token");

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByAd(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("결제 삭제가 필요한 알람입니다.");
        }

        @Test
        @DisplayName("실패: 오늘 회차가 RINGING이면 결제 삭제를 사용해야 한다")
        void fail_occurrenceRinging() {
            // given
            MemberEntity member = MemberFixture.MEMBER_8.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_08.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02
                .toMockEntity(alarm, 1503L, FIXED_NOW, OccurrenceStatus.RINGING);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token");

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), FIXED_NOW.toLocalDate()))
                .willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarmByAd(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("결제 삭제가 필요한 알람입니다.");
        }
    }

    @Nested
    @DisplayName("ringAlarm - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("성공: 알람이 울리면 alarmRinging=true, 로그 저장, Redis 적재가 모두 수행된다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_05.toMockEntity(member);
            LocalDateTime scheduledAt = FIXED_NOW.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_PAST_TIME
                .toMockEntity(alarm, 1L, scheduledAt, OccurrenceStatus.SCHEDULED);

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository
                    .findTopByAlarmIdAndStatusInOrderByOccurrenceDateDescOccurrenceTimeDesc(eq(alarm.getId()), anyList()))
                    .willReturn(Optional.of(occurrence));

            // when
            alarmCommandService.ringAlarm(member.getId(), alarm.getId());

            // then
            assertThat(occurrence.isAlarmRinging()).isTrue();          // DB 컬럼 변경
            assertThat(occurrence.getRingingCount()).isEqualTo(1);
            verify(alarmRingingLogRepository).save(any());             // 로그 저장
            verify(ringingAlarmRedisRepository).add(                   // Redis ZADD
                    eq(alarm.getId()), eq(member.getId()), anyLong());
        }

        @Test
        @DisplayName("실패: 알람 시간이 되지 않았으면 예외를 던진다")
        void fail_notAlarmTime() {

            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_05.toMockEntity(member);
            LocalDateTime scheduledAt = FIXED_NOW.plusDays(1).plusHours(1);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_FUTURE_TIME
                .toMockEntity(alarm, 1L, scheduledAt, OccurrenceStatus.SCHEDULED);

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(
                alarmOccurrenceRepository
                    .findTopByAlarmIdAndStatusInOrderByOccurrenceDateDescOccurrenceTimeDesc(eq(alarm.getId()), anyList())
            ).willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.ringAlarm(member.getId(), alarm.getId()))
                .isInstanceOf(ApplicationException.class);
        }
    }
}
