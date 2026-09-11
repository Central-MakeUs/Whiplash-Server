CREATE TABLE IF NOT EXISTS ad_session
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    ad_session_id            VARCHAR(64)  NOT NULL,
    member_id                BIGINT       NOT NULL,
    alarm_id                 BIGINT       NOT NULL,
    device_id                VARCHAR(255) NOT NULL,
    purpose                  VARCHAR(50)  NOT NULL,
    status                   VARCHAR(30)  NOT NULL,
    expires_at               DATETIME(6)  NOT NULL,
    verified_at              DATETIME(6),
    consumed_at              DATETIME(6),
    transaction_id           VARCHAR(128),
    ad_unit_id               VARCHAR(255),
    reward_amount            INT,
    reward_item              VARCHAR(100),
    raw_callback_received_at DATETIME(6),
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    CONSTRAINT uk_ad_session_ad_session_id UNIQUE (ad_session_id),
    CONSTRAINT uk_ad_session_transaction_id UNIQUE (transaction_id),
    KEY idx_ad_session_member_alarm_status (member_id, alarm_id, status),
    KEY idx_ad_session_expires_at (expires_at),
    KEY idx_ad_session_purpose_status (purpose, status),
    CONSTRAINT fk_ad_session_member
        FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_ad_session_alarm
        FOREIGN KEY (alarm_id) REFERENCES alarm (id)
);
