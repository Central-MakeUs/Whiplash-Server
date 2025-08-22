package akuma.whiplash.domains.member.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceTest
@DisplayName("MemberRepository Persistence Test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("푸시 알림 수신 동의를 변경하면 수정된 값이 저장된다")
    @Test
    void success_updatesPushNotificationPolicy() {
        // given
        MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());

        // when
        member.updatePushNotificationPolicy(false);
        memberRepository.save(member);

        // then
        MemberEntity updated = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updated.isPushNotificationPolicy()).isFalse();
    }

    @DisplayName("등록되지 않은 회원 ID로 조회하면 비어있는 Optional을 반환한다")
    @Test
    void fail_returnsEmpty_whenMemberNotExists() {
        // when
        Optional<MemberEntity> member = memberRepository.findById(999L);

        // then
        assertThat(member).isEmpty();
    }
}