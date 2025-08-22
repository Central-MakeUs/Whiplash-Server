package akuma.whiplash.domains.member.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("MemberCommandService Unit Test")
@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @InjectMocks
    private MemberCommandServiceImpl memberCommandService;

    @Mock
    private MemberRepository memberRepository;
    @Mock private AlarmRepository alarmRepository;
    @Mock private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock private AlarmOffLogRepository alarmOffLogRepository;
    @Mock private AlarmRingingLogRepository alarmRingingLogRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisService redisService;

    @Nested
    @DisplayName("modifyPushNotificationPolicy - 회원 푸시 알림 수신 동의 변경")
    class ModifyPushNotificationPolicyTest {

        @Test
        @DisplayName("성공: 푸시 알림 수신 동의를 변경한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

            // when
            memberCommandService.modifyPushNotificationPolicy(member.getId(), false);

            // then
            assertThat(member.isPushNotificationPolicy()).isFalse();
            verify(memberRepository).findById(member.getId());
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.modifyPushNotificationPolicy(999L, true))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getCode())
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("modifyPrivacyPolicy - 개인정보 수집 동의 변경")
    class ModifyPrivacyPolicyTest {

        @Test
        @DisplayName("성공: 개인정보 수집 동의를 변경한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_1.toMockEntity();
            member.updatePrivacyPolicy(false);
            MemberEntity spyMember = spy(member);
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(spyMember));

            // when
            memberCommandService.modifyPrivacyPolicy(member.getId(), true);

            // then
            verify(spyMember).updatePrivacyPolicy(true);
            assertThat(spyMember.isPrivacyPolicy()).isTrue();
        }

        @Test
        @DisplayName("실패: 회원이 없으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.modifyPrivacyPolicy(999L, true))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }
}