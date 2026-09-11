---
name: ask-env
description: >
  Time Bomb Server 실행 커맨드, 스프링 프로파일, 인프라 의존성, Claude Code 문서 조회 안내.
  "어떻게 실행해", "프로파일", "Docker", "인프라", "Claude Code 문서" 요청 시 참고.
---

# 환경 / 인프라 스킬

## 커맨드
```bash
./gradlew build / clean build
./gradlew test
./gradlew test --tests "akuma.whiplash.{패키지}.{클래스명}"
./gradlew bootRun --args='--spring.profiles.active=local'
docker-compose up -d
```

## 스프링 프로파일

| 프로파일 | 특이사항 |
|---|---|
| `local` | MockFcmService, MockPlaceQueryService, DevAuthController 활성 |
| `dev` | GitHub Actions CD |
| `qa` | Jenkins CD, QaAuthController 활성 |
| `prod` | Jenkins CD, ProdArchiveService 활성 |
| `test` | MockFcmService 활성, Sentry 비활성 |

## 인프라

| 서비스 | 용도 | 설정 |
|---|---|---|
| MySQL 8.0 | 메인 DB | `mysql.yml` |
| Redis 7.2 | 토큰 캐시, `alarm:ringing` Sorted Set | `redis.yml` |
| FCM | 푸시 알림 | `whiplash-firebase-key.json` |
| AuditLogRecorder | 알람 삭제 실패 감사 로그 저장 | DB `payment`, `alarm_delete_log` |
| Naver API | 장소 검색/역지오코딩 | `naver.yml` |
| Sentry | 에러 트래킹 | `sentry.yml` |

## Redis 키 패턴
- `REFRESH:{memberId}:{deviceId}` — 리프레시 토큰
- `fcm:member:{memberId}` — FCM 토큰 Set
- `alarm:ringing` — 현재 울리는 알람 Sorted Set (score = epoch millis)

## 인프라 코드
- Redis: `infrastructure/redis/` (RedisService)
- FCM: `infrastructure/firebase/` (FcmService / MockFcmService)

## Claude Code 문서
```bash
curl https://code.claude.com/docs/ko/{페이지명}.md
# 주요: overview / sub-agents / hooks-guide / skills
```
