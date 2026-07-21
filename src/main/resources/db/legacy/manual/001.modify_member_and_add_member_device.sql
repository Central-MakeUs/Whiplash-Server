-- ============================================================
-- V2: member 테이블 변경 + member_device 테이블 신규 생성
-- ============================================================

-- 1. member 테이블 변경
ALTER TABLE member
    DROP COLUMN social_id,
    DROP COLUMN privacy_policy,
    DROP COLUMN push_notification_policy,
    DROP COLUMN privacy_agreed_at,
    DROP COLUMN push_agreed_at;

ALTER TABLE member
    ADD COLUMN provider        VARCHAR(20)  NOT NULL AFTER id,
    ADD COLUMN provider_user_id VARCHAR(100) NOT NULL AFTER provider,
    ADD COLUMN status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' AFTER nickname,
    ADD COLUMN last_login_at   DATETIME     NULL AFTER updated_at,
    ADD COLUMN deleted_at      DATETIME     NULL AFTER last_login_at;

ALTER TABLE member
    ADD UNIQUE KEY UK_MEMBER_PROVIDER (provider, provider_user_id);

-- 2. member_device 테이블 신규 생성
CREATE TABLE member_device
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT       NOT NULL,
    device_id     VARCHAR(100) NOT NULL,
    platform      VARCHAR(20)  NOT NULL,
    fcm_token     VARCHAR(255) NULL,
    is_logged_in  BOOLEAN      NOT NULL DEFAULT TRUE,
    app_version   VARCHAR(30)  NULL,
    os_version    VARCHAR(30)  NULL,
    last_active_at DATETIME    NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    UNIQUE KEY UK_MEMBER_DEVICE (member_id, device_id),
    CONSTRAINT FK_MEMBER_DEVICE_MEMBER FOREIGN KEY (member_id) REFERENCES member (id)
);
