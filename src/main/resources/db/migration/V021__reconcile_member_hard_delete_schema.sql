-- Member withdrawal is a hard delete, so soft-delete/status columns are not part of the member schema.

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'member' AND COLUMN_NAME = 'status'
    ),
    'ALTER TABLE member DROP COLUMN status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'member' AND COLUMN_NAME = 'deleted_at'
    ),
    'ALTER TABLE member DROP COLUMN deleted_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE member MODIFY COLUMN last_login_at DATETIME(6) NULL;
