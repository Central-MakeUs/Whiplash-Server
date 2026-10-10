package akuma.whiplash.domains.alarm.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest;
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
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.util.date.TimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    @MockitoBean private GoogleClient googleClient;

    @BeforeEach
    void setUpTimeProvider() {
        given(timeProvider.now()).willReturn(FIXED_NOW);
        given(timeProvider.today(any(ZoneId.class))).willReturn(LocalDate.of(2026, 5, 4));
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
    @DisplayName("createAlarm - 알람 장소 등록")
    class CreateAlarmTest {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("성공: 검색 장소와 일반 핀을 등록한 뒤 목록에는 주소만 표시한다")
        void success_listShowsAddressOnly() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            String authorization = bearer(member, device.getDeviceId());
            AlarmRegisterRequest googlePlace = new AlarmRegisterRequest(
                new PlaceRequest("서울특별시 중구 세종대로 110", 37.5665, 126.978, "ChIJ", null),
                "검색 장소", LocalTime.of(12, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );
            AlarmRegisterRequest userPin = new AlarmRegisterRequest(
                new PlaceRequest("서울특별시 중구 퇴계로 123", 37.5642, 127.0016, null, null),
                "일반 지점", LocalTime.of(13, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(googlePlace)))
                .andExpect(status().isOk());
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userPin)))
                .andExpect(status().isOk());

            // when
            var result = mockMvc.perform(get(BASE)
                .header(HttpHeaders.AUTHORIZATION, authorization));

            // then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarms[*].address", hasItems(
                    "서울특별시 중구 세종대로 110", "서울특별시 중구 퇴계로 123"
                )))
                .andExpect(jsonPath("$.result.alarms[0].placeName").doesNotExist())
                .andExpect(jsonPath("$.result.alarms[1].placeName").doesNotExist());
        }

        @Test
        @DisplayName("성공: 일반 지도 핀은 터치 좌표와 주소 캐시를 저장한다")
        void success_userPin() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new PlaceRequest("서울특별시 중구 세종대로 110", 37.5665, 126.978, null, null),
                "핀 출근", LocalTime.of(12, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );

            // when
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            AlarmEntity alarm = alarmRepository.findAll().get(0);
            assertThat(alarm.getLocationSource()).isEqualTo(LocationSource.USER_PIN);
            assertThat(alarm.getGooglePlaceId()).isNull();
            assertThat(alarm.getAddress()).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(alarm.getLocationCachedAt()).isEqualTo(FIXED_NOW);
            assertThat(alarm.getLatitude()).isEqualTo(37.5665);
            assertThat(alarm.getLongitude()).isEqualTo(126.978);

            Long occurrenceId = alarmOccurrenceRepository.findAllByAlarmId(alarm.getId()).get(0).getId();
            mockMvc.perform(post(BASE + "/{alarmId}/occurrences/{occurrenceId}/destination", alarm.getId(), occurrenceId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value("READY"))
                .andExpect(jsonPath("$.result.targetLocation.latitude").value(37.5665))
                .andExpect(jsonPath("$.result.targetLocation.longitude").value(126.978));
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("성공: POI는 상세 주소 없이 Place ID와 좌표로 등록한다")
        void success_googlePlaceWithoutDetails() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new PlaceRequest(null, 37.5665, 126.978, "ChIJ", null),
                "POI 출근", LocalTime.of(12, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );

            // when
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            AlarmEntity alarm = alarmRepository.findAll().get(0);
            assertThat(alarm.getLocationSource()).isEqualTo(LocationSource.GOOGLE_PLACE);
            assertThat(alarm.getGooglePlaceId()).isEqualTo("ChIJ");
            assertThat(alarm.getAddress()).isNull();
            assertThat(alarm.getLocationCachedAt()).isEqualTo(FIXED_NOW);

            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("ChIJ", null, null, null)))
                .willReturn(new SelectedPlaceDetail(
                    "서울특별시 중구 세종대로 110", 37.5665, 126.978, "KR", "ChIJ"
                ));
            mockMvc.perform(get(BASE)
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarms[0].address").value("서울특별시 중구 세종대로 110"))
                .andExpect(jsonPath("$.result.alarms[0].placeName").doesNotExist());
            assertThat(alarmRepository.findById(alarm.getId()).orElseThrow().getAddress())
                .isEqualTo("서울특별시 중구 세종대로 110");
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("성공: 주소 없이 등록한 일반 핀은 목록 조회에서 역지오코딩 주소를 캐시한다")
        void success_userPinWithoutAddress() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new PlaceRequest(null, 37.5665, 126.978, null, null),
                "핀 출근", LocalTime.of(12, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );

            // when
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            AlarmEntity alarm = alarmRepository.findAll().get(0);
            assertThat(alarm.getLocationSource()).isEqualTo(LocationSource.USER_PIN);
            assertThat(alarm.getAddress()).isNull();
            assertThat(alarm.getLocationCachedAt()).isNull();

            given(googleClient.reverseGeocode(37.5665, 126.978, "ko"))
                .willReturn(new PlaceDetail(
                    "서울특별시 중구 세종대로 110", null, null, 37.5665, 126.978, "KR"
                ));
            mockMvc.perform(get(BASE)
                    .header(HttpHeaders.AUTHORIZATION, bearer(member, device.getDeviceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarms[0].address").value("서울특별시 중구 세종대로 110"));

            AlarmEntity refreshed = alarmRepository.findById(alarm.getId()).orElseThrow();
            assertThat(refreshed.getAddress()).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(refreshed.getLatitude()).isEqualTo(37.5665);
            assertThat(refreshed.getLongitude()).isEqualTo(126.978);
            assertThat(refreshed.getLocationCachedAt()).isEqualTo(FIXED_NOW);
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("성공: 역지오코딩 장애 중에도 알람 목록은 주소를 비워 반환한다")
        void success_userPinAddressProviderUnavailable() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            String authorization = bearer(member, device.getDeviceId());
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new PlaceRequest(null, 37.5665, 126.978, null, null),
                "핀 출근", LocalTime.of(12, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
            given(googleClient.reverseGeocode(37.5665, 126.978, "ko"))
                .willThrow(new IllegalStateException("provider unavailable"));

            // when
            var result = mockMvc.perform(get(BASE)
                .header(HttpHeaders.AUTHORIZATION, authorization));

            // then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarms[0].address").value(nullValue()));
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("성공: POI 주소 조회 실패 뒤에도 유효한 목표 좌표로 도착 인증한다")
        void success_googleAddressProviderUnavailableKeepsDestination() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            MemberDeviceEntity device = memberDeviceRepository.save(MemberDeviceFixture.IOS.toEntity(member));
            String authorization = bearer(member, device.getDeviceId());
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new PlaceRequest(null, 37.5665, 126.978, "ChIJ", null),
                "POI 출근", LocalTime.of(12, 0), List.of("MONDAY"), "KARINA_SCOLDING"
            );
            mockMvc.perform(post(BASE)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
            AlarmEntity alarm = alarmRepository.findAll().get(0);
            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("ChIJ", null, null, null)))
                .willThrow(new IllegalStateException("provider unavailable"));

            // when
            mockMvc.perform(get(BASE)
                    .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.alarms[0].address").value(nullValue()));

            // then
            AlarmEntity retained = alarmRepository.findById(alarm.getId()).orElseThrow();
            assertThat(retained.getLatitude()).isEqualTo(37.5665);
            assertThat(retained.getLongitude()).isEqualTo(126.978);
            Long occurrenceId = alarmOccurrenceRepository.findAllByAlarmId(alarm.getId()).get(0).getId();
            mockMvc.perform(post(BASE + "/{alarmId}/occurrences/{occurrenceId}/destination", alarm.getId(), occurrenceId)
                    .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.state").value("READY"));
        }
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

            assertThat(alarmRepository.findById(alarm.getId())).isEmpty();
            assertThat(alarmOccurrenceRepository.findAllByAlarmId(alarm.getId())).isEmpty();
            assertThat(adSessionRepository.findById(session.getId())).isEmpty();
            assertThat(alarmDeleteLogRepository.findAll()).isEmpty();
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
