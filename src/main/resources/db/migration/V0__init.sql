CREATE TABLE member
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 ID | 회원을 식별하는 내부 PK',
    provider         VARCHAR(20)  NOT NULL COMMENT '소셜 로그인 제공자 | 가입에 사용한 소셜 로그인 제공자',
    provider_user_id VARCHAR(100) NOT NULL COMMENT '소셜 제공자 회원 ID | 소셜 제공자가 발급한 회원 식별자',
    email            VARCHAR(255) NULL COMMENT '이메일 | 소셜 로그인으로 수집한 회원 이메일',
    nickname         VARCHAR(50)  NULL COMMENT '닉네임 | 서비스에서 표시할 회원 이름',
    status           VARCHAR(20)  NOT NULL COMMENT '회원 상태 | ACTIVE, WITHDRAWN 등 회원 이용 상태',
    role             VARCHAR(255) NULL COMMENT '회원 권한 | 서비스 접근 권한 구분',
    last_login_at    DATETIME(6)  NULL COMMENT '마지막 로그인 시각 | 회원이 마지막으로 로그인한 일시',
    deleted_at       DATETIME(6)  NULL COMMENT '회원 삭제 시각 | 탈퇴 또는 삭제 처리된 일시',
    created_at       DATETIME(6)  NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at       DATETIME(6)  NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    CONSTRAINT UK_MEMBER_PROVIDER UNIQUE (provider, provider_user_id)
) COMMENT='회원';

CREATE TABLE member_device
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 기기 ID | 회원 기기를 식별하는 내부 PK',
    member_id      BIGINT       NOT NULL COMMENT '회원 ID | 기기를 소유한 회원의 ID',
    device_id      VARCHAR(100) NOT NULL COMMENT '기기 식별자 | 클라이언트에서 전달하는 기기 고유 식별자',
    platform       VARCHAR(20)  NOT NULL COMMENT '기기 플랫폼 | iOS, Android 등 클라이언트 플랫폼',
    fcm_token      VARCHAR(255) NULL COMMENT 'FCM 토큰 | 푸시 알림 발송에 사용하는 Firebase 토큰',
    is_logged_in   BOOLEAN      NOT NULL COMMENT '로그인 여부 | 해당 기기의 현재 로그인 상태',
    app_version    VARCHAR(30)  NULL COMMENT '앱 버전 | 기기에 설치된 앱 버전',
    os_version     VARCHAR(30)  NULL COMMENT 'OS 버전 | 기기 운영체제 버전',
    time_zone      VARCHAR(50)  NOT NULL DEFAULT 'Asia/Seoul' COMMENT '기기 타임존 | 알람 계산에 사용하는 IANA 타임존',
    last_active_at DATETIME(6)  NULL COMMENT '마지막 활성 시각 | 기기가 마지막으로 활동한 일시',
    created_at     DATETIME(6)  NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at     DATETIME(6)  NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    CONSTRAINT UK_MEMBER_DEVICE UNIQUE (member_id, device_id),
    CONSTRAINT fk_member_device_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='회원 기기';

CREATE TABLE alarm
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 ID | 알람을 식별하는 내부 PK',
    member_id           BIGINT      NOT NULL COMMENT '회원 ID | 알람을 소유한 회원의 ID',
    alarm_purpose       VARCHAR(50) NOT NULL COMMENT '알람 목적 | 사용자가 설정한 알람 목적 또는 제목',
    alarm_time          TIME(6)     NOT NULL COMMENT '알람 시각 | 사용자가 설정한 하루 중 알람 시간',
    repeat_days         TEXT        NOT NULL COMMENT '반복 요일 | 알람이 반복되는 요일 목록',
    sound_type          VARCHAR(20) NOT NULL COMMENT '알람 소리 유형 | 알람 재생에 사용할 소리 타입',
    latitude            DOUBLE      NOT NULL COMMENT '목표 위치 위도 | 알람 해제를 위한 목표 위치의 위도',
    longitude           DOUBLE      NOT NULL COMMENT '목표 위치 경도 | 알람 해제를 위한 목표 위치의 경도',
    address             VARCHAR(50) NOT NULL COMMENT '목표 위치 주소 | 알람 해제를 위한 목표 위치 주소',
    status              VARCHAR(20) NOT NULL COMMENT '알람 상태 | ACTIVE, INACTIVE, DELETED 등 알람 상태',
    next_scheduled_time DATETIME(6) NULL COMMENT '다음 회차 일시 | 다음으로 예정된 알람 발생 일시',
    deleted_at          DATETIME(6) NULL COMMENT '삭제 일시 | 알람이 삭제 처리된 일시',
    created_at          DATETIME(6) NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at          DATETIME(6) NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    KEY idx_alarm_member_id (member_id),
    CONSTRAINT fk_alarm_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람';

CREATE TABLE alarm_occurrence
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 발생 ID | 특정 날짜의 알람 회차를 식별하는 내부 PK',
    alarm_id        BIGINT      NOT NULL COMMENT '알람 ID | 발생 회차가 속한 알람의 ID',
    occurrence_date DATE        NOT NULL COMMENT '알람 발생 날짜 | 알람이 울리도록 예약된 날짜',
    occurrence_time TIME(6)     NOT NULL COMMENT '알람 발생 시각 | 알람이 울리도록 예약된 시간',
    scheduled_at    DATETIME(6) NOT NULL COMMENT '예정 일시 | 날짜와 시간을 합산한 실제 예약 일시',
    status          VARCHAR(30) NOT NULL COMMENT '발생 상태 | SCHEDULED, RINGING, DEACTIVATED 등 회차 상태',
    deactivated_at  DATETIME(6) NULL COMMENT '비활성화 시각 | 알람 회차가 해제된 일시',
    checkin_time    DATETIME(6) NULL COMMENT '체크인 시각 | 목표 위치 체크인이 완료된 일시',
    alarm_ringing   BOOLEAN     NOT NULL COMMENT '울림 여부 | 현재 알람 울림이 진행 중인지 여부',
    ringing_count   INT         NOT NULL COMMENT '울림 횟수 | 해당 회차에서 알람을 울린 누적 횟수',
    reminder_sent   BOOLEAN     NOT NULL COMMENT '리마인더 발송 여부 | 해당 회차의 리마인더 푸시 발송 여부',
    created_at      DATETIME(6) NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at      DATETIME(6) NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    CONSTRAINT uk_alarm_date UNIQUE (alarm_id, occurrence_date),
    KEY idx_alarm_occurrence_alarm_id (alarm_id),
    KEY idx_alarm_occurrence_ringing_query (status, alarm_ringing, alarm_id),
    CONSTRAINT fk_alarm_occurrence_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='알람 발생';

CREATE TABLE alarm_deactivation_log
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 비활성화 로그 ID | 알람 비활성화 시도를 식별하는 내부 PK',
    alarm_occurrence_id BIGINT       NOT NULL COMMENT '알람 발생 ID | 비활성화 대상 알람 회차의 ID',
    member_id           BIGINT       NOT NULL COMMENT '회원 ID | 비활성화를 요청한 회원의 ID',
    payment_id          VARCHAR(100) NULL COMMENT '결제 ID | 결제 기반 비활성화와 연결된 외부 결제 ID',
    deactivate_type     VARCHAR(20)  NOT NULL COMMENT '비활성화 유형 | 체크인, 결제 등 알람 해제 방식',
    request_device_id   VARCHAR(100) NOT NULL COMMENT '요청 기기 ID | 비활성화를 요청한 기기 식별자',
    requested_at        DATETIME(6)  NOT NULL COMMENT '요청 시각 | 비활성화 요청이 발생한 일시',
    processed_at        DATETIME(6)  NOT NULL COMMENT '처리 시각 | 비활성화 처리가 완료된 일시',
    result              VARCHAR(20)  NOT NULL COMMENT '처리 결과 | SUCCESS, FAIL 등 비활성화 처리 결과',
    fail_reason         VARCHAR(500) NOT NULL COMMENT '실패 사유 | 비활성화 실패 시 기록하는 사유',
    created_at          DATETIME(6)  NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at          DATETIME(6)  NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    KEY idx_alarm_deactivation_log_occurrence_id (alarm_occurrence_id),
    KEY idx_alarm_deactivation_log_member_id (member_id),
    CONSTRAINT fk_alarm_deactivation_log_occurrence
        FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id),
    CONSTRAINT fk_alarm_deactivation_log_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 비활성화 로그';

CREATE TABLE alarm_delete_log
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 삭제 로그 ID | 알람 삭제 시도를 식별하는 내부 PK',
    alarm_id       BIGINT        NOT NULL COMMENT '알람 ID | 삭제 대상 알람의 ID',
    member_id      BIGINT        NOT NULL COMMENT '회원 ID | 삭제를 요청한 회원의 ID',
    delete_type    VARCHAR(20)   NOT NULL COMMENT '삭제 유형 | 일반 삭제, 광고 기반 삭제 등 삭제 방식',
    reason         VARCHAR(2000) NOT NULL COMMENT '삭제 사유 | 알람 삭제 사유 또는 처리 설명',
    payment_id     VARCHAR(100)  NULL COMMENT '결제 ID | 결제 기반 삭제와 연결된 외부 결제 ID',
    ad_proof_token VARCHAR(255)  NULL COMMENT '광고 증명 토큰 | 광고 시청 기반 삭제 증명 토큰',
    requested_at   DATETIME(6)   NULL COMMENT '요청 시각 | 삭제 요청이 발생한 일시',
    deleted_at     DATETIME(6)   NULL COMMENT '삭제 시각 | 알람 삭제가 완료된 일시',
    created_at     DATETIME(6)   NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at     DATETIME(6)   NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    KEY idx_alarm_delete_log_alarm_id (alarm_id),
    KEY idx_alarm_delete_log_member_id (member_id),
    CONSTRAINT fk_alarm_delete_log_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id),
    CONSTRAINT fk_alarm_delete_log_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 삭제 로그';

CREATE TABLE alarm_off_log
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 끄기 로그 ID | 알람 끄기 이벤트를 식별하는 내부 PK',
    alarm_id   BIGINT      NOT NULL COMMENT '알람 ID | 끄기 이벤트가 발생한 알람의 ID',
    member_id  BIGINT      NOT NULL COMMENT '회원 ID | 알람 끄기를 수행한 회원의 ID',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    KEY idx_alarm_off_log_alarm_id (alarm_id),
    KEY idx_alarm_off_log_member_id (member_id),
    CONSTRAINT fk_alarm_off_log_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id),
    CONSTRAINT fk_alarm_off_log_member FOREIGN KEY (member_id) REFERENCES member (id)
) COMMENT='알람 끄기 로그';

CREATE TABLE alarm_ringing_log
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알람 울림 로그 ID | 알람 울림 이벤트를 식별하는 내부 PK',
    alarm_occurrence_id BIGINT      NOT NULL COMMENT '알람 발생 ID | 울림 이벤트가 속한 알람 회차의 ID',
    ring_index          INT         NOT NULL COMMENT '울림 순번 | 해당 회차에서 몇 번째 울림인지 나타내는 순번',
    ringed_at           DATETIME(6) NULL COMMENT '울림 시각 | 알람 울림이 발생한 일시',
    created_at          DATETIME(6) NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at          DATETIME(6) NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    KEY idx_alarm_ringing_log_occurrence_id (alarm_occurrence_id),
    CONSTRAINT fk_alarm_ringing_log_occurrence
        FOREIGN KEY (alarm_occurrence_id) REFERENCES alarm_occurrence (id)
) COMMENT='알람 울림 로그';

CREATE TABLE payment
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID | 결제 내역을 식별하는 내부 PK',
    member_id    BIGINT       NOT NULL COMMENT '회원 ID | 결제를 수행한 회원의 ID',
    alarm_id     BIGINT       NOT NULL COMMENT '알람 ID | 결제 대상 알람의 ID',
    payment_id   VARCHAR(100) NOT NULL COMMENT '외부 결제 ID | 앱마켓 또는 결제 제공자가 발급한 결제 식별자',
    payment_type VARCHAR(20)  NOT NULL COMMENT '결제 유형 | 알람 해제 등 결제 목적 구분',
    amount       INT          NOT NULL COMMENT '결제 금액 | 결제 요청 또는 승인된 금액',
    status       VARCHAR(20)  NOT NULL COMMENT '결제 상태 | 결제 처리 상태',
    created_at   DATETIME(6)  NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at   DATETIME(6)  NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    CONSTRAINT uk_payment_payment_id UNIQUE (payment_id),
    KEY idx_payment_member_id (member_id),
    KEY idx_payment_alarm_id (alarm_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_payment_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='결제';

CREATE TABLE ad_session
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '광고 세션 ID | 광고 세션을 식별하는 내부 PK',
    ad_session_id            VARCHAR(64)  NOT NULL COMMENT '광고 세션 공개 ID | 클라이언트와 콜백에서 사용하는 공개 세션 식별자',
    member_id                BIGINT       NOT NULL COMMENT '회원 ID | 광고 세션을 생성한 회원의 ID',
    alarm_id                 BIGINT       NOT NULL COMMENT '알람 ID | 광고 보상 적용 대상 알람의 ID',
    device_id                VARCHAR(255) NOT NULL COMMENT '기기 ID | 광고를 요청한 클라이언트 기기 식별자',
    purpose                  VARCHAR(50)  NOT NULL COMMENT '광고 목적 | 광고 시청으로 수행하려는 기능 목적',
    status                   VARCHAR(30)  NOT NULL COMMENT '광고 세션 상태 | CREATED, VERIFIED, CONSUMED 등 세션 상태',
    expires_at               DATETIME(6)  NOT NULL COMMENT '만료 시각 | 광고 세션을 사용할 수 있는 만료 일시',
    verified_at              DATETIME(6)  NULL COMMENT '검증 시각 | 광고 보상 콜백 검증이 완료된 일시',
    consumed_at              DATETIME(6)  NULL COMMENT '사용 시각 | 광고 보상이 실제 기능에 사용된 일시',
    transaction_id           VARCHAR(128) NULL COMMENT '광고 트랜잭션 ID | 광고 플랫폼에서 전달한 거래 식별자',
    ad_unit_id               VARCHAR(255) NULL COMMENT '광고 단위 ID | 광고 플랫폼의 광고 단위 식별자',
    reward_amount            INT          NULL COMMENT '광고 보상 수량 | 광고 플랫폼에서 전달한 보상 수량',
    reward_item              VARCHAR(100) NULL COMMENT '광고 보상 항목 | 광고 플랫폼에서 전달한 보상 항목명',
    raw_callback_received_at DATETIME(6)  NULL COMMENT '원본 콜백 수신 시각 | 광고 플랫폼 콜백을 최초 수신한 일시',
    created_at               DATETIME(6)  NOT NULL COMMENT '생성 시각 | 데이터가 최초 생성된 일시',
    updated_at               DATETIME(6)  NOT NULL COMMENT '수정 시각 | 데이터가 마지막으로 수정된 일시',
    CONSTRAINT uk_ad_session_ad_session_id UNIQUE (ad_session_id),
    CONSTRAINT uk_ad_session_transaction_id UNIQUE (transaction_id),
    KEY idx_ad_session_member_alarm_status (member_id, alarm_id, status),
    KEY idx_ad_session_expires_at (expires_at),
    KEY idx_ad_session_purpose_status (purpose, status),
    KEY idx_ad_session_alarm_id (alarm_id),
    CONSTRAINT fk_ad_session_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_ad_session_alarm FOREIGN KEY (alarm_id) REFERENCES alarm (id)
) COMMENT='광고 세션';
