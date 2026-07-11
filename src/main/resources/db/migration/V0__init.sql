CREATE TABLE member
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 ID',
    provider         VARCHAR(20)  NOT NULL COMMENT '소셜 로그인 제공자',
    provider_user_id VARCHAR(100) NOT NULL COMMENT '소셜 제공자 회원 ID',
    email            VARCHAR(255) NULL COMMENT '이메일',
    nickname         VARCHAR(50)  NULL COMMENT '닉네임',
    status           VARCHAR(20)  NOT NULL COMMENT '회원 상태',
    role             VARCHAR(255) NULL COMMENT '회원 권한',
    last_login_at    DATETIME(6)  NULL COMMENT '마지막 로그인 시각',
    deleted_at       DATETIME(6)  NULL COMMENT '회원 삭제 시각',
    created_at       DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at       DATETIME(6)  NOT NULL COMMENT '수정 시각',
    CONSTRAINT UK_MEMBER_PROVIDER UNIQUE (provider, provider_user_id)
) COMMENT='회원';

CREATE TABLE member_device
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 기기 ID',
    member_id      BIGINT       NOT NULL COMMENT '회원 ID',
    device_id      VARCHAR(100) NOT NULL COMMENT '기기 식별자',
    platform       VARCHAR(20)  NOT NULL COMMENT '기기 플랫폼',
    fcm_token      VARCHAR(255) NULL COMMENT 'FCM 토큰',
    is_logged_in   BOOLEAN      NOT NULL COMMENT '로그인 여부',
    app_version    VARCHAR(30)  NULL COMMENT '앱 버전',
    os_version     VARCHAR(30)  NULL COMMENT 'OS 버전',
    time_zone      VARCHAR(50)  NOT NULL DEFAULT 'Asia/Seoul' COMMENT '기기 타임존',
    last_active_at DATETIME(6)  NULL COMMENT '마지막 활성 시각',
    created_at     DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at     DATETIME(6)  NOT NULL COMMENT '수정 시각',
    CONSTRAINT UK_MEMBER_DEVICE UNIQUE (member_id, device_id),
    CONSTRAINT fk_member_device_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='회원 기기';

CREATE TABLE alarm
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 ID',
    member_id           BIGINT      NOT NULL COMMENT '회원 ID',
    alarm_purpose       VARCHAR(50) NOT NULL COMMENT '알람 목적',
    alarm_time          TIME(6)     NOT NULL COMMENT '알람 시각',
    repeat_days         TEXT        NOT NULL COMMENT '반복 요일',
    sound_type          VARCHAR(20) NOT NULL COMMENT '알람 소리 유형',
    latitude            DOUBLE      NOT NULL COMMENT '목표 위도',
    longitude           DOUBLE      NOT NULL COMMENT '목표 경도',
    address             VARCHAR(50) NOT NULL COMMENT '목표 주소',
    status              VARCHAR(20) NOT NULL COMMENT '알람 상태',
    next_scheduled_time DATETIME(6) NULL COMMENT '다음 예정 시각',
    deleted_at          DATETIME(6) NULL COMMENT '삭제 시각',
    created_at          DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at          DATETIME(6) NOT NULL COMMENT '수정 시각',
    KEY idx_alarm_member_id (member_id),
    CONSTRAINT fk_alarm_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람';

CREATE TABLE alarm_occurrence
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 발생 ID',
    alarm_id        BIGINT      NOT NULL COMMENT '알람 ID',
    occurrence_date DATE        NOT NULL COMMENT '알람 발생 날짜',
    occurrence_time TIME(6)     NOT NULL COMMENT '알람 발생 시각',
    scheduled_at    DATETIME(6) NOT NULL COMMENT '예정 일시',
    status          VARCHAR(30) NOT NULL COMMENT '발생 상태',
    deactivated_at  DATETIME(6) NULL COMMENT '비활성화 시각',
    checkin_time    DATETIME(6) NULL COMMENT '체크인 시각',
    alarm_ringing   BOOLEAN     NOT NULL COMMENT '울림 여부',
    ringing_count   INT         NOT NULL COMMENT '울림 횟수',
    reminder_sent   BOOLEAN     NOT NULL COMMENT '리마인더 발송 여부',
    created_at      DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at      DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_alarm_date UNIQUE (alarm_id, occurrence_date),
    KEY idx_alarm_occurrence_alarm_id (alarm_id),
    KEY idx_alarm_occurrence_ringing_query (status, alarm_ringing, alarm_id),
    CONSTRAINT fk_alarm_occurrence_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='알람 발생';

CREATE TABLE alarm_deactivation_log
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 비활성화 로그 ID',
    alarm_occurrence_id BIGINT       NOT NULL COMMENT '알람 발생 ID',
    member_id           BIGINT       NOT NULL COMMENT '회원 ID',
    payment_id          VARCHAR(100) NULL COMMENT '결제 ID',
    deactivate_type     VARCHAR(20)  NOT NULL COMMENT '비활성화 유형',
    request_device_id   VARCHAR(100) NOT NULL COMMENT '요청 기기 ID',
    requested_at        DATETIME(6)  NOT NULL COMMENT '요청 시각',
    processed_at        DATETIME(6)  NOT NULL COMMENT '처리 시각',
    result              VARCHAR(20)  NOT NULL COMMENT '처리 결과',
    fail_reason         VARCHAR(500) NOT NULL COMMENT '실패 사유',
    created_at          DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at          DATETIME(6)  NOT NULL COMMENT '수정 시각',
    KEY idx_alarm_deactivation_log_occurrence_id (alarm_occurrence_id),
    KEY idx_alarm_deactivation_log_member_id (member_id),
    CONSTRAINT fk_alarm_deactivation_log_occurrence
        FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id),
    CONSTRAINT fk_alarm_deactivation_log_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 비활성화 로그';

CREATE TABLE alarm_delete_log
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 삭제 로그 ID',
    alarm_id       BIGINT        NOT NULL COMMENT '알람 ID',
    member_id      BIGINT        NOT NULL COMMENT '회원 ID',
    delete_type    VARCHAR(20)   NOT NULL COMMENT '삭제 유형',
    reason         VARCHAR(2000) NOT NULL COMMENT '삭제 사유',
    payment_id     VARCHAR(100)  NULL COMMENT '결제 ID',
    ad_proof_token VARCHAR(255)  NULL COMMENT '광고 증명 토큰',
    requested_at   DATETIME(6)   NULL COMMENT '요청 시각',
    deleted_at     DATETIME(6)   NULL COMMENT '삭제 시각',
    created_at     DATETIME(6)   NOT NULL COMMENT '생성 시각',
    updated_at     DATETIME(6)   NOT NULL COMMENT '수정 시각',
    KEY idx_alarm_delete_log_alarm_id (alarm_id),
    KEY idx_alarm_delete_log_member_id (member_id),
    CONSTRAINT fk_alarm_delete_log_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id),
    CONSTRAINT fk_alarm_delete_log_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 삭제 로그';

CREATE TABLE alarm_off_log
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 끄기 로그 ID',
    alarm_id   BIGINT      NOT NULL COMMENT '알람 ID',
    member_id  BIGINT      NOT NULL COMMENT '회원 ID',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    KEY idx_alarm_off_log_alarm_id (alarm_id),
    KEY idx_alarm_off_log_member_id (member_id),
    CONSTRAINT fk_alarm_off_log_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id),
    CONSTRAINT fk_alarm_off_log_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 끄기 로그';

CREATE TABLE alarm_ringing_log
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 울림 로그 ID',
    alarm_occurrence_id BIGINT      NOT NULL COMMENT '알람 발생 ID',
    ring_index          INT         NOT NULL COMMENT '울림 순번',
    ringed_at           DATETIME(6) NULL COMMENT '울림 시각',
    created_at          DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at          DATETIME(6) NOT NULL COMMENT '수정 시각',
    KEY idx_alarm_ringing_log_occurrence_id (alarm_occurrence_id),
    CONSTRAINT fk_alarm_ringing_log_occurrence
        FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id)
) COMMENT='알람 울림 로그';

CREATE TABLE payment
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID',
    member_id    BIGINT       NOT NULL COMMENT '회원 ID',
    alarm_id     BIGINT       NOT NULL COMMENT '알람 ID',
    payment_id   VARCHAR(100) NOT NULL COMMENT '외부 결제 ID',
    payment_type VARCHAR(20)  NOT NULL COMMENT '결제 유형',
    amount       INT          NOT NULL COMMENT '결제 금액',
    status       VARCHAR(20)  NOT NULL COMMENT '결제 상태',
    created_at   DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at   DATETIME(6)  NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_payment_payment_id UNIQUE (payment_id),
    KEY idx_payment_member_id (member_id),
    KEY idx_payment_alarm_id (alarm_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_payment_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='결제';

CREATE TABLE ad_session
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '광고 세션 ID',
    ad_session_id            VARCHAR(64)  NOT NULL COMMENT '광고 세션 공개 ID',
    member_id                BIGINT       NOT NULL COMMENT '회원 ID',
    alarm_id                 BIGINT       NOT NULL COMMENT '알람 ID',
    device_id                VARCHAR(255) NOT NULL COMMENT '기기 ID',
    purpose                  VARCHAR(50)  NOT NULL COMMENT '광고 목적',
    status                   VARCHAR(30)  NOT NULL COMMENT '광고 세션 상태',
    expires_at               DATETIME(6)  NOT NULL COMMENT '만료 시각',
    verified_at              DATETIME(6)  NULL COMMENT '검증 시각',
    consumed_at              DATETIME(6)  NULL COMMENT '사용 시각',
    transaction_id           VARCHAR(128) NULL COMMENT '광고 트랜잭션 ID',
    ad_unit_id               VARCHAR(255) NULL COMMENT '광고 단위 ID',
    reward_amount            INT          NULL COMMENT '광고 보상 수량',
    reward_item              VARCHAR(100) NULL COMMENT '광고 보상 항목',
    raw_callback_received_at DATETIME(6)  NULL COMMENT '원본 콜백 수신 시각',
    created_at               DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at               DATETIME(6)  NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_ad_session_ad_session_id UNIQUE (ad_session_id),
    CONSTRAINT uk_ad_session_transaction_id UNIQUE (transaction_id),
    KEY idx_ad_session_member_alarm_status (member_id, alarm_id, status),
    KEY idx_ad_session_expires_at (expires_at),
    KEY idx_ad_session_purpose_status (purpose, status),
    KEY idx_ad_session_alarm_id (alarm_id),
    CONSTRAINT fk_ad_session_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_ad_session_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='광고 세션';
