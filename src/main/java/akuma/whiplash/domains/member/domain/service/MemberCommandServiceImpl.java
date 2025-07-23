package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.member.application.dto.request.MemberTermsModifyRequest;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;

    @Override
    public void modifyMemberTermsInfo(Long memberId, MemberTermsModifyRequest request) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        boolean updateFlag = false;

        if (request.privacyPolicy() != null) {
            member.updatePrivacyPolicy(request.privacyPolicy());
            updateFlag = true;
        }

        if (request.pushNotificationPolicy() != null) {
            member.updatePushNotificationPolicy(request.pushNotificationPolicy());
            updateFlag = true;
        }

        if (updateFlag) {
            memberRepository.save(member);
        }
    }
}
