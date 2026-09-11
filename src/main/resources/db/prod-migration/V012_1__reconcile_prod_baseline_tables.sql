-- Reconcile legacy production schema before V013+ migrations.
-- Safe on QA/rehearsal databases that already contain these tables.

CREATE TABLE IF NOT EXISTS member_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL, platform VARCHAR(20) NOT NULL,
    fcm_token VARCHAR(255) NULL, is_logged_in BOOLEAN NOT NULL,
    app_version VARCHAR(30) NULL, os_version VARCHAR(30) NULL,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'Asia/Seoul', last_active_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    CONSTRAINT UK_MEMBER_DEVICE UNIQUE (member_id, device_id),
    CONSTRAINT fk_member_device_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='회원 기기';

CREATE TABLE IF NOT EXISTS alarm_deactivation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, alarm_occurrence_id BIGINT NOT NULL, member_id BIGINT NOT NULL,
    payment_id VARCHAR(100) NULL, deactivate_type VARCHAR(20) NOT NULL, request_device_id VARCHAR(100) NOT NULL,
    requested_at DATETIME(6) NOT NULL, processed_at DATETIME(6) NOT NULL, result VARCHAR(20) NOT NULL,
    fail_reason VARCHAR(500) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    KEY idx_alarm_deactivation_log_occurrence_id (alarm_occurrence_id), KEY idx_alarm_deactivation_log_member_id (member_id),
    CONSTRAINT fk_alarm_deactivation_log_occurrence FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id),
    CONSTRAINT fk_alarm_deactivation_log_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 비활성화 로그';

CREATE TABLE IF NOT EXISTS alarm_delete_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, alarm_id BIGINT NOT NULL, member_id BIGINT NOT NULL,
    delete_type VARCHAR(20) NOT NULL, reason VARCHAR(2000) NOT NULL, payment_id VARCHAR(100) NULL,
    ad_proof_token VARCHAR(255) NULL, requested_at DATETIME(6) NULL, deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    KEY idx_alarm_delete_log_alarm_id (alarm_id), KEY idx_alarm_delete_log_member_id (member_id),
    CONSTRAINT fk_alarm_delete_log_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id),
    CONSTRAINT fk_alarm_delete_log_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 삭제 로그';

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL, alarm_id BIGINT NOT NULL,
    payment_id VARCHAR(100) NOT NULL, payment_type VARCHAR(20) NOT NULL, amount INT NOT NULL,
    status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_payment_payment_id UNIQUE (payment_id), KEY idx_payment_member_id (member_id), KEY idx_payment_alarm_id (alarm_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_payment_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='결제';
