package akuma.whiplash.domains.member.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.member.domain.contants.MemberStatus;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceTest
@DisplayName("MemberRepository Persistence Test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("존재하지 않는 회원 ID로 조회하면 빈 Optional을 반환한다")
    @Test
    void fail_returnsEmpty_whenMemberNotExists() {
        // when
        Optional<MemberEntity> member = memberRepository.findById(999L);

        // then
        assertThat(member).isEmpty();
    }

    @Nested
    @DisplayName("findByProviderAndProviderUserId - provider 기반 회원 조회")
    class FindByProviderAndProviderUserIdTest {

        @Test
        @DisplayName("성공: provider와 providerUserId로 회원을 조회한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());

            // when
            Optional<MemberEntity> result = memberRepository.findByProviderAndProviderUserId(
                member.getProvider(), member.getProviderUserId()
            );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(member.getEmail());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 providerUserId이면 빈 값을 반환한다")
        void fail_memberNotFound() {
            // when
            Optional<MemberEntity> result = memberRepository.findByProviderAndProviderUserId(
                MemberFixture.MEMBER_1.getProvider(), "non-existent-id"
            );

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateLastLoginAt - 마지막 로그인 시각 업데이트")
    class UpdateLastLoginAtTest {

        @Test
        @DisplayName("성공: updateLastLoginAt 호출 후 lastLoginAt이 저장된다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            assertThat(member.getLastLoginAt()).isNull();

            // when
            member.updateLastLoginAt();
            memberRepository.save(member);

            // then
            MemberEntity updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getLastLoginAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("isDeleted - 탈퇴 회원 판별")
    class IsDeletedTest {

        @Test
        @DisplayName("성공: status가 ACTIVE이면 isDeleted는 false를 반환한다")
        void success_activeIsNotDeleted() {
            // given
            MemberEntity member = MemberFixture.MEMBER_3.toMockEntity();

            // when & then
            assertThat(member.isDeleted()).isFalse();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }
    }
}
