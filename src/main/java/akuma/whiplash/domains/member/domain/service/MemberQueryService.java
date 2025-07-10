package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.member.persistence.entity.MemberEntity;

public interface MemberQueryService {

    MemberEntity findById(Long memberId);
}
