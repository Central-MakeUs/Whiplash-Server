package akuma.whiplash.domains.member.persistence.repository;

import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberDeviceRepository extends JpaRepository<MemberDeviceEntity, Long> {

    Optional<MemberDeviceEntity> findByMember_IdAndDeviceId(Long memberId, String deviceId);

    Optional<MemberDeviceEntity> findFirstByMember_IdAndIsLoggedInTrueOrderByLastActiveAtDesc(Long memberId);

    @Modifying
    @Query("""
        DELETE FROM MemberDeviceEntity md
        WHERE md.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
