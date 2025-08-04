package akuma.whiplash.domains.auth.domain.service;

import static akuma.whiplash.domains.auth.exception.AuthErrorCode.*;
import static akuma.whiplash.global.response.code.CommonErrorCode.*;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.application.utils.AppleVerifier;
import akuma.whiplash.domains.auth.application.utils.GoogleVerifier;
import akuma.whiplash.domains.auth.application.utils.KakaoVerifier;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.mapper.AuthMapper;
import akuma.whiplash.domains.auth.application.utils.MockVerifier;
import akuma.whiplash.domains.auth.application.utils.SocialVerifier;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisRepository;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RedisRepository redisRepository;
    private final JwtProvider jwtProvider;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(SocialLoginRequest request) {
        SocialVerifier verifier = verifierMap.get(request.socialType());
        if (verifier == null) {
            throw ApplicationException.from(UNSUPPORTED_SOCIAL_TYPE);
        }

        SocialMemberInfo socialMemberInfo = verifier.verify(request);
        
        // TODO: 탈퇴 후 재가입시 탈퇴한 회원의 정보가 조회되는 문제 해결 필요
        // DB에서 사용자 조회 or 신규 가입
        boolean isNewMember = false;
        MemberEntity member;

        Optional<MemberEntity> findMember = memberRepository.findBySocialId(socialMemberInfo.socialId());

        if (findMember.isPresent()) {
            // TODO:휴면 계정으로 로그인 하시겠습니까? 처리
            if (!findMember.get().isActiveStatus()) throw ApplicationException.from(AuthErrorCode.IS_SLEEPER_ACCOUNT);
            else member = findMember.get();
        } else {
            member = memberRepository.save(AuthMapper.mapToMemberEntity(socialMemberInfo));
            isNewMember = true;
        }

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
            .accessToken(BEARER_PREFIX + accessToken)
            .refreshToken(BEARER_PREFIX + refreshToken)
            .nickname(member.getNickname())
            .isNewMember(isNewMember)
            .build();
    }

    @Override
    public void logout(LogoutRequest request, Long memberId) {
        jwtUtils.expireRefreshToken(memberId, request.deviceId());
    }

    @Override
    public TokenResponse reissueToken(String oldRefreshToken, MemberContext memberContext, String deviceId) {
        jwtUtils.validateRefreshToken(oldRefreshToken, memberContext.memberId(), deviceId);
        jwtUtils.expireRefreshToken(memberContext.memberId(), deviceId);

        String accessToken = jwtProvider.generateAccessToken(memberContext.memberId(), memberContext.role());
        String newRefreshToken = jwtProvider.generateRefreshToken(memberContext.memberId(), deviceId, memberContext.role());

        return TokenResponse.builder()
            .accessToken(BEARER_PREFIX + accessToken)
            .refreshToken(BEARER_PREFIX + newRefreshToken)
            .build();
    }
}