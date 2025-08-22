package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmRemainingOffCountResponse;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AlarmQueryService Unit Test")
@ExtendWith(MockitoExtension.class)
class AlarmQueryServiceTest {

    @Mock
    private AlarmRepository alarmRepository;

    @Mock
    private AlarmOccurrenceRepository alarmOccurrenceRepository;

    @Mock
    private AlarmOffLogRepository alarmOffLogRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AlarmQueryServiceImpl alarmQueryService;

    @Nested
    @DisplayName("getWeeklyRemainingOffCount - 남은 알람 끄기 횟수 조회")
    class GetWeeklyRemainingOffCountTest {

        @Test
        @DisplayName("성공: 이번 주 남은 OFF 횟수를 반환한다")
        void success() {
            // given
            Long memberId = MemberFixture.MEMBER_5.getId();
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(eq(memberId), any(), any()))
                .willReturn(1L);

            // when
            AlarmRemainingOffCountResponse response = alarmQueryService.getWeeklyRemainingOffCount(memberId);

            // then
            assertThat(response.remainingOffCount()).isEqualTo(1);
            verify(alarmOffLogRepository).countByMemberIdAndCreatedAtBetween(eq(memberId), any(), any());
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            Long memberId = MemberFixture.MEMBER_6.getId();
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmQueryService.getWeeklyRemainingOffCount(memberId))
                .isInstanceOf(ApplicationException.class);
        }
    }
}