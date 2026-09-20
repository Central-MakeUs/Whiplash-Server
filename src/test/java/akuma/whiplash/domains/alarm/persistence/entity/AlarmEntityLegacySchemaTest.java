package akuma.whiplash.domains.alarm.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
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
@DisplayName("AlarmEntity Legacy Schema Test")
class AlarmEntityLegacySchemaTest {

    @Container
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(
        DockerImageName.parse("mysql:8.0.33")
    )
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @BeforeEach
    void setUpLegacyAlarmTable() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS alarm");
            statement.execute("DROP TABLE IF EXISTS member");
            statement.execute("CREATE TABLE member (id BIGINT AUTO_INCREMENT PRIMARY KEY)");
            statement.execute("INSERT INTO member VALUES (1)");
            statement.execute("""
                CREATE TABLE alarm (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    member_id BIGINT NOT NULL,
                    alarm_purpose VARCHAR(50) NOT NULL,
                    time TIME NOT NULL,
                    repeat_days TEXT NOT NULL,
                    sound_type VARCHAR(20) NOT NULL,
                    latitude DOUBLE NULL,
                    longitude DOUBLE NULL,
                    address VARCHAR(255) NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    location_source VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_PLACE',
                    google_place_id VARCHAR(255) NULL,
                    location_cached_at DATETIME(6) NULL
                )
                """);
        }
    }

    @Nested
    @DisplayName("reconcile - 레거시 alarm 스키마 보정")
    class ReconcileTest {

        @Test
        @DisplayName("성공: 현재 엔티티가 요구하는 컬럼을 추가하고 재실행해도 안전하다")
        void success() throws SQLException {
            // given
            applyAlarmSchemaMigration();
            applyAlarmSchemaMigration();

            // when
            Map<String, String> columns = getColumns();

            // then
            assertThat(columns)
                .containsEntry("alarm_time", "time")
                .containsEntry("status", "varchar(20)")
                .containsEntry("next_scheduled_time", "datetime(6)")
                .doesNotContainKey("time");
            assertThat(getColumnDefault("status")).isEqualTo("ACTIVE");
        }
    }

    private Connection getConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(
            MYSQL_CONTAINER.getJdbcUrl(),
            MYSQL_CONTAINER.getUsername(),
            MYSQL_CONTAINER.getPassword()
        );
    }

    private void applyAlarmSchemaMigration() throws SQLException {
        try (Connection connection = getConnection()) {
            ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("db/migration/V022__reconcile_legacy_alarm_schema.sql")
            );
        }
    }

    private Map<String, String> getColumns() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("""
                SELECT COLUMN_NAME, COLUMN_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'alarm'
                """)) {
                Map<String, String> columns = new java.util.HashMap<>();
                while (resultSet.next()) {
                    columns.put(resultSet.getString("COLUMN_NAME"), resultSet.getString("COLUMN_TYPE"));
                }
                return columns;
            }
        }
    }

    private String getColumnDefault(String columnName) throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("""
                SELECT COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'alarm'
                  AND COLUMN_NAME = '%s'
                """.formatted(columnName))) {
                resultSet.next();
                return resultSet.getString("COLUMN_DEFAULT");
            }
        }
    }
}
