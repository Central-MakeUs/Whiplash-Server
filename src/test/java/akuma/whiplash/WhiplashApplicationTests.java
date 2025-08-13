package akuma.whiplash;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.config.RedisContainerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@ContextConfiguration(initializers = {
	RedisContainerInitializer.class
})
class WhiplashApplicationTests {

	@Test
	void contextLoads() {
	}

}
