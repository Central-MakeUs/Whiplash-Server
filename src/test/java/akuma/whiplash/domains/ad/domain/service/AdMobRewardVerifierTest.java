package akuma.whiplash.domains.ad.domain.service;

import static akuma.whiplash.domains.ad.exception.AdErrorCode.INVALID_ADMOB_CALLBACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import akuma.whiplash.global.exception.ApplicationException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("AdMobRewardVerifier Unit Test")
class AdMobRewardVerifierTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("verify - AdMob SSV 서명 검증")
    class VerifyTest {

        @Test
        @DisplayName("성공: 유효한 서명이면 콜백 정보를 반환한다")
        void success() throws Exception {
            // given
            long keyId = 3335741209L;
            KeyPair keyPair = generateKeyPair();
            enqueuePublicKey(keyId, keyPair);
            String signedContent = "custom_data=ad-session-id&transaction_id=transaction-id&reward_amount=1&reward_item=coin&ad_unit=ad-unit";
            String signature = sign(signedContent, keyPair);
            MockHttpServletRequest request = request(signedContent + "&signature=" + signature + "&key_id=" + keyId);
            AdMobRewardVerifier verifier = verifier();

            // when
            AdMobRewardCallback callback = verifier.verify(request);

            // then
            assertThat(callback.customData()).isEqualTo("ad-session-id");
            assertThat(callback.transactionId()).isEqualTo("transaction-id");
            assertThat(callback.rewardAmount()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패: 서명이 유효하지 않으면 예외를 던진다")
        void fail_invalidSignature() throws Exception {
            // given
            long keyId = 3335741209L;
            KeyPair keyPair = generateKeyPair();
            enqueuePublicKey(keyId, keyPair);
            String signedContent = "custom_data=ad-session-id&transaction_id=transaction-id";
            String invalidSignature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("invalid".getBytes(StandardCharsets.UTF_8));
            MockHttpServletRequest request = request(signedContent + "&signature=" + invalidSignature + "&key_id=" + keyId);
            AdMobRewardVerifier verifier = verifier();

            // when & then
            assertThatThrownBy(() -> verifier.verify(request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(INVALID_ADMOB_CALLBACK)
                );
        }
    }

    private AdMobRewardVerifier verifier() {
        return new AdMobRewardVerifierImpl(
            WebClient.builder(),
            mockWebServer.url("/keys").toString()
        );
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private void enqueuePublicKey(long keyId, KeyPair keyPair) {
        String base64 = Base64.getEncoder().encodeToString(((ECPublicKey) keyPair.getPublic()).getEncoded());
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"keys":[{"keyId":%d,"base64":"%s"}]}
                """.formatted(keyId, base64)));
    }

    private String sign(String signedContent, KeyPair keyPair) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(signedContent.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
    }

    private MockHttpServletRequest request(String queryString) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ads/rewards/callback/admob");
        request.setQueryString(queryString);
        for (String pair : queryString.split("&")) {
            String[] parts = pair.split("=", 2);
            request.addParameter(parts[0], parts.length == 2 ? parts[1] : "");
        }
        return request;
    }
}
