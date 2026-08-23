package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisService;
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
    private final AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    private final AlarmDeleteLogRepository alarmDeleteLogRepository;
    private final AdSessionRepository adSessionRepository;
    private final PaymentRepository paymentRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final JwtUtils jwtUtils;
    private final RedisService redisService;
    @Override
    public void deleteMember(Long memberId, String deviceId) {
        MemberEntity member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        // 1. alarm_ringing_log 삭제
        alarmRingingLogRepository.deleteByMemberId(memberId);

        // 2. alarm_deactivation_log, alarm_delete_log, alarm_off_log 삭제
        alarmDeactivationLogRepository.deleteByMemberId(memberId);
        alarmDeleteLogRepository.deleteByMemberId(memberId);
        alarmOffLogRepository.deleteByMemberId(memberId);

        // 3. payment, ad_session 삭제
        paymentRepository.deleteByMemberId(memberId);
        adSessionRepository.deleteByMemberId(memberId);

        // 4. alarm_occurrence, alarm 삭제
        alarmOccurrenceRepository.deleteByMemberId(memberId);
        alarmRepository.deleteByMemberId(memberId);

        // 5. member_device, member 삭제
        memberDeviceRepository.deleteByMemberId(memberId);
        memberRepository.delete(member);

        // 리프레시 토큰, FCM 토큰 삭제
        jwtUtils.expireRefreshToken(memberId, deviceId);
        redisService.removeFcmTokenForDevice(memberId, deviceId);
    }
}
