package akuma.whiplash.domains.alarm.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
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

    @DisplayName("알람 등록 요청이 성공하면 알람이 저장된다")
    @Test
    void createAlarm_savesAlarm_whenRequestValid() throws Exception {
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

    @DisplayName("반복 요일이 비어 있으면 400 응답을 반환한다")
    @Test
    void createAlarm_returnsBadRequest_whenRepeatDaysEmpty() throws Exception {
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