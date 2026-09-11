-- V10: ringing alarm DB 원장 조회 보조 인덱스 추가
-- Redis alarm:ringing 보조 상태를 제거하고 alarm_occurrence를 기준으로 울림 푸시 대상을 조회한다.

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE alarm_occurrence ADD INDEX idx_alarm_occurrence_ringing_query (status, alarm_ringing, alarm_id)',
        'SELECT 1'
    )
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'alarm_occurrence'
      AND INDEX_NAME = 'idx_alarm_occurrence_ringing_query'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
