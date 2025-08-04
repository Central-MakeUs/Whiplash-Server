package akuma.whiplash.domains.member.persistence.repository;

import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findBySocialId(String socialId);
    Optional<MemberEntity> findByIdAndActiveStatusIsTrue(Long memberId);
}
