-- ============================================================
-- V8: alarm_occurrence 스키마를 현재 애플리케이션 코드 기준으로 보정
-- ============================================================
-- 배경:
-- - 구버전 QA DB는 alarm_occurrence.date, time, deactivate_type 컬럼을 사용한다.
-- - 현재 코드는 occurrence_date, occurrence_time, scheduled_at, status 컬럼을 조회/저장한다.
-- - qa/prod profile은 spring.jpa.hibernate.ddl-auto=none 이므로 DB 스키마를 직접 맞춰야 한다.
--
-- 이 스크립트는 INFORMATION_SCHEMA 기반 조건부 DDL을 사용하여
-- 일부 단계가 이미 반영된 DB에서도 재실행 가능하도록 작성했다.

SET @schema_name = DATABASE();

-- 1. alarm_id FK를 받쳐줄 단독 인덱스 보강
--    구버전 DB에서는 uq_alarm_date_member(alarm_id, date)가 FK 보조 인덱스 역할도 한다.
--    이 상태에서 유니크 인덱스를 바로 제거하면 MySQL 1553 에러가 발생한다.
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

-- 2. 기존 유니크 인덱스 제거
--    date 컬럼명을 occurrence_date로 바꾸기 전후 어떤 상태에서도 안전하게 처리한다.
SET @sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE alarm_occurrence DROP INDEX uq_alarm_date_member',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'uq_alarm_date_member'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE alarm_occurrence DROP INDEX uk_alarm_date',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'uk_alarm_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 구버전 컬럼명 정렬: date -> occurrence_date
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

-- 4. 구버전 컬럼명 정렬: time -> occurrence_time
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

-- 5. scheduled_at 컬럼 추가 및 데이터 보정
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

SET @sql = IF(
    @has_scheduled_at = 0 AND @has_occurrence_time > 0,
    'ALTER TABLE alarm_occurrence ADD COLUMN scheduled_at DATETIME NULL AFTER occurrence_time',
    IF(
        @has_scheduled_at = 0,
        'ALTER TABLE alarm_occurrence ADD COLUMN scheduled_at DATETIME NULL',
        'SELECT 1'
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

SET @sql = IF(
    @has_scheduled_at > 0 AND @has_occurrence_date > 0 AND @has_occurrence_time > 0,
    'UPDATE alarm_occurrence
     SET scheduled_at = TIMESTAMP(occurrence_date, occurrence_time)
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

-- 6. status 컬럼 추가 및 기존 deactivate_type 값 이관
SET @has_status = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'status'
);

SET @sql = IF(
    @has_status = 0,
    'ALTER TABLE alarm_occurrence ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''SCHEDULED'' AFTER scheduled_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

-- 7. 현재 AlarmOccurrenceEntity는 deactivate_type을 더 이상 insert하지 않는다.
--    구버전 NOT NULL 컬럼이 남아 있으면 신규 회차 저장이 실패하므로 제거한다.
SET @has_deactivate_type = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND COLUMN_NAME = 'deactivate_type'
);

SET @sql = IF(
    @has_deactivate_type > 0,
    'ALTER TABLE alarm_occurrence DROP COLUMN deactivate_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8. 현재 엔티티의 유니크 제약 복원
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm_occurrence ADD UNIQUE KEY uk_alarm_date (alarm_id, occurrence_date)',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'uk_alarm_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
