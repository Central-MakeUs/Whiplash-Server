-- ============================================================
-- V9: alarm revision 컬럼 제거
-- ============================================================
-- alarmRevision은 클라이언트 동기화 계약에서 제외한다.
-- 기존 DB와 fresh DB 모두에서 안전하게 실행되도록 조건부 DDL을 사용한다.

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE alarm DROP COLUMN revision',
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
