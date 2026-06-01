package akuma.whiplash.domains.device.domain.service;

import static akuma.whiplash.domains.device.exception.DeviceErrorCode.DEVICE_NOT_FOUND;

import akuma.whiplash.domains.device.application.dto.request.FcmTokenUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.FcmTokenUpdateResponse;
import akuma.whiplash.domains.device.application.mapper.DeviceMapper;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final MemberDeviceRepository memberDeviceRepository;
    private final RedisService redisService;
    private final TimeProvider timeProvider;

    @Override
    public FcmTokenUpdateResponse modifyFcmToken(Long memberId, FcmTokenUpdateRequest request) {
        MemberDeviceEntity device = memberDeviceRepository
            .findByMember_IdAndDeviceId(memberId, request.deviceId())
            .orElseThrow(() -> ApplicationException.from(DEVICE_NOT_FOUND));

        device.updateFcmToken(request.fcmToken(), timeProvider.now());
        redisService.upsertFcmToken(memberId, request.deviceId(), request.fcmToken());

        return DeviceMapper.mapToFcmTokenUpdateResponse(device);
    }
}
