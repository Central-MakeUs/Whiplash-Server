package akuma.whiplash.domains.member.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.MemberFixture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("MemberRepository Lock Persistence Test")
@PersistenceTest
class MemberRepositoryLockTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Nested
    @DisplayName("findByIdForUpdate - 회원 탈퇴 잠금")
    class FindByIdForUpdateTest {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("성공: 먼저 획득한 잠금이 해제될 때까지 다음 트랜잭션이 대기한다")
        void success() throws Exception {
            // given
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            Long memberId = transactionTemplate.execute(status ->
                memberRepository.save(MemberFixture.MEMBER_1.toEntity()).getId()
            );
            CountDownLatch firstLockAcquired = new CountDownLatch(1);
            CountDownLatch releaseFirstLock = new CountDownLatch(1);
            CountDownLatch secondLockAcquired = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                Future<?> firstTransaction = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        memberRepository.findByIdForUpdate(memberId).orElseThrow();
                        firstLockAcquired.countDown();
                        await(releaseFirstLock);
                    })
                );
                assertThat(firstLockAcquired.await(3, TimeUnit.SECONDS)).isTrue();

                Future<?> secondTransaction = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        memberRepository.findByIdForUpdate(memberId).orElseThrow();
                        secondLockAcquired.countDown();
                    })
                );

                // when
                boolean acquiredBeforeRelease = secondLockAcquired.await(300, TimeUnit.MILLISECONDS);
                releaseFirstLock.countDown();
                firstTransaction.get(3, TimeUnit.SECONDS);
                secondTransaction.get(3, TimeUnit.SECONDS);

                // then
                assertThat(acquiredBeforeRelease).isFalse();
                assertThat(secondLockAcquired.getCount()).isZero();
            } finally {
                releaseFirstLock.countDown();
                executor.shutdownNow();
                executor.awaitTermination(3, TimeUnit.SECONDS);
                transactionTemplate.executeWithoutResult(status -> memberRepository.deleteById(memberId));
            }
        }

        private void await(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
