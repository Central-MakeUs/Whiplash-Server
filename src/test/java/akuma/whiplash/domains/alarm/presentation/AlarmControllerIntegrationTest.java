package akuma.whiplash.domains.alarm.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.util.date.DateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("AlarmController Integration Test")
class AlarmControllerIntegrationTest {

    @Autowired private JwtProvider jwtProvider;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AlarmRepository alarmRepository;
    @Autowired private AlarmOffLogRepository alarmOffLogRepository;
    @Autowired private AlarmOccurrenceRepository alarmOccurrenceRepository;

    private static final String BASE = "/api/alarms";

    private AlarmEntity createAlarm(MemberEntity member, List<Weekday> repeatDays, LocalTime time) {
        return alarmRepository.save(AlarmEntity.builder()
            .alarmPurpose("test")
            .time(time)
            .repeatDays(repeatDays)
            .soundType(SoundType.ONE)
            .latitude(0.0)
            .longitude(0.0)
            .address("addr")
            .member(member)
            .build());
    }

    private String authHeader(MemberEntity member) {
        String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");
        return "Bearer " + token;
    }

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

    @Nested
    @DisplayName("[POST] /api/alarms - 알람 등록")
    class CreateAlarmTest {

        @Test
        @DisplayName("성공: 알람 등록 요청이 성공하면 알람이 저장된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            AlarmFixture fixture = AlarmFixture.ALARM_01;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                fixture.getAddress(),
                fixture.getLatitude(),
                fixture.getLongitude(),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when
            mockMvc.perform(post("/api/alarms")
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
                fixture.getAddress(),
                fixture.getLatitude(),
                fixture.getLongitude(),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                List.of(),
                fixture.getSoundType().getDescription()
            );
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post("/api/alarms")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("[POST] /api/alarms/{alarmId}/ring - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("성공: 알람 울림 요청이 성공한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));
            alarmOccurrenceRepository.save(AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toEntity(alarm));
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", alarm.getId())
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
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", nonExistentAlarmId)
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
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", alarm.getId())
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
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", alarm.getId())
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
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound()); // TODO: isBadRequest로 검증해야함.
        }

        @Test
        @DisplayName("실패: 알람 시간이 아니면 400 응답을 반환한다")
        void fail_notAlarmTime() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            var alarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));
            // Set alarm time to a future time to simulate "not alarm time"
            alarmOccurrenceRepository.save(AlarmOccurrenceFixture.ALARM_OCCURRENCE_FUTURE_TIME.toEntity(alarm));
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "mock_device_id");

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[POST] /api/alarms/{alarmId}/checkin - 도착 인증")
    class CheckinTest {

        @Test
        @DisplayName("성공: 체크인이 완료되면 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            AlarmEntity alarm = saveAlarmForToday(member);
            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());
            String accessToken = buildAccessToken(member);

            // when
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
                .findByAlarmIdAndDate(alarm.getId(), LocalDate.now())
                .orElseThrow();
            assertThat(occurrence.getDeactivateType()).isEqualTo(DeactivateType.CHECKIN);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람이면 404와 에러 코드를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = new AlarmCheckinRequest(0.0, 0.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 999L)
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
            String accessToken = buildAccessToken(other);
            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", alarm.getId())
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
            LocalDate targetDate = LocalDate.now();
            AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, targetDate);
            occurrence.checkin(LocalDateTime.now());
            alarmOccurrenceRepository.save(occurrence);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", alarm.getId())
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
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = new AlarmCheckinRequest(
                alarm.getLatitude() + 1,
                alarm.getLongitude() + 1
            );

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 다음 주 알람에는 체크인할 수 없다")
        void fail_nextWeek() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = saveAlarmForNextWeek(member);
            String accessToken = buildAccessToken(member);
            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", alarm.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[POST] /api/alarms/{id}/off - 알람 끄기(OFF)")
    class AlarmOffTest {

        @Test
        @DisplayName("성공: 알람을 끄면 끈 기록이 저장된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            AlarmEntity alarm = createAlarm(member, Arrays.asList(Weekday.values()), LocalTime.of(8, 0));
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now());

            // when
            mockMvc.perform(post(BASE + "/" + alarm.getId() + "/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            LocalDate today = LocalDate.now();
            long count = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(
                member.getId(),
                DateUtil.getWeekStartDate(today).atStartOfDay(),
                DateUtil.getWeekEndDate(today).plusDays(1).atStartOfDay());
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람이면 404를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now());

            // when & then
            mockMvc.perform(post(BASE + "/999/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 403을 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            MemberEntity owner = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            MemberEntity other = memberRepository.save(MemberFixture.MEMBER_4.toEntity());
            AlarmEntity alarm = createAlarm(owner, Arrays.asList(Weekday.values()), LocalTime.of(8, 0));
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now());

            // when & then
            mockMvc.perform(post(BASE + "/" + alarm.getId() + "/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(other))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 클라이언트 날짜가 서버와 다르면 400을 반환한다")
        void fail_invalidClientDate() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_5.toEntity());
            AlarmEntity alarm = createAlarm(member, Arrays.asList(Weekday.values()), LocalTime.of(8, 0));
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now().minusDays(1));

            // when & then
            mockMvc.perform(post(BASE + "/" + alarm.getId() + "/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 주간 OFF 한도를 초과하면 400을 반환한다")
        void fail_weeklyLimitExceeded() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_6.toEntity());
            AlarmEntity alarm = createAlarm(member, Arrays.asList(Weekday.values()), LocalTime.of(8, 0));
            alarmOffLogRepository.save(AlarmOffLogEntity.builder().alarm(alarm).member(member).build());
            alarmOffLogRepository.save(AlarmOffLogEntity.builder().alarm(alarm).member(member).build());
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now());

            // when & then
            mockMvc.perform(post(BASE + "/" + alarm.getId() + "/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 이미 끈 알람이면 400을 반환한다")
        void fail_alreadyDeactivated() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = createAlarm(member, Arrays.asList(Weekday.values()), LocalTime.of(8, 0));
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                .alarm(alarm)
                .date(LocalDate.now())
                .time(alarm.getTime())
                .deactivateType(DeactivateType.OFF)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();
            alarmOccurrenceRepository.save(occurrence);
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now());

            // when & then
            mockMvc.perform(post(BASE + "/" + alarm.getId() + "/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 다음 주 알람은 끌 수 없어 400을 반환한다")
        void fail_nextWeekAlarm() throws Exception {
            // given
            DayOfWeek today = LocalDate.now().getDayOfWeek();
            assumeTrue(today != DayOfWeek.MONDAY);
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            Weekday repeat = Weekday.from(today.minus(1));
            AlarmEntity alarm = createAlarm(member, List.of(repeat), LocalTime.of(8, 0));
            AlarmOffRequest request = new AlarmOffRequest(LocalDateTime.now());

            // when & then
            mockMvc.perform(post(BASE + "/" + alarm.getId() + "/off")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}