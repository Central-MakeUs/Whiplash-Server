---
name: ask-env
description: 이 저장소에서 프로젝트 실행 방법, Spring profile 선택, 인프라 의존성, Redis/FCM 연동 위치, local과 비운영 환경 차이를 설명할 때 사용한다.
---

# Ask Env

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 자주 쓰는 커맨드

```bash
./gradlew clean build
./gradlew test
./gradlew test --tests "akuma.whiplash.{package}.{ClassName}"
./gradlew bootRun --args='--spring.profiles.active=local'
docker-compose up -d
```

## Spring 프로파일

| Profile | Notes |
|---|---|
| `local` | 로컬 개발용 mock FCM, mock place 연동 동작이 포함된다 |
| `dev` | GitHub Actions CD 환경 |
| `qa` | Jenkins CD 환경, QA 전용 인증 동작이 포함될 수 있다 |
| `prod` | 운영 전용 서비스 및 동작 |
| `test` | mock 중심 동작, Sentry 비활성화 |

## 인프라

| Service | Purpose |
|---|---|
| MySQL 8 | 메인 관계형 DB |
| Redis 7.2 | 토큰 캐시 및 알람 관련 임시 상태 저장 |
| Firebase FCM | 푸시 알림 |
| AuditLogRecorder | 알람 삭제 실패 감사 로그 저장(AlarmDeleteLogEntity) |
| Naver API | 장소 검색 / 역지오코딩 |
| Sentry | 에러 추적 |

## Redis 키 패턴

- `REFRESH:{memberId}:{deviceId}`: refresh token
- `fcm:member:{memberId}`: member FCM token set
- `alarm:ringing`: 현재 울리는 알람 sorted set

## 관련 코드 위치

- Redis integration: [src/main/java/akuma/whiplash/infrastructure/redis](../../../src/main/java/akuma/whiplash/infrastructure/redis)
- Firebase integration: [src/main/java/akuma/whiplash/infrastructure/firebase](../../../src/main/java/akuma/whiplash/infrastructure/firebase)
- Place integration: [src/main/java/akuma/whiplash/domains/place](../../../src/main/java/akuma/whiplash/domains/place)

## 응답 가이드

- 추정보다 저장소 기준 답변을 우선한다.
- 환경별로 동작이 다르면 정확한 profile명을 함께 언급한다.
- 커맨드나 설정이 바뀌었을 수 있으면 답변 전에 저장소 기준으로 다시 확인한다.
