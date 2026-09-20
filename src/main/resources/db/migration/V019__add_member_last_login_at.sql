-- Reconcile pre-Flyway member schemas with MemberEntity.lastLoginAt.
-- V0 defines this column for new databases, but production was baselined at V12.

SET @schema_name = DATABASE();

SET @sql = IF(
    NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'member'
          AND COLUMN_NAME = 'last_login_at'
    ),
    'ALTER TABLE member ADD COLUMN last_login_at DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
