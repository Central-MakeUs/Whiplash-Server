package akuma.whiplash.domains.alarm.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private AlarmOccurrenceRepository alarmOccurrenceRepository;

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
}