package akuma.whiplash.infrastructure.redis;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface RedisRepository {
    void setValues(String key, String value, Duration timeout);
    Optional<String> getValues(String key);
    void deleteValues(String key);
    List<String> getKeys(String pattern);
}
