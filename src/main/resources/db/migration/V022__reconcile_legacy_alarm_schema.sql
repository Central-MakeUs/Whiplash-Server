-- Reconcile the pre-Flyway production alarm schema with AlarmEntity.
-- The legacy production baseline uses `time` and lacks scheduling columns.

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm' AND COLUMN_NAME = 'time'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm' AND COLUMN_NAME = 'alarm_time'
    ),
    'ALTER TABLE alarm CHANGE COLUMN `time` alarm_time TIME NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm' AND COLUMN_NAME = 'status'
    ),
    'ALTER TABLE alarm ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'' AFTER address',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm' AND COLUMN_NAME = 'next_scheduled_time'
    ),
    'ALTER TABLE alarm ADD COLUMN next_scheduled_time DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
