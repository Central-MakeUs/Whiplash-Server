package akuma.whiplash.infrastructure.redis;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

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
     * - 같은 토큰 재등록이면 idempotent하게 Set/매핑 보강만 함
     * - 다른 토큰으로 교체되면 이전 토큰을 member Set에서 제거하고 매핑 정리
     *
     * 참고: MULTI/EXEC 트랜잭션을 쓰므로 RedisTemplate에 transactionSupport=true 필요.
     */
    public void upsertFcmToken(Long memberId, String deviceId, String newToken) {
        String deviceKey = keyDeviceToToken(deviceId);
        String memberSetKey = keyMemberTokens(memberId);
        String newTokenMapKey = keyTokenToDevice(newToken);

        String oldToken = redisTemplate.opsForValue().get(deviceKey);

        // 1) 동일 토큰 재등록: idempotent 보강
        if (Objects.equals(oldToken, newToken)) {
            redisTemplate.opsForSet().add(memberSetKey, newToken);
            redisTemplate.opsForValue().set(newTokenMapKey, deviceId);
            return;
        }

        // 2) 토큰 교체: 원자적 멀티 실행
        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                // 이전 토큰 정리
                if (oldToken != null && !oldToken.isBlank()) {
                    operations.opsForSet().remove(memberSetKey, oldToken);
                    operations.delete(keyTokenToDevice(oldToken));
                }
                // 신규 매핑/등록
                operations.opsForValue().set(deviceKey, newToken);
                operations.opsForValue().set(newTokenMapKey, deviceId);
                operations.opsForSet().add(memberSetKey, newToken);
                operations.exec();
                return null;
            }
        });
    }

    // ===== 선택: 특정 디바이스 로그아웃 시 정리 =====

    /**
     * 특정 deviceId에 매핑된 토큰을 제거하고, member Set에서도 제거.
     */
    public void removeFcmTokenForDevice(Long memberId, String deviceId) {
        String deviceKey = keyDeviceToToken(deviceId);
        String memberSetKey = keyMemberTokens(memberId);

        String oldToken = redisTemplate.opsForValue().get(deviceKey);
        if (oldToken == null || oldToken.isBlank()) {
            redisTemplate.delete(deviceKey);
            return;
        }

        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForSet().remove(memberSetKey, oldToken);
                operations.delete(keyTokenToDevice(oldToken));
                operations.delete(deviceKey);
                operations.exec();
                return null;
            }
        });
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
