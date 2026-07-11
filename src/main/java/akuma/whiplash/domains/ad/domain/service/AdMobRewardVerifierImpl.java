package akuma.whiplash.domains.ad.domain.service;

import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import akuma.whiplash.domains.ad.exception.AdErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AdMobRewardVerifierImpl implements AdMobRewardVerifier {

    private static final String SIGNATURE_PARAM = "signature=";
    private static final String KEY_ID_PARAM = "&key_id=";
    private static final Duration KEY_CACHE_TTL = Duration.ofHours(24);
    private static final Duration KEY_FETCH_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final String publicKeyUrl;
    private volatile Map<Long, PublicKey> cachedPublicKeys = new ConcurrentHashMap<>();
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    public AdMobRewardVerifierImpl(
        WebClient.Builder webClientBuilder,
        @Value("${admob.ssv.public-key-url:https://www.gstatic.com/admob/reward/verifier-keys.json}")
        String publicKeyUrl
    ) {
        this.webClient = webClientBuilder.build();
        this.publicKeyUrl = publicKeyUrl;
    }

    @Override
    public AdMobRewardCallback verify(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }

        String signedContent = extractSignedContent(queryString);
        String signature = extractSignature(queryString);
        long keyId = extractKeyId(queryString);

        verifySignature(signedContent, signature, keyId);

        String customData = request.getParameter("custom_data");
        String transactionId = request.getParameter("transaction_id");
        if (customData == null || customData.isBlank() || transactionId == null || transactionId.isBlank()) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }

        return new AdMobRewardCallback(
            customData,
            transactionId,
            request.getParameter("ad_unit"),
            parseInteger(request.getParameter("reward_amount")),
            request.getParameter("reward_item")
        );
    }

    private String extractSignedContent(String queryString) {
        int signatureIndex = queryString.indexOf("&" + SIGNATURE_PARAM);
        if (signatureIndex < 0) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
        return queryString.substring(0, signatureIndex);
    }

    private String extractSignature(String queryString) {
        int signatureIndex = queryString.indexOf("&" + SIGNATURE_PARAM);
        int keyIdIndex = queryString.indexOf(KEY_ID_PARAM);
        if (signatureIndex < 0 || keyIdIndex < 0 || keyIdIndex <= signatureIndex) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
        return queryString.substring(signatureIndex + SIGNATURE_PARAM.length() + 1, keyIdIndex);
    }

    private long extractKeyId(String queryString) {
        int keyIdIndex = queryString.indexOf(KEY_ID_PARAM);
        if (keyIdIndex < 0) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
        try {
            return Long.parseLong(queryString.substring(keyIdIndex + KEY_ID_PARAM.length()));
        } catch (NumberFormatException e) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
    }

    private void verifySignature(String signedContent, String encodedSignature, long keyId) {
        try {
            PublicKey publicKey = getPublicKeys().get(keyId);
            if (publicKey == null) {
                throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
            }

            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(signedContent.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(decodeUrlSafeBase64(encodedSignature))) {
                throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
            }
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
    }

    private Map<Long, PublicKey> getPublicKeys() {
        Instant now = Instant.now();
        if (now.isBefore(cacheExpiresAt) && !cachedPublicKeys.isEmpty()) {
            return cachedPublicKeys;
        }

        synchronized (this) {
            if (now.isBefore(cacheExpiresAt) && !cachedPublicKeys.isEmpty()) {
                return cachedPublicKeys;
            }
            AdMobPublicKeysResponse response = webClient.get()
                .uri(publicKeyUrl)
                .retrieve()
                .bodyToMono(AdMobPublicKeysResponse.class)
                .block(KEY_FETCH_TIMEOUT);
            if (response == null || response.keys() == null || response.keys().isEmpty()) {
                throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
            }

            cachedPublicKeys = response.keys().stream()
                .filter(key -> key.keyId() != null && key.base64() != null)
                .collect(Collectors.toConcurrentMap(
                    AdMobPublicKey::keyId,
                    this::toPublicKey
                ));
            cacheExpiresAt = now.plus(KEY_CACHE_TTL);
            return cachedPublicKeys;
        }
    }

    private PublicKey toPublicKey(AdMobPublicKey key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(key.base64());
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
    }

    private byte[] decodeUrlSafeBase64(String value) {
        String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
        int padding = (4 - decoded.length() % 4) % 4;
        return Base64.getUrlDecoder().decode(decoded + "=".repeat(padding));
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ApplicationException.from(AdErrorCode.INVALID_ADMOB_CALLBACK);
        }
    }

    private record AdMobPublicKeysResponse(List<AdMobPublicKey> keys) {
    }

    private record AdMobPublicKey(Long keyId, String base64) {
    }
}
