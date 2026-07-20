package akuma.whiplash.infrastructure.redis;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final RedisScript<Long> REMOVE_DEVICE_SCRIPT = RedisScript.of("""
        local oldToken = redis.call('GET', KEYS[1])
        if not oldToken or oldToken == '' then
            redis.call('DEL', KEYS[1])
            return 0
        end
        redis.call('SREM', KEYS[2], oldToken)
        redis.call('DEL', 'fcm:token:' .. oldToken .. ':device')
        redis.call('DEL', KEYS[1])
        return 1
        """, Long.class);

    private static final RedisScript<Long> UPSERT_SCRIPT = RedisScript.of("""
        local oldToken = redis.call('GET', KEYS[1])
        if oldToken and oldToken ~= '' and oldToken ~= ARGV[1] then
            redis.call('SREM', KEYS[2], oldToken)
            redis.call('DEL', 'fcm:token:' .. oldToken .. ':device')
        end
        redis.call('SET', KEYS[1], ARGV[1])
        redis.call('SET', 'fcm:token:' .. ARGV[1] .. ':device', ARGV[2])
        redis.call('SADD', KEYS[2], ARGV[1])
        return 1
        """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public Set<String> getFcmTokens(Long memberId) {
        String key = "fcm:member:" + memberId;
        return Optional.ofNullable(redisTemplate.opsForSet().members(key)).orElse(Set.of());
    }

    public void removeInvalidToken(Long memberId, String token) {
        redisTemplate.opsForSet().remove("fcm:member:" + memberId, token);
    }


    // ===== 신규: fcmToken 등록/갱신(원자적) =====

    /**
     * deviceId에 새 fcmToken을 등록(upsert).
     * - 다른 토큰으로 교체되면 이전 토큰을 member Set에서 제거하고 매핑 정리
     * - 같은 토큰 재등록이면 idempotent하게 처리됨
     *
     * Lua 스크립트로 GET-SREM-DEL-SET-SADD를 단일 원자적 명령으로 실행한다.
     */
    public void upsertFcmToken(Long memberId, String deviceId, String newToken) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be null or blank");
        }
        if (newToken == null || newToken.isBlank()) {
            throw new IllegalArgumentException("newToken must not be null or blank");
        }

        redisTemplate.execute(
            UPSERT_SCRIPT,
            List.of(keyDeviceToToken(deviceId), keyMemberTokens(memberId)),
            newToken,
            deviceId
        );
    }

    // ===== 선택: 특정 디바이스 로그아웃 시 정리 =====

    /**
     * 특정 deviceId에 매핑된 토큰을 제거하고, member Set에서도 제거.
     */
    public void removeFcmTokenForDevice(Long memberId, String deviceId) {
        redisTemplate.execute(
            REMOVE_DEVICE_SCRIPT,
            List.of(keyDeviceToToken(deviceId), keyMemberTokens(memberId))
        );
    }

    // ===== 선택: 조회 유틸 =====

    public String getFcmTokenByDevice(String deviceId) {
        return redisTemplate.opsForValue().get(keyDeviceToToken(deviceId));
    }

    public String getDeviceIdByFcmToken(String token) {
        return redisTemplate.opsForValue().get(keyTokenToDevice(token));
    }

    // ===== Key builders =====

    private String keyMemberTokens(Long memberId) {
        return "fcm:member:" + memberId;
    }

    private String keyDeviceToToken(String deviceId) {
        return "fcm:device:" + deviceId + ":token";
    }

    private String keyTokenToDevice(String token) {
        return "fcm:token:" + token + ":device";
    }
}
