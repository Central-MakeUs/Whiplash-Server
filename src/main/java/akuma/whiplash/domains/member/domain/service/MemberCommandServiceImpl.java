package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
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
    private final AlarmOffLogRepository alarmOffLogRepository;
    private final AlarmRingingLogRepository alarmRingingLogRepository;

    @Override
    public void modifyMemberTermsInfo(Long memberId, MemberTermsModifyRequest request) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        if (request.privacyPolicy() != null) {
            member.updatePrivacyPolicy(request.privacyPolicy());
        }

        if (request.pushNotificationPolicy() != null) {
            member.updatePushNotificationPolicy(request.pushNotificationPolicy());
        }
    }

    @Override
    public void hardDeleteMember(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        // 2. alarm_ringing_log 삭제
        alarmRingingLogRepository.deleteByMemberId(memberId);

        // 3. alarm_occurrence 삭제
        alarmOccurrenceRepository.deleteByMemberId(memberId);

        // 4. alarm_off_log 삭제
        alarmOffLogRepository.deleteByMemberId(memberId);

        // 5. alarm 삭제
        alarmRepository.deleteByMemberId(memberId);

        // 6. member 삭제
        memberRepository.delete(member);


        // TODO: 리프레시 토큰, FCM 토큰 삭제
    }
}
