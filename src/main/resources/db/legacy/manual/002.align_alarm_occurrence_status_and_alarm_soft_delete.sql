-- ============================================================
-- V2: alarm_occurrence 상태 모델 정렬 + alarm soft delete 컬럼 추가
-- ============================================================
-- 이 스크립트는 기존 DB가 일부만 반영된 상태에서도 다시 실행할 수 있도록
-- INFORMATION_SCHEMA 기반 조건부 DDL을 사용한다.

SET @schema_name = DATABASE();

-- 1. alarm 테이블을 최신 엔티티/스키마 기준으로 정렬
--    구버전 DB는 time 컬럼명을 사용하고, 최신 코드는 alarm_time을 사용한다.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm'
              AND COLUMN_NAME = 'time'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm'
              AND COLUMN_NAME = 'alarm_time'
        ),
        'ALTER TABLE alarm CHANGE COLUMN `time` alarm_time TIME NOT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'' AFTER address',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm'
      AND COLUMN_NAME = 'status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm ADD COLUMN revision INT NOT NULL DEFAULT 1 AFTER status',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm'
      AND COLUMN_NAME = 'revision'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm ADD COLUMN next_scheduled_time DATETIME NULL AFTER revision',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm'
      AND COLUMN_NAME = 'next_scheduled_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm ADD COLUMN deleted_at DATETIME NULL AFTER updated_at',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm'
      AND COLUMN_NAME = 'deleted_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 구버전 alarm_occurrence.date -> occurrence_date 컬럼명 정렬
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_occurrence'
              AND COLUMN_NAME = 'date'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_occurrence'
              AND COLUMN_NAME = 'occurrence_date'
        ),
        'ALTER TABLE alarm_occurrence CHANGE COLUMN `date` occurrence_date DATE NOT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 구버전 alarm_occurrence.time -> occurrence_time 컬럼명 정렬
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_occurrence'
              AND COLUMN_NAME = 'time'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_occurrence'
              AND COLUMN_NAME = 'occurrence_time'
        ),
        'ALTER TABLE alarm_occurrence CHANGE COLUMN `time` occurrence_time TIME NOT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. scheduled_at 컬럼 보강
SET @has_scheduled_at = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'scheduled_at'
);

SET @has_occurrence_time = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'occurrence_time'
);

SET @has_legacy_time = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'time'
);

SET @sql = IF(
    @has_scheduled_at = 0 AND @has_occurrence_time > 0,
    'ALTER TABLE alarm_occurrence ADD COLUMN scheduled_at DATETIME NULL AFTER occurrence_time',
    IF(
        @has_scheduled_at = 0 AND @has_legacy_time > 0,
        'ALTER TABLE alarm_occurrence ADD COLUMN scheduled_at DATETIME NULL AFTER `time`',
        IF(
            @has_scheduled_at = 0,
            'ALTER TABLE alarm_occurrence ADD COLUMN scheduled_at DATETIME NULL',
            'SELECT 1'
        )
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_scheduled_at = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'scheduled_at'
);

SET @has_occurrence_date = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'occurrence_date'
);

SET @has_occurrence_time = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'occurrence_time'
);

SET @has_legacy_date = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'date'
);

SET @has_legacy_time = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'time'
);

SET @sql = IF(
    @has_scheduled_at > 0 AND @has_occurrence_date > 0 AND @has_occurrence_time > 0,
    'UPDATE alarm_occurrence
     SET scheduled_at = TIMESTAMP(occurrence_date, occurrence_time)
     WHERE scheduled_at IS NULL',
    IF(
        @has_scheduled_at > 0 AND @has_legacy_date > 0 AND @has_legacy_time > 0,
        'UPDATE alarm_occurrence
         SET scheduled_at = TIMESTAMP(`date`, `time`)
         WHERE scheduled_at IS NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_scheduled_at > 0,
    'UPDATE alarm_occurrence
     SET scheduled_at = COALESCE(scheduled_at, updated_at, created_at, NOW())
     WHERE scheduled_at IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_scheduled_at > 0,
    'ALTER TABLE alarm_occurrence MODIFY COLUMN scheduled_at DATETIME NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 최신 스키마의 회차 상태 컬럼 추가
--    기존 DB에는 deactivate_type만 있고 status가 없을 수 있으므로 데이터 이관 전에 먼저 추가한다.
SET @has_status = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'status'
);

SET @has_scheduled_at = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'scheduled_at'
);

SET @sql = IF(
    @has_status = 0 AND @has_scheduled_at > 0,
    'ALTER TABLE alarm_occurrence ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''SCHEDULED'' AFTER scheduled_at',
    IF(
        @has_status = 0 AND @has_occurrence_time > 0,
        'ALTER TABLE alarm_occurrence ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''SCHEDULED'' AFTER occurrence_time',
        IF(
            @has_status = 0,
            'ALTER TABLE alarm_occurrence ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''SCHEDULED''',
            'SELECT 1'
        )
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 기존 deactivate_type 데이터를 status로 이관
--    - CHECKIN: 위치 인증으로 처리된 회차
--    - OFF: 기존 수동/유료 끄기 의미를 새 스키마의 PAYMENT 처리 상태로 이관
--    - NONE: 아직 처리되지 않은 회차이므로 기존 status 유지
SET @has_deactivate_type = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'deactivate_type'
);

SET @sql = IF(
    @has_deactivate_type > 0,
    'UPDATE alarm_occurrence
     SET status = CASE
         WHEN deactivate_type = ''CHECKIN'' THEN ''CHECKIN''
         WHEN deactivate_type = ''OFF'' THEN ''PAYMENT''
         ELSE status
     END
     WHERE deactivate_type IN (''CHECKIN'', ''OFF'')',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. 처리 완료 시각 보정
SET @sql = IF(
    @has_deactivate_type > 0,
    'UPDATE alarm_occurrence
     SET deactivated_at = updated_at
     WHERE deactivate_type IN (''CHECKIN'', ''OFF'')
       AND deactivated_at IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8. 스키마 설계서 기준으로 회차 처리 상태는 status만 사용하므로 deactivate_type 제거
SET @sql = IF(
    @has_deactivate_type > 0,
    'ALTER TABLE alarm_occurrence DROP COLUMN deactivate_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
