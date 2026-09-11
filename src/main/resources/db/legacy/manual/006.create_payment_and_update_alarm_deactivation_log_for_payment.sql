-- ============================================================
-- V5: 결제 기반 알람 비활성화 지원
-- ============================================================

SET @schema_name = DATABASE();

CREATE TABLE IF NOT EXISTS payment
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id    BIGINT       NOT NULL,
    alarm_id     BIGINT       NOT NULL,
    payment_id   VARCHAR(100) NOT NULL,
    payment_type VARCHAR(20)  NOT NULL,
    amount       INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_payment_payment_id (payment_id),
    KEY idx_payment_member_id (member_id),
    KEY idx_payment_alarm_id (alarm_id),
    CONSTRAINT fk_payment_member
        FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_payment_alarm
        FOREIGN KEY (alarm_id) REFERENCES alarm (id)
);

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'alarm_deactivation_log'
              AND COLUMN_NAME = 'fail_reason'
              AND CHARACTER_MAXIMUM_LENGTH < 500
        ),
        'ALTER TABLE alarm_deactivation_log MODIFY COLUMN fail_reason VARCHAR(500) NOT NULL DEFAULT ''''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
