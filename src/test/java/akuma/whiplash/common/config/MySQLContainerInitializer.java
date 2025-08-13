package akuma.whiplash.common.config;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class MySQLContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    // MySQL 이미지와 버전 지정
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.33");

    // Testcontainers MySQL 컨테이너 생성
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL_CONTAINER.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        // JDBC URL에 rewriteBatchedStatements 옵션 추가
        String jdbcUrl = MYSQL_CONTAINER.getJdbcUrl() + "?rewriteBatchedStatements=true";

        // 추가 모니터링 옵션이 필요하다면 여기에 붙일 수 있음
        // jdbcUrl += "&profileSQL=true&logger=Slf4JLogger&maxQuerySizeToLog=2147483647";

        TestPropertyValues.of(
                "spring.datasource.url=" + jdbcUrl,
                "spring.datasource.username=" + MYSQL_CONTAINER.getUsername(),
                "spring.datasource.password=" + MYSQL_CONTAINER.getPassword(),
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
        ).applyTo(ctx.getEnvironment());
    }
}