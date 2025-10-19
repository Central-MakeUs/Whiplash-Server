package akuma.whiplash.infrastructure.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void setValues(String key, String value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    @Override
    public Optional<String> getValues(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public List<String> getKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? List.of() : new ArrayList<>(keys);
    }
}