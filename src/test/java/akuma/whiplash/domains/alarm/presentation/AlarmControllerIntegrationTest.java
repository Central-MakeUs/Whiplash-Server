package akuma.whiplash.domains.alarm.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.MemberDeviceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.constant.AdSessionStatus;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdActionRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.util.date.TimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("AlarmController Integration Test")
class AlarmControllerIntegrationTest {

    private static final String BASE = "/api/v1/alarms";
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 4, 11, 0);

    @Autowired private JwtProvider jwtProvider;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberDeviceRepository memberDeviceRepository;
    @Autowired private AlarmRepository alarmRepository;
    @Autowired private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Autowired private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Autowired private AlarmDeleteLogRepository alarmDeleteLogRepository;
    @Autowired private AdSessionRepository adSessionRepository;
    @MockitoBean private TimeProvider timeProvider;

    @BeforeEach
    void setUpTimeProvider() {
        given(timeProvider.now()).willReturn(FIXED_NOW);
        given(timeProvider.now(any(ZoneId.class))).willReturn(FIXED_NOW);
        given(timeProvider.instant()).willReturn(FIXED_NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }

    @AfterEach
    void cleanup() {
        alarmDeleteLogRepository.deleteAll();
        alarmDeactivationLogRepository.deleteAll();
        adSessionRepository.deleteAll();
        alarmOccurrenceRepository.deleteAll();
        memberDeviceRepository.deleteAll();
        alarmRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("광고 세션 발급")
    class CreateAdSessionTest {

        @Test
        @DisplayName("알람 끄기 세션은 인증 기기와 알람 회차에 결합된다")
        void createsOffSession() throws Exception {
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmEntity alarm = saveAlarm(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, OccurrenceStatus.SCHEDULED);

            mockMvc.perform(post(BASE + "/{alarmId}/off/ad-session", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AlarmOffAdSessionCreateRequest(occurrence.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.adSessionId").isNotEmpty());

            AdSessionEntity session = adSessionRepository.findAll().get(0);
            assertThat(session.getPurpose()).isEqualTo(AdPurpose.STOP_ALARM);
            assertThat(session.getDeviceId()).isEqualTo(device.getDeviceId());
            assertThat(session.getAlarmOccurrence().getId()).isEqualTo(occurrence.getId());
        }

        @Test
        @DisplayName("알람 삭제 세션은 진행 중인 회차가 있어도 발급된다")
        void createsDeleteSession() throws Exception {
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmEntity alarm = saveAlarm(member);
            saveOccurrence(alarm, OccurrenceStatus.RINGING);

            mockMvc.perform(post(BASE + "/{alarmId}/delete/ad-session", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId())))
                .andExpect(status().isOk());

            AdSessionEntity session = adSessionRepository.findAll().get(0);
            assertThat(session.getPurpose()).isEqualTo(AdPurpose.DELETE_ALARM);
            assertThat(session.getAlarmOccurrence()).isNull();
        }
    }

    @Nested
    @DisplayName("광고 보상 검증 후 액션")
    class ExecuteAdActionTest {

        @Test
        @DisplayName("검증된 광고로 알람 회차를 끄고 세션을 소비한다")
        void deactivatesOccurrence() throws Exception {
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmEntity alarm = saveAlarm(member);
            AlarmOccurrenceEntity occurrence = saveOccurrence(alarm, OccurrenceStatus.SCHEDULED);
            AdSessionEntity session = saveVerifiedSession(member, alarm, occurrence, device.getDeviceId(), AdPurpose.STOP_ALARM, "off-session");

            mockMvc.perform(post(BASE + "/{alarmId}/off/ad", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AlarmAdActionRequest(session.getAdSessionId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarmId").value(alarm.getId()));

            assertThat(alarmOccurrenceRepository.findById(occurrence.getId()).orElseThrow().getStatus()).isEqualTo(OccurrenceStatus.WATCH_AD);
            assertThat(adSessionRepository.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(AdSessionStatus.CONSUMED);
            assertThat(alarmDeactivationLogRepository.findAll()).singleElement().satisfies(log -> assertThat(log.getAdProofToken()).isEqualTo(session.getAdSessionId()));
        }

        @Test
        @DisplayName("검증되지 않은 세션으로는 알람을 삭제할 수 없다")
        void rejectsUnverifiedDeleteSession() throws Exception {
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmEntity alarm = saveAlarm(member);
            AdSessionEntity session = adSessionRepository.save(AdSessionEntity.builder()
                .adSessionId("issued-session").member(member).alarm(alarm).deviceId(device.getDeviceId())
                .purpose(AdPurpose.DELETE_ALARM).status(AdSessionStatus.ISSUED).expiresAt(FIXED_NOW.plusMinutes(10)).build());

            mockMvc.perform(post(BASE + "/{alarmId}/delete/ad", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AlarmAdActionRequest(session.getAdSessionId()))))
                .andExpect(status().isBadRequest());

            assertThat(alarmRepository.findById(alarm.getId()).orElseThrow().getStatus().name()).isNotEqualTo("DELETED");
        }

        @Test
        @DisplayName("검증된 광고로 울리는 알람도 삭제할 수 있다")
        void deletesAlarm() throws Exception {
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmEntity alarm = saveAlarm(member);
            saveOccurrence(alarm, OccurrenceStatus.RINGING);
            AdSessionEntity session = saveVerifiedSession(member, alarm, null, device.getDeviceId(), AdPurpose.DELETE_ALARM, "delete-session");

            mockMvc.perform(post(BASE + "/{alarmId}/delete/ad", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AlarmAdActionRequest(session.getAdSessionId()))))
                .andExpect(status().isOk());

            assertThat(alarmRepository.findById(alarm.getId()).orElseThrow().getStatus().name()).isEqualTo("DELETED");
            assertThat(adSessionRepository.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(AdSessionStatus.CONSUMED);
        }
    }

    private AlarmEntity saveAlarm(MemberEntity member) {
        return alarmRepository.save(AlarmEntity.builder().alarmPurpose("test").time(LocalTime.of(7, 0))
            .repeatDays(List.of(Weekday.from(FIXED_NOW.getDayOfWeek()))).soundType(SoundType.KARINA_SCOLDING)
            .latitude(37.5665).longitude(126.9780).address("서울특별시 중구 퇴계로 123")
            .locationSource(LocationSource.USER_PIN).member(member).build());
    }

    private AlarmOccurrenceEntity saveOccurrence(AlarmEntity alarm, OccurrenceStatus status) {
        return alarmOccurrenceRepository.save(AlarmOccurrenceEntity.builder().alarm(alarm)
            .occurrenceDate(FIXED_NOW.toLocalDate()).occurrenceTime(FIXED_NOW.toLocalTime()).scheduledAt(FIXED_NOW.plusHours(1))
            .status(status).alarmRinging(status == OccurrenceStatus.RINGING).ringingCount(status == OccurrenceStatus.RINGING ? 1 : 0).reminderSent(false).build());
    }

    private AdSessionEntity saveVerifiedSession(MemberEntity member, AlarmEntity alarm, AlarmOccurrenceEntity occurrence, String deviceId, AdPurpose purpose, String sessionId) {
        return adSessionRepository.save(AdSessionEntity.builder().adSessionId(sessionId).member(member).alarm(alarm).alarmOccurrence(occurrence)
            .deviceId(deviceId).purpose(purpose).status(AdSessionStatus.VERIFIED).expiresAt(FIXED_NOW.plusMinutes(10))
            .verifiedAt(FIXED_NOW.minusMinutes(1)).transactionId("tx-" + sessionId).build());
    }

    private String bearer(MemberEntity member, String deviceId) {
        return "Bearer " + jwtProvider.generateAccessToken(member.getId(), member.getRole(), deviceId);
    }
}
