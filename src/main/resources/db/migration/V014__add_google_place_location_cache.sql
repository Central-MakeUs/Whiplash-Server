ALTER TABLE alarm
    MODIFY COLUMN latitude DOUBLE NULL COMMENT '목표 위치 위도 | Google 장소는 임시 캐시',
    MODIFY COLUMN longitude DOUBLE NULL COMMENT '목표 위치 경도 | Google 장소는 임시 캐시',
    MODIFY COLUMN address VARCHAR(255) NULL COMMENT '목표 위치 주소 | Google 장소는 임시 캐시',
    ADD COLUMN location_source VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_PLACE' COMMENT '위치 데이터 출처',
    ADD COLUMN google_place_id VARCHAR(255) NULL COMMENT 'Google Place ID',
    ADD COLUMN location_cached_at DATETIME(6) NULL COMMENT 'Google 위치 캐시 저장 시각',
    ADD KEY idx_alarm_location_cache_cleanup (location_source, location_cached_at);

UPDATE alarm
SET location_source = 'GOOGLE_PLACE',
    latitude = NULL,
    longitude = NULL,
    address = NULL,
    location_cached_at = NULL;
