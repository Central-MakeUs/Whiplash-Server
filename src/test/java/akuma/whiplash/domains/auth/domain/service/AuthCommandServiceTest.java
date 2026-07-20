package akuma.whiplash.domains.auth.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.application.utils.SocialVerifier;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.domain.contants.MemberStatus;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AuthCommandService Unit Test")
@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock private Map<String, SocialVerifier> verifierMap;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberDeviceRepository memberDeviceRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisService redisService;
    @Mock private SocialVerifier socialVerifier;

    @InjectMocks
    private AuthCommandServiceImpl authCommandService;

    private MemberContext buildContext() {
        return MemberContext.builder()
            .memberId(1L)
            .deviceId("device")
            .role(Role.USER)
            .build();
    }

    private SocialLoginRequest buildSocialLoginRequest(String provider, String deviceId, String fcmToken) {
        return new SocialLoginRequest(
            provider,
            "provider-access-token",
            deviceId,
            "ANDROID",
            fcmToken,
            "1.0.0",
            "14",
            "Asia/Seoul"
        );
    }

    private SocialMemberInfo buildSocialMemberInfo(String providerUserId) {
        return SocialMemberInfo.builder()
            .provider(SocialType.MOCK)
            .providerUserId(providerUserId)
            .email("mock-user@test.com")
            .name("Mock User")
            .build();
    }

    private MemberEntity buildMember(Long memberId, SocialType socialType, String providerUserId, MemberStatus status) {
        return MemberEntity.builder()
            .id(memberId)
            .provider(socialType)
            .providerUserId(providerUserId)
            .email("saved-user@test.com")
            .nickname("Saved User")
            .role(Role.USER)
            .status(status)
            .build();
    }

    @Nested
    @DisplayName("login - 소셜 로그인")
    class LoginTest {

        @Test
        @DisplayName("성공: 신규 회원이면 저장 후 isNewMember=true 를 반환한다")
        void success_newMember() {
            // given
            SocialLoginRequest request = buildSocialLoginRequest("MOCK", "new-device", "new-fcm-token");
            SocialMemberInfo socialMemberInfo = buildSocialMemberInfo("mock-new-user");
            MemberEntity savedMember = buildMember(101L, SocialType.MOCK, "mock-new-user", MemberStatus.ACTIVE);

            when(verifierMap.get("MOCK")).thenReturn(socialVerifier);
            when(socialVerifier.verify(request)).thenReturn(socialMemberInfo);
            when(memberRepository.findByProviderAndProviderUserId(SocialType.MOCK, "mock-new-user"))
                .thenReturn(Optional.empty());
            when(memberRepository.save(any(MemberEntity.class))).thenReturn(savedMember);
            when(jwtProvider.generateAccessToken(101L, Role.USER, "new-device")).thenReturn("new-access");
            when(jwtProvider.generateRefreshToken(101L, "new-device", Role.USER)).thenReturn("new-refresh");
            when(memberDeviceRepository.findByMember_IdAndDeviceId(101L, "new-device"))
                .thenReturn(Optional.empty());

            // when
            LoginResponse response = authCommandService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("Bearer new-access");
            assertThat(response.refreshToken()).isEqualTo("Bearer new-refresh");
            assertThat(response.member().memberId()).isEqualTo(101L);
            assertThat(response.member().isNewMember()).isTrue();
            verify(memberRepository).save(any(MemberEntity.class));
            verify(memberDeviceRepository).save(any());
            verify(redisService).upsertFcmToken(101L, "new-device", "new-fcm-token");
        }

        @Test
        @DisplayName("성공: 기존 회원이면 로그인 처리 후 updateLastLoginAt 을 호출한다")
        void success_existingMember() {
            // given
            SocialLoginRequest request = buildSocialLoginRequest("MOCK", "existing-device", "existing-fcm-token");
            SocialMemberInfo socialMemberInfo = buildSocialMemberInfo("mock-existing-user");
            MemberEntity existingMember = spy(
                buildMember(102L, SocialType.MOCK, "mock-existing-user", MemberStatus.ACTIVE)
            );

            when(verifierMap.get("MOCK")).thenReturn(socialVerifier);
            when(socialVerifier.verify(request)).thenReturn(socialMemberInfo);
            when(memberRepository.findByProviderAndProviderUserId(SocialType.MOCK, "mock-existing-user"))
                .thenReturn(Optional.of(existingMember));
            when(jwtProvider.generateAccessToken(102L, Role.USER, "existing-device")).thenReturn("existing-access");
            when(jwtProvider.generateRefreshToken(102L, "existing-device", Role.USER)).thenReturn("existing-refresh");
            when(memberDeviceRepository.findByMember_IdAndDeviceId(102L, "existing-device"))
                .thenReturn(Optional.empty());

            // when
            LoginResponse response = authCommandService.login(request);

            // then
            assertThat(response.member().isNewMember()).isFalse();
            verify(existingMember).updateLastLoginAt();
            verify(memberRepository, never()).save(any(MemberEntity.class));
        }

        @Test
        @DisplayName("실패: 탈퇴 회원이면 MEMBER_DELETED 예외를 던진다")
        void fail_deletedMember() {
            // given
            SocialLoginRequest request = buildSocialLoginRequest("MOCK", "deleted-device", "deleted-fcm-token");
            SocialMemberInfo socialMemberInfo = buildSocialMemberInfo("mock-deleted-user");
            MemberEntity deletedMember = buildMember(103L, SocialType.MOCK, "mock-deleted-user", MemberStatus.DELETED);

            when(verifierMap.get("MOCK")).thenReturn(socialVerifier);
            when(socialVerifier.verify(request)).thenReturn(socialMemberInfo);
            when(memberRepository.findByProviderAndProviderUserId(SocialType.MOCK, "mock-deleted-user"))
                .thenReturn(Optional.of(deletedMember));

            // when & then
            assertThatThrownBy(() -> authCommandService.login(request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(MemberErrorCode.MEMBER_DELETED)
                );
        }

        @Test
        @DisplayName("실패: verifier가 없으면 UNSUPPORTED_SOCIAL_TYPE 예외를 던진다")
        void fail_unsupportedSocialType() {
            // given
            SocialLoginRequest request = buildSocialLoginRequest("NAVER", "unsupported-device", "unsupported-fcm-token");
            when(verifierMap.get("NAVER")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> authCommandService.login(request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE)
                );
        }
    }

    @Nested
    @DisplayName("logout - 로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("성공: 리프레시 토큰과 FCM 토큰을 삭제하고 device 로그아웃 처리한다")
        void success() {
            // given
            MemberContext ctx = buildContext();
            when(memberDeviceRepository.findByMember_IdAndDeviceId(1L, "device"))
                .thenReturn(Optional.empty());

            // when
            authCommandService.logout(ctx);

            // then
            verify(jwtUtils).expireRefreshToken(1L, "device");
            verify(redisService).removeFcmTokenForDevice(1L, "device");
        }

        @Test
        @DisplayName("실패: 리프레시 토큰이 없으면 예외를 던진다")
        void fail_tokenNotFound() {
            // given
            MemberContext ctx = buildContext();
            doThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN))
                .when(jwtUtils).expireRefreshToken(1L, "device");

            // when & then
            assertThatThrownBy(() -> authCommandService.logout(ctx))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
                );
        }
    }

    @Nested
    @DisplayName("reissueToken - 토큰 재발급")
    class ReissueTokenTest {

        @Test
        @DisplayName("성공: 새 액세스 및 리프레시 토큰을 반환한다")
        void success() {
            // given
            MemberContext context = buildContext();
            when(jwtProvider.generateAccessToken(context.memberId(), context.role(), context.deviceId()))
                .thenReturn("newAccess");
            when(jwtProvider.generateRefreshToken(context.memberId(), context.deviceId(), context.role()))
                .thenReturn("newRefresh");

            // when
            TokenResponse response = authCommandService.reissueToken(context);

            // then
            assertThat(response.accessToken()).isEqualTo("Bearer newAccess");
            assertThat(response.refreshToken()).isEqualTo("Bearer newRefresh");
            verify(jwtUtils).expireRefreshToken(context.memberId(), context.deviceId());
        }

        @Test
        @DisplayName("실패: 토큰 생성 중 예외가 발생하면 예외를 던진다")
        void fail_tokenGenerationError() {
            // given
            MemberContext context = buildContext();
            when(jwtProvider.generateAccessToken(context.memberId(), context.role(), context.deviceId()))
                .thenThrow(new RuntimeException("error"));

            // when & then
            assertThatThrownBy(() -> authCommandService.reissueToken(context))
                .isInstanceOf(RuntimeException.class);
        }
    }
}
