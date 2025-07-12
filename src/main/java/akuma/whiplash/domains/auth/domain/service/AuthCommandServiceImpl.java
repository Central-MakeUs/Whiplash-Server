package akuma.whiplash.domains.auth.domain.service;

import static akuma.whiplash.global.response.code.CommonErrorCode.*;

import akuma.whiplash.domains.auth.application.AppleVerifier;
import akuma.whiplash.domains.auth.application.GoogleVerifier;
import akuma.whiplash.domains.auth.application.KakaoVerifier;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.mapper.AuthMapper;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandServiceImpl implements AuthCommandService {

    private final GoogleVerifier googleVerifier;
    private final KakaoVerifier kakaoVerifier;
    private final AppleVerifier appleVerifier;
    private final MemberRepository memberRepository;
    private final RedisRepository redisRepository;
    private final JwtProvider jwtProvider;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(SocialLoginRequest request) {
        SocialMemberInfo socialMemberInfo = switch (request.socialType()) {
            case "GOOGLE" -> googleVerifier.verify(request);
            case "APPLE" -> appleVerifier.verify(request);
            case "KAKAO" -> kakaoVerifier.verify(request);
            default -> throw ApplicationException.from(BAD_REQUEST);
        };

        // DB에서 사용자 조회 or 신규 가입
        MemberEntity member = memberRepository.findBySocialId(socialMemberInfo.socialId())
                .orElseGet(() -> memberRepository.save(
                    AuthMapper.toMemberEntity(socialMemberInfo)
                ));

        // TODO: 멀티 디바이스 로그인 가능하도록 처리 필요
        // 다른 디바이스 ID로 리프레시 토큰 존재할 시 기존 리프레시 토큰 삭제
        redisRepository.getKeys("REFRESH:" + member.getId() + ":*")
            .stream()
            .filter(key -> !key.endsWith(request.deviceId()))
            .findFirst().ifPresent(redisRepository::deleteValues);

        // JWT 생성
        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId(), request.deviceId(), member.getRole());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .nickname(member.getNickname())
            .build();
    }

    @Override
    public void logout(LogoutRequest request, Long memberId) {
        jwtUtils.expireRefreshToken(memberId, request.deviceId());
    }
}