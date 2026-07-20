package akuma.whiplash.domains.member.persistence.repository;

import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDeviceRepository extends JpaRepository<MemberDeviceEntity, Long> {

    Optional<MemberDeviceEntity> findByMember_IdAndDeviceId(Long memberId, String deviceId);

    Optional<MemberDeviceEntity> findFirstByMember_IdAndIsLoggedInTrueOrderByLastActiveAtDesc(Long memberId);
}
