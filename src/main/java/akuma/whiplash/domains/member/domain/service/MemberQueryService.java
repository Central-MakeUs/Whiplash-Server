package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public MemberEntity findById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(CommonErrorCode.BAD_REQUEST));
    }
}
