package akuma.whiplash.domains.auth.domain.service;

import static akuma.whiplash.domains.auth.exception.AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_DELETED;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.application.mapper.AuthMapper;
import akuma.whiplash.domains.auth.application.utils.SocialVerifier;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandServiceImpl implements AuthCommandService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final Map<String, SocialVerifier> verifierMap;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final JwtProvider jwtProvider;
    private final JwtUtils jwtUtils;
    private final RedisService redisService;
    private final TimeProvider timeProvider;

    @Override
    public LoginResponse login(SocialLoginRequest request) {
        SocialVerifier verifier = verifierMap.get(request.provider());
        if (verifier == null) {
            throw ApplicationException.from(UNSUPPORTED_SOCIAL_TYPE);
        }

        SocialMemberInfo socialMemberInfo = verifier.verify(request);

        boolean isNewMember = false;
        MemberEntity member;

        Optional<MemberEntity> findMember = memberRepository.findByProviderAndProviderUserId(
            socialMemberInfo.provider(), socialMemberInfo.providerUserId()
        );

        if (findMember.isPresent()) {
            member = findMember.get();
            if (member.isDeleted()) {
                throw ApplicationException.from(MEMBER_DELETED);
            }
            member.updateLastLoginAt();
        } else {
            member = memberRepository.save(AuthMapper.mapToMemberEntity(socialMemberInfo));
            isNewMember = true;
        }

        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), request.deviceId());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId(), request.deviceId(), member.getRole());

        upsertMemberDevice(member, request);
        redisService.upsertFcmToken(member.getId(), request.deviceId(), request.fcmToken());

        return LoginResponse.builder()
            .accessToken(BEARER_PREFIX + accessToken)
            .refreshToken(BEARER_PREFIX + refreshToken)
            .member(AuthMapper.mapToMemberInfo(member, isNewMember))
            .build();
    }

    @Override
    public void logout(MemberContext memberContext) {
        jwtUtils.expireRefreshToken(memberContext.memberId(), memberContext.deviceId());
        redisService.removeFcmTokenForDevice(memberContext.memberId(), memberContext.deviceId());

        memberDeviceRepository.findByMember_IdAndDeviceId(memberContext.memberId(), memberContext.deviceId())
            .ifPresent(MemberDeviceEntity::logout);
    }

    @Override
    public TokenResponse reissueToken(MemberContext memberContext) {
        jwtUtils.expireRefreshToken(memberContext.memberId(), memberContext.deviceId());

        String newAccessToken = jwtProvider.generateAccessToken(memberContext.memberId(), memberContext.role(), memberContext.deviceId());
        String newRefreshToken = jwtProvider.generateRefreshToken(memberContext.memberId(), memberContext.deviceId(), memberContext.role());

        return TokenResponse.builder()
            .accessToken(BEARER_PREFIX + newAccessToken)
            .refreshToken(BEARER_PREFIX + newRefreshToken)
            .build();
    }

    private void upsertMemberDevice(MemberEntity member, SocialLoginRequest request) {
        memberDeviceRepository.findByMember_IdAndDeviceId(member.getId(), request.deviceId())
            .ifPresentOrElse(
                device -> device.updateDevice(
                    request.fcmToken(),
                    request.platform(),
                    request.appVersion(),
                    request.osVersion(),
                    request.timeZone(),
                    timeProvider.now()
                ),
                () -> memberDeviceRepository.save(AuthMapper.mapToMemberDeviceEntity(member, request))
            );
    }
}
