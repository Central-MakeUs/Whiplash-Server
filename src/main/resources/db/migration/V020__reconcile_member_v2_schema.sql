-- Reconcile the pre-v2 member schema with MemberEntity.
-- Production has no member rows, so the removed legacy required columns contain no member data.

SET @schema_name = DATABASE();

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'member' AND COLUMN_NAME = 'provider'
    ),
    'ALTER TABLE member ADD COLUMN provider VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'member' AND COLUMN_NAME = 'provider_user_id'
    ),
    'ALTER TABLE member ADD COLUMN provider_user_id VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'member' AND COLUMN_NAME = 'status'
    ),
    'ALTER TABLE member ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_social_id = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'member' AND COLUMN_NAME = 'social_id'
);

SET @sql = IF(
    @has_social_id > 0,
    'UPDATE member
     SET provider = COALESCE(provider, SUBSTRING_INDEX(social_id, ''_'', 1)),
         provider_user_id = COALESCE(provider_user_id, SUBSTRING(social_id, LOCATE(''_'', social_id) + 1))
     WHERE social_id REGEXP ''^(GOOGLE|APPLE|KAKAO)_.+''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @invalid_member_count = (
    SELECT COUNT(*) FROM member
    WHERE provider IS NULL OR provider_user_id IS NULL
);

SET @sql = IF(
    @invalid_member_count = 0,
    'SELECT 1',
    'SELECT * FROM member_v2_schema_migration_requires_manual_data_repair'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE member
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    MODIFY COLUMN provider VARCHAR(20) NOT NULL,
    MODIFY COLUMN provider_user_id VARCHAR(100) NOT NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL,
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN nickname VARCHAR(50) NULL,
    MODIFY COLUMN role VARCHAR(255) NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL,
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL;

SET @sql = IF(
    @has_social_id > 0,
    'ALTER TABLE member
        DROP COLUMN social_id,
        DROP COLUMN privacy_policy,
        DROP COLUMN push_notification_policy,
        DROP COLUMN privacy_agreed_at,
        DROP COLUMN push_agreed_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'member'
          AND INDEX_NAME = 'UK_MEMBER_PROVIDER'
    ),
    'ALTER TABLE member ADD CONSTRAINT UK_MEMBER_PROVIDER UNIQUE (provider, provider_user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
