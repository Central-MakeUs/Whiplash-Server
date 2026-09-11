-- Reconcile pre-Flyway alarm_occurrence variants with AlarmOccurrenceEntity.
-- The legacy production baseline used `date`, `time`, and `deactivate_type`.

SET @schema_name = DATABASE();

-- Keep a standalone alarm_id index before replacing the legacy unique key.
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm_occurrence ADD INDEX idx_alarm_occurrence_alarm_id (alarm_id)',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'idx_alarm_occurrence_alarm_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) > 0, 'ALTER TABLE alarm_occurrence DROP INDEX uq_alarm_date_member', 'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'uq_alarm_date_member'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) > 0, 'ALTER TABLE alarm_occurrence DROP INDEX uk_alarm_date', 'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'uk_alarm_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'date'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'occurrence_date'
    ),
    'ALTER TABLE alarm_occurrence CHANGE COLUMN `date` occurrence_date DATE NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'time'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'occurrence_time'
    ),
    'ALTER TABLE alarm_occurrence CHANGE COLUMN `time` occurrence_time TIME NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'scheduled_at'
    ),
    'ALTER TABLE alarm_occurrence ADD COLUMN scheduled_at DATETIME NULL AFTER occurrence_time',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE alarm_occurrence
SET scheduled_at = TIMESTAMP(occurrence_date, occurrence_time)
WHERE scheduled_at IS NULL;

ALTER TABLE alarm_occurrence MODIFY COLUMN scheduled_at DATETIME NOT NULL;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'status'
    ),
    'ALTER TABLE alarm_occurrence ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''SCHEDULED'' AFTER scheduled_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_deactivate_type = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'alarm_occurrence' AND COLUMN_NAME = 'deactivate_type'
);

SET @sql = IF(
    @has_deactivate_type > 0,
    'UPDATE alarm_occurrence
     SET status = CASE
         WHEN deactivate_type = ''CHECKIN'' THEN ''CHECKIN''
         WHEN deactivate_type IN (''OFF'', ''PAYMENT'') THEN ''PAYMENT''
         ELSE status
     END
     WHERE deactivate_type IN (''CHECKIN'', ''OFF'', ''PAYMENT'')',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_deactivate_type > 0,
    'UPDATE alarm_occurrence
     SET deactivated_at = COALESCE(deactivated_at, updated_at)
     WHERE deactivate_type IN (''CHECKIN'', ''OFF'', ''PAYMENT'')',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_deactivate_type > 0,
    'ALTER TABLE alarm_occurrence DROP COLUMN deactivate_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE alarm_occurrence ADD UNIQUE KEY uk_alarm_date (alarm_id, occurrence_date)', 'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'uk_alarm_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE alarm_occurrence ADD INDEX idx_alarm_occurrence_ringing_query (status, alarm_ringing, alarm_id)', 'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'idx_alarm_occurrence_ringing_query'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
