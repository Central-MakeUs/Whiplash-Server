-- ============================================================
-- V2: 위치 인증 기반 알람 비활성화 로그 테이블 추가
-- ============================================================

CREATE TABLE IF NOT EXISTS alarm_deactivation_log
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    alarm_occurrence_id BIGINT       NOT NULL,
    member_id           BIGINT       NOT NULL,
    payment_id          VARCHAR(100) NULL,
    deactivate_type     VARCHAR(20)  NOT NULL,
    request_latitude    DOUBLE       NULL,
    request_longitude   DOUBLE       NULL,
    request_device_id   VARCHAR(100) NOT NULL,
    requested_at        DATETIME(6)  NOT NULL,
    processed_at        DATETIME(6)  NOT NULL,
    result              VARCHAR(20)  NOT NULL,
    fail_reason         VARCHAR(100) NOT NULL DEFAULT '',
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    KEY idx_alarm_deactivation_log_occurrence_id (alarm_occurrence_id),
    KEY idx_alarm_deactivation_log_member_id (member_id),
    CONSTRAINT fk_alarm_deactivation_log_occurrence
        FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id),
    CONSTRAINT fk_alarm_deactivation_log_member
        FOREIGN KEY (member_id) REFERENCES member (id)
);
