ALTER TABLE ad_session
    ADD COLUMN alarm_occurrence_id BIGINT NULL COMMENT '알람 발생 건 ID | 광고로 알람을 끄는 경우에만 귀속되는 발생 건 ID' AFTER alarm_id,
    ADD CONSTRAINT fk_ad_session_alarm_occurrence
        FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id),
    ADD KEY idx_ad_session_alarm_occurrence_id (alarm_occurrence_id);

ALTER TABLE alarm_deactivation_log
    ADD COLUMN ad_proof_token VARCHAR(64) NULL COMMENT '광고 증명 ID | 광고로 알람을 끈 경우의 공개 광고 세션 ID' AFTER payment_id;
