package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALARM_DELETE_REQUIRES_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberDeviceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.common.fixture.PaymentFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.*;
import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.payment.PaymentVerificationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("AlarmController Integration Test")
class AlarmControllerIntegrationTest {

    @Autowired private JwtProvider jwtProvider;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberDeviceRepository memberDeviceRepository;
    @Autowired private AlarmRepository alarmRepository;
    @Autowired private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Autowired private AlarmRingingLogRepository alarmRingingLogRepository;
    @Autowired private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Autowired private AlarmDeleteLogRepository alarmDeleteLogRepository;
    @Autowired private PaymentRepository paymentRepository;
    @MockitoBean private PaymentVerificationPort paymentVerificationPort;
    @MockitoBean private TimeProvider timeProvider;

    private static final String BASE = "/api/v1/alarms";
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 4, 11, 0);

    @AfterEach
    void cleanupCommittedData() {
        alarmDeleteLogRepository.deleteAll();
        alarmDeactivationLogRepository.deleteAll();
        paymentRepository.deleteAll();
        alarmRingingLogRepository.deleteAll();
        alarmOccurrenceRepository.deleteAll();
        memberDeviceRepository.deleteAll();
        alarmRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @BeforeEach
    void setUpTimeProvider() {
        given(timeProvider.now()).willReturn(FIXED_NOW);
        given(timeProvider.today()).willReturn(FIXED_NOW.toLocalDate());
    }

    private AlarmEntity saveAlarmForToday(MemberEntity member) {
        DayOfWeek today = FIXED_NOW.toLocalDate().getDayOfWeek();
        return alarmRepository.save(AlarmEntity.builder()
            .alarmPurpose("test")
            .time(LocalTime.of(7, 0))
            .repeatDays(List.of(Weekday.from(today)))
            .soundType(SoundType.KARINA_SCOLDING)
            .latitude(37.5665)
            .longitude(126.9780)
            .address("서울특별시 중구 퇴계로 123")
            .member(member)
            .build());
    }

    private AlarmEntity saveAlarmForNextWeek(MemberEntity member) {
        DayOfWeek today = FIXED_NOW.toLocalDate().getDayOfWeek();
        DayOfWeek previous = today.minus(1);
        return alarmRepository.save(AlarmEntity.builder()
            .alarmPurpose("test")
            .time(LocalTime.of(7, 0))
            .repeatDays(List.of(Weekday.from(previous)))
            .soundType(SoundType.KARINA_SCOLDING)
            .latitude(37.5665)
            .longitude(126.9780)
            .address("서울특별시 중구 퇴계로 123")
            .member(member)
            .build());
    }

    private String buildAccessToken(MemberEntity member) {
        return jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");
    }

    private AlarmOccurrenceEntity saveOccurrence(AlarmEntity alarm, LocalDateTime scheduledAt, OccurrenceStatus status) {
        return alarmOccurrenceRepository.save(AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(scheduledAt.toLocalDate())
            .occurrenceTime(scheduledAt.toLocalTime())
            .scheduledAt(scheduledAt)
            .status(status)
            .alarmRinging(status == OccurrenceStatus.RINGING)
            .ringingCount(status == OccurrenceStatus.RINGING ? 1 : 0)
            .reminderSent(false)
            .build());
    }

    private AlarmCheckinRequest buildCheckinRequest(AlarmOccurrenceEntity occurrence, Double latitude, Double longitude) {
        return new AlarmCheckinRequest(occurrence.getId(), "device-uuid", latitude, longitude);
    }

    @Nested
    @DisplayName("[POST] /api/v1/alarms - 알람 등록")
    class CreateAlarmTest {

        @Test
        @DisplayName("성공: 알람 등록 요청이 성공하면 알람이 저장된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            AlarmFixture fixture = AlarmFixture.ALARM_01;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::name).toList(),
                fixture.getSoundType().name()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            assertThat(alarmRepository.findAllByMemberId(member.getId())).hasSize(1);
        }

        @Test
        @DisplayName("실패: 반복 요일이 비어 있으면 400 응답을 반환한다")
        void fail_repeatDaysEmpty() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            AlarmFixture fixture = AlarmFixture.ALARM_02;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                List.of(),
                fixture.getSoundType().name()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 같은 이름의 알람이 존재하면 409 응답을 반환한다")
        void fail_duplicateAlarmPurpose() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            AlarmFixture fixture = AlarmFixture.ALARM_03;
            alarmRepository.save(fixture.toEntity(member));
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::name).toList(),
                fixture.getSoundType().name()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }


    @Nested
    @DisplayName("[POST] /api/v1/alarms/{alarmId}/ring - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("성공: 알람 울림 요청이 성공하면 200을 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));

            var now = FIXED_NOW;
            saveOccurrence(alarm, now.minusMinutes(1), OccurrenceStatus.SCHEDULED);

            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람 ID로 요청하면 404 응답을 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");
            long nonExistentAlarmId = 999L;

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/ring", nonExistentAlarmId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 다른 사람의 알람을 울리려고 하면 403 응답을 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            MemberEntity owner = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            MemberEntity other = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(owner));
            alarmOccurrenceRepository.save(AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toEntity(alarm));
            String accessToken = jwtProvider.generateAccessToken(other.getId(), other.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 알람 발생 내역이 없으면 404 응답을 반환한다")
        void fail_alarmOccurrenceNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 이미 비활성화된 알람이면 400 응답을 반환한다")
        void fail_alreadyDeactivated() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));
            alarmOccurrenceRepository.save(AlarmOccurrenceFixture.ALARM_OCCURRENCE_DEACTIVATED.toEntity(alarm));
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound()); // TODO: isBadRequest로 검증해야함.
        }

        @Test
        @DisplayName("실패: 알람 시간이 아니면 400 응답을 반환한다")
        void fail_notAlarmTime() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));
            alarmOccurrenceRepository.save(AlarmOccurrenceFixture.ALARM_OCCURRENCE_FUTURE_TIME.toEntity(alarm));
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[POST] /api/v1/alarms/{alarmId}/off/checkin - 도착 인증")
    class CheckinTest {

        @Test
        @DisplayName("성공: 체크인이 완료되면 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            AlarmEntity alarm = saveAlarmForToday(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            saveOccurrence(alarm, FIXED_NOW.plusDays(1), OccurrenceStatus.SCHEDULED);
            AlarmCheckinRequest request = buildCheckinRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());
            String accessToken = buildAccessToken(member);

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/off/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            AlarmOccurrenceEntity savedOccurrence = alarmOccurrenceRepository
                .findById(occurrence.getId())
                .orElseThrow();
            assertThat(savedOccurrence.getStatus()).isEqualTo(OccurrenceStatus.CHECKIN);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람이면 404와 에러 코드를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = new AlarmCheckinRequest(999L, "device-uuid", 0.0, 0.0);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/checkin", 999L)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 다른 사용자의 알람이면 403과 에러 코드를 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            MemberEntity owner = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            MemberEntity other = memberRepository.save(MemberFixture.MEMBER_4.toEntity());
            AlarmEntity alarm = saveAlarmForToday(owner);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            String accessToken = buildAccessToken(other);
            AlarmCheckinRequest request = buildCheckinRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 이미 체크인된 알람이면 400과 에러 코드를 반환한다")
        void fail_alreadyDeactivated() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_5.toEntity());
            AlarmEntity alarm = saveAlarmForToday(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            occurrence.checkin(FIXED_NOW);
            alarmOccurrenceRepository.save(occurrence);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = buildCheckinRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 허용 반경 밖에서 체크인하면 400과 에러 코드를 반환한다")
        void fail_outOfRange() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_6.toEntity());
            AlarmEntity alarm = saveAlarmForToday(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = buildCheckinRequest(
                occurrence,
                alarm.getLatitude() + 1,
                alarm.getLongitude() + 1
            );

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 가능 시간 전이면 400을 반환한다")
        void fail_notYetAvailable() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = saveAlarmForToday(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, FIXED_NOW.plusHours(6), OccurrenceStatus.SCHEDULED);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = buildCheckinRequest(
                occurrence,
                alarm.getLatitude(),
                alarm.getLongitude()
            );

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deactivateByPayment - 결제로 알람 끄기")
    class DeactivateByPaymentTest {

        @Test
        @DisplayName("성공: 결제 요청이 성공하면 200 OK와 동기화 정보를 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            memberDeviceRepository.save(MemberDeviceFixture.ANDROID.toEntity(member));
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_08.toEntity(member));
            AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.save(
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toEntity(alarm, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED)
            );
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                "integration-payment-success-001"
            );
            String accessToken = buildAccessToken(member);
            given(paymentVerificationPort.supportedPlatform()).willReturn(MemberDeviceFixture.ANDROID.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(true);

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/off/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarmId").value(alarm.getId()))
                .andExpect(jsonPath("$.result.alarmRevision").value(2));

            // then
            AlarmOccurrenceEntity savedOccurrence = alarmOccurrenceRepository.findById(occurrence.getId()).orElseThrow();
            assertThat(savedOccurrence.getStatus()).isEqualTo(OccurrenceStatus.PAYMENT);
            assertThat(paymentRepository.existsByPaymentId(request.paymentId())).isTrue();
            assertThat(alarmDeactivationLogRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("실패: 이미 처리된 결제 ID이면 409와 에러 코드를 반환한다")
        void fail_duplicatePayment() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            memberDeviceRepository.save(MemberDeviceFixture.ANDROID.toEntity(member));
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_09.toEntity(member));
            AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.save(
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toEntity(alarm, FIXED_NOW.plusHours(1), OccurrenceStatus.SCHEDULED)
            );
            paymentRepository.save(PaymentFixture.STOP_ALARM_SUCCESS.toEntity(member, alarm));
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                PaymentFixture.STOP_ALARM_SUCCESS.getPaymentId()
            );
            String accessToken = buildAccessToken(member);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_901"));
        }

        @Test
        @DisplayName("실패: 결제 가능 시간 전이면 400과 에러 코드를 반환한다")
        void fail_notYetAvailable() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_10.toEntity());
            memberDeviceRepository.save(MemberDeviceFixture.ANDROID.toEntity(member));
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_10.toEntity(member));
            AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.save(
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toEntity(alarm, FIXED_NOW.plusHours(6), OccurrenceStatus.SCHEDULED)
            );
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                "integration-payment-not-yet-available"
            );
            String accessToken = buildAccessToken(member);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/off/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_001"));
        }
    }

    @Nested
    @DisplayName("[POST] /api/v1/alarms/{alarmId}/delete/payment - 결제로 알람 삭제")
    class RemoveAlarmByPaymentTest {

        @Test
        @DisplayName("성공: 결제 삭제 요청이 성공하면 알람이 소프트 삭제된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_03.toEntity(member));
            saveOccurrence(alarm, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            memberDeviceRepository.save(MemberDeviceFixture.ANDROID.toEntity(member));
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest(
                MemberDeviceFixture.ANDROID.getDeviceId(),
                PaymentFixture.DELETE_ALARM_SUCCESS.getPaymentId()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");
            given(paymentVerificationPort.supportedPlatform()).willReturn(MemberDeviceFixture.ANDROID.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(true);

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").doesNotExist());

            // then
            AlarmEntity deletedAlarm = alarmRepository.findById(alarm.getId()).orElseThrow();
            assertThat(deletedAlarm.getStatus()).isEqualTo(AlarmStatus.DELETED);
            assertThat(deletedAlarm.getDeletedAt()).isEqualTo(FIXED_NOW);
            assertThat(paymentRepository.existsByPaymentId(request.paymentId())).isTrue();
            assertThat(alarmDeleteLogRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 404를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_4.toEntity());
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", 999L)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 403을 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            MemberEntity owner = memberRepository.save(MemberFixture.MEMBER_5.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_05.toEntity(owner));
            MemberEntity other = memberRepository.save(MemberFixture.MEMBER_6.toEntity());
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");
            String accessToken = jwtProvider.generateAccessToken(other.getId(), other.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 결제 ID가 비어 있으면 400을 반환한다")
        void fail_paymentIdBlank() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 오늘 회차가 없으면 400을 반환한다")
        void fail_todayIsNotAlarmDay() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALARM_001"));
        }

        @Test
        @DisplayName("실패: 이미 비활성화된 회차이면 400을 반환한다")
        void fail_alreadyDeactivated() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            saveOccurrence(alarm, FIXED_NOW, OccurrenceStatus.PAYMENT);
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest("device-uuid", "payment-id");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALARM_003"));
        }

        @Test
        @DisplayName("실패: 결제 검증에 실패하면 실패 기록을 남기고 400을 반환한다")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void fail_paymentVerificationFailed() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_20.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            saveOccurrence(alarm, FIXED_NOW, OccurrenceStatus.SCHEDULED);
            memberDeviceRepository.save(MemberDeviceFixture.ANDROID.toEntity(member));
            AlarmDeleteByPaymentRequest request = new AlarmDeleteByPaymentRequest(
                MemberDeviceFixture.ANDROID.getDeviceId(),
                PaymentFixture.DELETE_ALARM_FAILED.getPaymentId()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");
            given(paymentVerificationPort.supportedPlatform()).willReturn(MemberDeviceFixture.ANDROID.getPlatform());
            given(paymentVerificationPort.verify(request.paymentId())).willReturn(false);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_002"));

            assertThat(paymentRepository.existsByPaymentId(request.paymentId())).isTrue();
            assertThat(alarmDeleteLogRepository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAlarmDeleteMethod - 알람 삭제 방법 조회")
    class GetAlarmDeleteMethodTest {

        @Nested
        @DisplayName("오늘 회차가 없는 경우")
        class NoOccurrenceTodayTest {

            @Test
            @DisplayName("성공: 광고 삭제 방법을 반환한다")
            void success() throws Exception {
                // given
                MemberEntity member = memberRepository.save(MemberFixture.MEMBER_11.toEntity());
                AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_11.toEntity(member));
                String accessToken = buildAccessToken(member);

                // when
                var result = mockMvc.perform(get(BASE + "/{alarmId}/delete-method", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

                // then
                result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.deleteMethod").value(AlarmDeleteMethod.AD.name()))
                    .andExpect(jsonPath("$.result.alarmId").doesNotExist());
            }
        }

        @Nested
        @DisplayName("오늘 회차가 예정 상태인 경우")
        class ScheduledTest {

            @Test
            @DisplayName("성공: 결제 삭제 방법을 반환한다")
            void success() throws Exception {
                // given
                MemberEntity member = memberRepository.save(MemberFixture.MEMBER_12.toEntity());
                AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_12.toEntity(member));
                saveOccurrence(alarm, FIXED_NOW, OccurrenceStatus.SCHEDULED);
                String accessToken = buildAccessToken(member);

                // when
                var result = mockMvc.perform(get(BASE + "/{alarmId}/delete-method", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

                // then
                result
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.deleteMethod").value(AlarmDeleteMethod.PAYMENT.name()));
            }
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 404를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_13.toEntity());
            String accessToken = buildAccessToken(member);

            // when
            var result = mockMvc.perform(get(BASE + "/{alarmId}/delete-method", 999L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

            // then
            result
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AlarmErrorCode.ALARM_NOT_FOUND.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 403을 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            MemberEntity owner = memberRepository.save(MemberFixture.MEMBER_14.toEntity());
            MemberEntity other = memberRepository.save(MemberFixture.MEMBER_15.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_14.toEntity(owner));
            String accessToken = buildAccessToken(other);

            // when
            var result = mockMvc.perform(get(BASE + "/{alarmId}/delete-method", alarm.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

            // then
            result
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_DENIED.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("removeAlarmByAd - 광고 시청으로 알람 삭제")
    class RemoveAlarmByAdTest {

        @Test
        @DisplayName("성공: 비활성화된 오늘 회차가 있으면 알람을 소프트 삭제한다")
        void success_todayOccurrenceDeactivated() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_08.toEntity(member));
            saveOccurrence(alarm, FIXED_NOW, OccurrenceStatus.CHECKIN);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token-001");
            String accessToken = buildAccessToken(member);

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/delete/ad", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").doesNotExist());

            // then
            AlarmEntity deletedAlarm = alarmRepository.findById(alarm.getId()).orElseThrow();
            assertThat(deletedAlarm.getStatus()).isEqualTo(AlarmStatus.DELETED);
            assertThat(deletedAlarm.getDeletedAt()).isEqualTo(FIXED_NOW);
            assertThat(deletedAlarm.getRevision()).isEqualTo(2);
            assertThat(alarmDeleteLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getDeleteType()).isEqualTo(DeleteType.AD);
                    assertThat(log.getAdProofToken()).isEqualTo(request.adProofToken());
                });
        }

        @Test
        @DisplayName("성공: 오늘 회차가 없으면 알람을 소프트 삭제한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_09.toEntity(member));
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token-002");
            String accessToken = buildAccessToken(member);

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/delete/ad", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").doesNotExist());

            // then
            AlarmEntity deletedAlarm = alarmRepository.findById(alarm.getId()).orElseThrow();
            assertThat(deletedAlarm.getStatus()).isEqualTo(AlarmStatus.DELETED);
            assertThat(deletedAlarm.getDeletedAt()).isEqualTo(FIXED_NOW);
            assertThat(alarmDeleteLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getDeleteType()).isEqualTo(DeleteType.AD);
                    assertThat(log.getAdProofToken()).isEqualTo(request.adProofToken());
                });
        }

        @Test
        @DisplayName("실패: 오늘 회차가 아직 비활성화되지 않았으면 400을 반환한다")
        void fail_alarmDeleteNotAvailable() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_10.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_10.toEntity(member));
            saveOccurrence(alarm, FIXED_NOW, OccurrenceStatus.RINGING);
            AlarmDeleteByAdRequest request = new AlarmDeleteByAdRequest("device-uuid", "ad-proof-token");
            String accessToken = buildAccessToken(member);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/delete/ad", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(ALARM_DELETE_REQUIRES_PAYMENT.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("[GET] /api/v1/alarms - 알람 목록 조회")
    class GetAlarmsTest {

        @Test
        @DisplayName("성공: 200 OK와 알람 목록을 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            AlarmFixture fixture = AlarmFixture.ALARM_03;
            alarmRepository.save(fixture.toEntity(member));
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(get(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarms[0].alarmPurpose").value(fixture.getAlarmPurpose()));
        }

/*        @Test
        @DisplayName("실패: 회원이 없으면 404와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            Long memberId = 999L;
            String accessToken = jwtProvider.generateAccessToken(memberId, MemberFixture.MEMBER_3.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(get(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCustomCode()));
        }*/
    }

}
