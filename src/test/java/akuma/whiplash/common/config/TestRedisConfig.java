package akuma.whiplash.common.config;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestRedisConfig {

    private static final String REDIS_IMAGE = "redis:7.2.0";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = "test-password";

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
        .withExposedPorts(REDIS_PORT)
        .withCommand("redis-server", "--requirepass", REDIS_PASSWORD);

    static {
        redisContainer.start();

        String host = redisContainer.getHost();
        Integer port = redisContainer.getMappedPort(REDIS_PORT);

        System.setProperty("spring.data.redis.host", host);
        System.setProperty("spring.data.redis.port", port.toString());
        System.setProperty("spring.data.redis.password", REDIS_PASSWORD);
    }


    @Bean
    public GenericContainer<?> redisContainer() {
        return redisContainer;
    }

    @PreDestroy
    public void stopRedis() {
        if (redisContainer != null && redisContainer.isRunning()) {
            redisContainer.stop();
        }
    }
}