package akuma.whiplash.domains.member.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DisplayName("MemberEntity Legacy Schema Test")
class MemberEntityLegacySchemaTest {

    @Container
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(
        DockerImageName.parse("mysql:8.0.33")
    )
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUpSessionFactory() {
        sessionFactory = new Configuration()
            .addAnnotatedClass(MemberEntity.class)
            .setProperty(AvailableSettings.JAKARTA_JDBC_URL, MYSQL_CONTAINER.getJdbcUrl())
            .setProperty(AvailableSettings.JAKARTA_JDBC_USER, MYSQL_CONTAINER.getUsername())
            .setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, MYSQL_CONTAINER.getPassword())
            .setProperty(
                AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
            )
            .setProperty(AvailableSettings.HBM2DDL_AUTO, "none")
            .buildSessionFactory();
    }

    @AfterAll
    static void closeSessionFactory() {
        sessionFactory.close();
    }

    @BeforeEach
    void setUpLegacyMemberTable() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS member");
            statement.execute("""
                CREATE TABLE member (
                    id BIGINT PRIMARY KEY,
                    provider VARCHAR(20) NOT NULL,
                    provider_user_id VARCHAR(100) NOT NULL,
                    email VARCHAR(255) NULL,
                    nickname VARCHAR(50) NULL,
                    status VARCHAR(20) NOT NULL,
                    role VARCHAR(255) NULL,
                    last_login_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL
                )
                """);
            statement.execute("""
                INSERT INTO member (
                    id, provider, provider_user_id, email, nickname, status, role,
                    last_login_at, created_at, updated_at
                ) VALUES (
                    1, 'KAKAO', 'legacy-kakao-id', 'legacy@example.com', 'legacy', 'ACTIVE', 'USER',
                    NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        }
    }

    @Nested
    @DisplayName("find - legacy member 조회")
    class FindTest {

        @Test
        @DisplayName("성공: deleted_at 없는 레거시 스키마에서도 회원을 조회한다")
        void success() {
            // given
            Long memberId = 1L;

            // when
            MemberEntity member = findMember(memberId);

            // then
            assertThat(member).isNotNull();
            assertThat(member.getEmail()).isEqualTo("legacy@example.com");
        }
    }

    private MemberEntity findMember(Long memberId) {
        try (Session session = sessionFactory.openSession()) {
            return session.find(MemberEntity.class, memberId);
        }
    }

    private Connection getConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(
            MYSQL_CONTAINER.getJdbcUrl(),
            MYSQL_CONTAINER.getUsername(),
            MYSQL_CONTAINER.getPassword()
        );
    }
}
