package akuma.whiplash.domains.alarm.presentation;

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
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRemoveRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.infrastructure.payment.PaymentVerificationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    @Autowired private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Autowired private PaymentRepository paymentRepository;
    @MockitoBean private PaymentVerificationPort paymentVerificationPort;

    private static final String BASE = "/api/v1/alarms";

    private AlarmEntity saveAlarmForToday(MemberEntity member) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return alarmRepository.save(AlarmEntity.builder()
            .alarmPurpose("test")
            .time(LocalTime.of(7, 0))
            .repeatDays(List.of(Weekday.from(today)))
            .soundType(SoundType.ONE)
            .latitude(37.5665)
            .longitude(126.9780)
            .address("서울특별시 중구 퇴계로 123")
            .member(member)
            .build());
    }

    private AlarmEntity saveAlarmForNextWeek(MemberEntity member) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        DayOfWeek previous = today.minus(1);
        return alarmRepository.save(AlarmEntity.builder()
            .alarmPurpose("test")
            .time(LocalTime.of(7, 0))
            .repeatDays(List.of(Weekday.from(previous)))
            .soundType(SoundType.ONE)
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
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
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
                fixture.getSoundType().getDescription()
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
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
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
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));

            var now = LocalDateTime.now();
            var occurrence = AlarmOccurrenceEntity.builder()
                .alarm(alarm)
                .occurrenceDate(now.toLocalDate())
                .occurrenceTime(now.toLocalTime().minusMinutes(1)) // ← now보다 과거
                .scheduledAt(now.minusMinutes(1))
                .status(OccurrenceStatus.SCHEDULED)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();
            alarmOccurrenceRepository.save(occurrence);

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
    @DisplayName("[POST] /api/v1/alarms/{alarmId}/checkin - 도착 인증")
    class CheckinTest {

        @Test
        @DisplayName("성공: 체크인이 완료되면 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            AlarmEntity alarm = saveAlarmForToday(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, LocalDateTime.now().plusHours(1), OccurrenceStatus.SCHEDULED);
            saveOccurrence(alarm, LocalDateTime.now().plusDays(1), OccurrenceStatus.SCHEDULED);
            AlarmCheckinRequest request = buildCheckinRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());
            String accessToken = buildAccessToken(member);

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", alarm.getId())
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
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 999L)
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
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, LocalDateTime.now().plusHours(1), OccurrenceStatus.SCHEDULED);
            String accessToken = buildAccessToken(other);
            AlarmCheckinRequest request = buildCheckinRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", alarm.getId())
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
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, LocalDateTime.now().plusHours(1), OccurrenceStatus.SCHEDULED);
            occurrence.checkin(LocalDateTime.now());
            alarmOccurrenceRepository.save(occurrence);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = buildCheckinRequest(occurrence, alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", alarm.getId())
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
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, LocalDateTime.now().plusHours(1), OccurrenceStatus.SCHEDULED);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = buildCheckinRequest(
                occurrence,
                alarm.getLatitude() + 1,
                alarm.getLongitude() + 1
            );

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", alarm.getId())
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
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, LocalDateTime.now().plusHours(6), OccurrenceStatus.SCHEDULED);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = buildCheckinRequest(
                occurrence,
                alarm.getLatitude(),
                alarm.getLongitude()
            );

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deactivateByPayment - 결제로 알람 끄기")
    class DeactivateByPaymentTest {

        private static final LocalDateTime PAYMENT_NOW = LocalDateTime.now();

        @Test
        @DisplayName("성공: 결제 요청이 성공하면 200 OK와 동기화 정보를 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            memberDeviceRepository.save(MemberDeviceFixture.ANDROID.toEntity(member));
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_08.toEntity(member));
            AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.save(
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toEntity(alarm, PAYMENT_NOW.plusHours(1), OccurrenceStatus.SCHEDULED)
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
            mockMvc.perform(post(BASE + "/{alarmId}/payment", alarm.getId())
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
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toEntity(alarm, PAYMENT_NOW.plusHours(1), OccurrenceStatus.SCHEDULED)
            );
            paymentRepository.save(PaymentFixture.STOP_ALARM_SUCCESS.toEntity(member, alarm));
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                PaymentFixture.STOP_ALARM_SUCCESS.getPaymentId()
            );
            String accessToken = buildAccessToken(member);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/payment", alarm.getId())
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
                AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toEntity(alarm, PAYMENT_NOW.plusHours(6), OccurrenceStatus.SCHEDULED)
            );
            AlarmPaymentRequest request = new AlarmPaymentRequest(
                occurrence.getId(),
                MemberDeviceFixture.ANDROID.getDeviceId(),
                "integration-payment-not-yet-available"
            );
            String accessToken = buildAccessToken(member);

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/payment", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_001"));
        }
    }

    @Nested
    @DisplayName("[DELETE] /api/v1/alarms/{alarmId} - 알람 삭제")
    class RemoveAlarmTest {

        @Test
        @DisplayName("성공: 알람 삭제 요청이 성공하면 알람이 제거된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_03.toEntity(member));
            AlarmRemoveRequest request = new AlarmRemoveRequest("필요 없어졌어요");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when
            mockMvc.perform(delete(BASE + "/{alarmId}", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            assertThat(alarmRepository.findById(alarm.getId())).isEmpty();
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 404를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_4.toEntity());
            AlarmRemoveRequest request = new AlarmRemoveRequest("사유");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", 999L)
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
            AlarmRemoveRequest request = new AlarmRemoveRequest("사유");
            String accessToken = jwtProvider.generateAccessToken(other.getId(), other.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 삭제 사유가 비어 있으면 400을 반환한다")
        void fail_reasonBlank() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            AlarmRemoveRequest request = new AlarmRemoveRequest("");
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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
