ALTER TABLE payment
    MODIFY COLUMN payment_id VARCHAR(512) NOT NULL;

ALTER TABLE alarm_deactivation_log
    MODIFY COLUMN payment_id VARCHAR(512) NULL;

ALTER TABLE alarm_delete_log
    MODIFY COLUMN payment_id VARCHAR(512) NULL;
