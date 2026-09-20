package akuma.whiplash.domains.member.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.fixture.MemberFixture;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
                    social_id VARCHAR(255) NOT NULL UNIQUE,
                    email VARCHAR(50) NOT NULL,
                    nickname VARCHAR(50) NOT NULL,
                    role VARCHAR(255) NOT NULL,
                    privacy_policy BOOLEAN NOT NULL,
                    push_notification_policy BOOLEAN NOT NULL,
                    privacy_agreed_at DATETIME NOT NULL,
                    push_agreed_at DATETIME NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL
                )
                """);
        }
    }

    @Nested
    @DisplayName("persist - 레거시 member 스키마 보정")
    class PersistTest {

        @Test
        @DisplayName("성공: v2 스키마 보정 후 소셜 회원을 저장하고 조회한다")
        void success() throws SQLException {
            // given
            applyLastLoginAtMigration();
            applyLastLoginAtMigration();
            applyMemberV2SchemaMigration();
            applyMemberV2SchemaMigration();
            applyMemberHardDeleteSchemaMigration();
            applyMemberHardDeleteSchemaMigration();
            MemberEntity member = MemberFixture.MEMBER_1.toEntity();

            // when
            insertMember(member);
            MemberEntity savedMember = findMember(member);

            // then
            assertThat(savedMember.getProvider()).isEqualTo(member.getProvider());
            assertThat(savedMember.getProviderUserId()).isEqualTo(member.getProviderUserId());
            assertThat(hasLastLoginAtColumn()).isTrue();
            assertThat(hasMemberV2Columns()).isTrue();
            assertThat(hasMemberProviderUniqueKey()).isTrue();
            assertThat(hasHardDeleteColumns()).isTrue();
        }
    }

    private void insertMember(MemberEntity member) throws SQLException {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO member (
                provider, provider_user_id, email, nickname, role, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, member.getProvider().name());
            statement.setString(2, member.getProviderUserId());
            statement.setString(3, member.getEmail());
            statement.setString(4, member.getNickname());
            statement.setString(5, member.getRole().name());
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.of(2026, 9, 20, 0, 0)));
            statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.of(2026, 9, 20, 0, 0)));
            statement.executeUpdate();
        }
    }

    private MemberEntity findMember(MemberEntity member) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                """
                    SELECT member
                    FROM MemberEntity member
                    WHERE member.provider = :provider
                      AND member.providerUserId = :providerUserId
                    """,
                MemberEntity.class
            )
                .setParameter("provider", member.getProvider())
                .setParameter("providerUserId", member.getProviderUserId())
                .getSingleResult();
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

    private void applyMemberV2SchemaMigration() throws SQLException {
        try (Connection connection = getConnection()) {
            ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("db/migration/V020__reconcile_member_v2_schema.sql")
            );
        }
    }

    private void applyMemberHardDeleteSchemaMigration() throws SQLException {
        try (Connection connection = getConnection()) {
            ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("db/migration/V021__reconcile_member_hard_delete_schema.sql")
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

    private boolean hasMemberV2Columns() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'member'
                  AND COLUMN_NAME IN ('provider', 'provider_user_id')
                """)) {
                resultSet.next();
                return resultSet.getInt(1) == 2;
            }
        }
    }

    private boolean hasHardDeleteColumns() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'member'
                  AND COLUMN_NAME IN ('status', 'deleted_at')
                """)) {
                resultSet.next();
                return resultSet.getInt(1) == 0;
            }
        }
    }

    private boolean hasMemberProviderUniqueKey() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'member'
                  AND INDEX_NAME = 'UK_MEMBER_PROVIDER'
                """)) {
                resultSet.next();
                return resultSet.getInt(1) == 2;
            }
        }
    }
}
