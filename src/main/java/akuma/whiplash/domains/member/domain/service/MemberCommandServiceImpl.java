package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
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
    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;

    @Override
    public void modifyMemberTermsInfo(Long memberId, MemberTermsModifyRequest request) {
        MemberEntity member = memberRepository.findByIdAndActiveStatusIsTrue(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        if (request.privacyPolicy() != null) {
            member.updatePrivacyPolicy(request.privacyPolicy());
        }

        if (request.pushNotificationPolicy() != null) {
            member.updatePushNotificationPolicy(request.pushNotificationPolicy());
        }
    }

    @Override
    public void softDeleteMember(Long memberId) {
        MemberEntity member = memberRepository.findByIdAndActiveStatusIsTrue(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        // 1. member 비활성화
        member.updateDeactivate();

        // 뒤에서 @Modifying(clearAutomatically = true) 호출하므로 명시적으로 영속성 컨텍스트 호출 필요
        memberRepository.flush();

        // 2. alarm 테이블 비정규화 컬럼 동기화
        alarmRepository.updateMemberDeactivateByMemberId(memberId);

        // 3. alarm_occurrence 테이블 비정규화 컬럼 동기화
        alarmOccurrenceRepository.updateMemberDeactivateByMemberId(memberId);

        // TODO: alarm_ringing_log 삭제

        // TODO: 리프레시 토큰, FCM 토큰 삭제
    }
}
