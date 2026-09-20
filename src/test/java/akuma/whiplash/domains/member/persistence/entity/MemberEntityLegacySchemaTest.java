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
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL
                )
                """);
            statement.execute("""
                INSERT INTO member (
                    id, provider, provider_user_id, email, nickname, status, role,
                    created_at, updated_at
                ) VALUES (
                    1, 'KAKAO', 'legacy-kakao-id', 'legacy@example.com', 'legacy', 'ACTIVE', 'USER',
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        }
    }

    @Nested
    @DisplayName("find - legacy member 조회")
    class FindTest {

        @Test
        @DisplayName("성공: last_login_at 없는 레거시 스키마를 보정한 뒤 회원을 조회한다")
        void success() throws SQLException {
            // given
            Long memberId = 1L;
            applyLastLoginAtMigration();
            applyLastLoginAtMigration();

            // when
            MemberEntity member = findMember(memberId);

            // then
            assertThat(member).isNotNull();
            assertThat(member.getEmail()).isEqualTo("legacy@example.com");
            assertThat(hasLastLoginAtColumn()).isTrue();
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

    private void applyLastLoginAtMigration() throws SQLException {
        try (Connection connection = getConnection()) {
            ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("db/migration/V019__add_member_last_login_at.sql")
            );
        }
    }

    private boolean hasLastLoginAtColumn() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'member'
                  AND COLUMN_NAME = 'last_login_at'
                """)) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
