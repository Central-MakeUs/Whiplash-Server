package akuma.whiplash.domains.member.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
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
import akuma.whiplash.global.service.ArchiveService;
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

    @Mock private MemberRepository memberRepository;
    @Mock private AlarmRepository alarmRepository;
    @Mock private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock private AlarmOffLogRepository alarmOffLogRepository;
    @Mock private AlarmRingingLogRepository alarmRingingLogRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisService redisService;
    @Mock private ArchiveService archiveService;

    @Nested
    @DisplayName("softDeleteMember - 회원 soft delete")
    class SoftDeleteMemberTest {

        @Test
        @DisplayName("성공: 회원과 관련 데이터를 삭제하고 Redis 토큰을 만료시킨다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_1.toMockEntity();
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

            // when
            memberCommandService.softDeleteMember(member.getId(), "device");

            // then
            verify(archiveService).archiveMemberWithRelations(member.getId());
            verify(alarmRepository).deleteByMemberId(member.getId());
            verify(memberRepository).delete(member);
            verify(jwtUtils).expireRefreshToken(member.getId(), "device");
            verify(redisService).removeFcmTokenForDevice(member.getId(), "device");
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.softDeleteMember(999L, "device"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getCode())
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }
}
