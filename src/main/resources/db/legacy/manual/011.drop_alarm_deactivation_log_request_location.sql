-- ============================================================
-- V11: 알람 비활성화 로그의 요청 현재 위치 좌표 컬럼 제거
-- ============================================================

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_deactivation_log'
              AND COLUMN_NAME = 'request_latitude'
        ),
        'ALTER TABLE alarm_deactivation_log DROP COLUMN request_latitude',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_deactivation_log'
              AND COLUMN_NAME = 'request_longitude'
        ),
        'ALTER TABLE alarm_deactivation_log DROP COLUMN request_longitude',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
