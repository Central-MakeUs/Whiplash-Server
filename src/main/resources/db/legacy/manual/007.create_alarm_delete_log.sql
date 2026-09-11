ALTER TABLE alarm
    ADD COLUMN deleted_at DATETIME(6);

CREATE TABLE IF NOT EXISTS alarm_delete_log
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_id       BIGINT       NOT NULL,
    member_id      BIGINT       NOT NULL,
    delete_type    VARCHAR(20)  NOT NULL,
    reason         VARCHAR(2000) NOT NULL,
    payment_id     VARCHAR(100),
    ad_proof_token VARCHAR(255),
    requested_at   DATETIME(6),
    deleted_at     DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    KEY idx_alarm_delete_log_alarm_id (alarm_id),
    KEY idx_alarm_delete_log_member_id (member_id),
    CONSTRAINT fk_alarm_delete_log_alarm
        FOREIGN KEY (alarm_id) REFERENCES alarm (id),
    CONSTRAINT fk_alarm_delete_log_member
        FOREIGN KEY (member_id) REFERENCES member (id)
);
