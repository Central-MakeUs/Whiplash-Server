# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# 빌드
./gradlew build
./gradlew clean build

# 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트 전체 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "akuma.whiplash.domains.alarm.domain.service.AlarmCommandServiceTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "akuma.whiplash.domains.alarm.domain.service.AlarmCommandServiceTest.RingAlarmTest.success"

# Docker Compose로 전체 스택 실행 (MySQL, Redis, Prometheus, Grafana, k6)
docker-compose up -d
```

## 아키텍처

### 레이어 구조 (도메인별로 반복)

```
presentation (Controller)
    ↓
application (UseCase)         ← 도메인 서비스 조합, DTO 변환
    ↓
domain (CommandService / QueryService)  ← 비즈니스 규칙
    ↓
persistence (Repository / Entity)
```

각 도메인(`alarm`, `auth`, `member`, `place`)은 위 4개 레이어를 독립적으로 가진다.

### 도메인 패키지 구조

```
akuma.whiplash/
├── domains/
│   └── {domain}/
│       ├── application/
│       │   ├── dto/           (request, response, etc)
│       │   ├── mapper/        (Entity ↔ DTO)
│       │   ├── usecase/       (UseCase 인터페이스 + 구현)
│       │   └── scheduler/     (alarm 도메인만 존재)
│       ├── domain/
│       │   ├── constant/      (enum)
│       │   └── service/       (CommandService, QueryService)
│       ├── persistence/
│       │   ├── entity/
│       │   └── repository/
│       ├── presentation/
│       └── exception/         (도메인별 ErrorCode enum)
│
├── infrastructure/
│   ├── redis/                 (RedisRepository, RedisService, RingingAlarmRedisRepository)
│   └── firebase/              (FcmService, MockFcmService)
│
└── global/
    ├── config/
    │   ├── security/          (JWT, Spring Security)
    │   └── scheduler/         (스케줄러 ThreadPool)
    ├── exception/             (ApplicationException)
    └── response/code/         (공통 ErrorCode, SuccessCode)
```

### 예외 처리 규칙

- 모든 도메인 예외는 `ApplicationException.from(ErrorCode)` 패턴 사용
- 각 도메인에 `{Domain}ErrorCode` enum 존재 (예: `AlarmErrorCode`, `AuthErrorCode`)
- ErrorCode enum 포맷: `DOMAIN_STATUS(HttpStatus.STATUS, "Domain_x0n", "message")`
- 허용 HTTP 상태: `400`, `401`, `403`, `404`, `409`만 사용

### API 응답 규칙

- 공통 래퍼: `ApplicationResponse<T>`
- 성공 코드: `SuccessCode` enum
- 오류 코드: `CommonErrorCode` / 도메인별 `*ErrorCode` enum

## 코드 컨벤션

### 레이어별 메서드 명명 규칙

| 동작 | Controller / UseCase / Service | Repository |
|---|---|---|
| 조회 | `getXxx` | `findByXxx`, `countByXxx`, `existsByXxx` |
| 생성 | `createXxx` | `insertXxx` |
| 삭제 | `removeXxx` | `deleteXxx` |
| 수정 | `modifyXxx` | `updateXxx` |

### 요청/응답 객체 필드 규칙

- PK는 반드시 도메인명을 붙인다: `Alarm.id` → `alarmId`
- URL에 도메인이 명시되는 요청 객체는 도메인명 생략 (PK 제외): `POST /api/alarms` 의 바디는 `purpose` (not `alarmPurpose`)
- 응답 List 필드명: `{도메인명}s` (자료형 명시 X) → `alarms`, `tickets`
- 중첩 객체 속성은 엔티티 이름 생략 (PK 제외)
- Enum 값은 `.name()` 그대로 반환
- 날짜는 `ISO_LOCAL_DATE`(2011-12-03) 또는 `ISO_LOCAL_DATE_TIME`(2011-12-03T10:15:30) 포맷 사용
- 페이지네이션 파라미터: `page`, `size`, `sortType`

## 테스트 컨벤션

### 어떤 테스트를 선택할까

| 목적 | 사용할 어노테이션 |
|---|---|
| 컨트롤러 요청-응답 / 검증 / 예외 핸들링 | `@WebMvcTest` |
| 서비스 비즈니스 로직 / 트랜잭션 경계 | `@ExtendWith(MockitoExtension.class)` |
| 리포지토리 / 엔티티 / JPQL | `@PersistenceTest` |
| 보안 + 필터 + DB/Redis 연동 + 전체 플로우 | `@IntegrationTest` |
| Redis Sorted Set 등 Redis 슬라이스 | `@DataRedisTest` + `RedisContainerInitializer` |

### 테스트 어노테이션 상세

- **`@ExtendWith(MockitoExtension.class)`**: 서비스 단위 테스트. DB/Redis 없음, Mockito만 사용
- **`@WebMvcTest`**: 컨트롤러 슬라이스. 보안 필터 제외 시 `@AutoConfigureMockMvc(addFilters = false)`. 협력 빈은 `@MockitoBean`으로 주입
- **`@PersistenceTest`**: MySQL Testcontainer + `@DataJpaTest`. JPA Auditing 필요 시 `@Import(JpaAuditingConfig.class)`
- **`@IntegrationTest`**: 전체 Spring 컨텍스트, MySQL + Redis Testcontainer, 트랜잭션 롤백
- **`@DataRedisTest`** + `@ContextConfiguration(initializers = RedisContainerInitializer.class)`: Redis 슬라이스

### 테스트 구조 규칙 (@Nested)

하나의 테스트 클래스는 **클래스의 각 메서드마다 `@Nested` inner class 하나**로 구성한다.
각 inner class 안에 성공/실패 케이스를 모두 작성한다.

```
{원본클래스}Test
  └── @Nested {메서드명}Test       ← 메서드마다 inner class
        ├── success()
        ├── fail_{비즈니스실패이유}()
        └── fail_{다른실패이유}()
```

예시:
```java
@DisplayName("AlarmCommandService Unit Test")
@ExtendWith(MockitoExtension.class)
class AlarmCommandServiceTest {

    @Mock private AlarmRepository alarmRepository;
    @InjectMocks private AlarmCommandServiceImpl alarmCommandService;

    @Nested
    @DisplayName("alarmOff - 알람 끄기(OFF)")
    class AlarmOffTest {

        @Test
        @DisplayName("성공: 주간 OFF 한도 내에서 알람을 끈다")
        void success() { ... }

        @Test
        @DisplayName("실패: 주간 OFF 한도를 초과하면 예외를 던진다")
        void fail_weeklyLimitExceeded() { ... }
    }

    @Nested
    @DisplayName("ringAlarm - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("성공: alarmRinging=true, 로그 저장, Redis 적재가 모두 수행된다")
        void success() { ... }

        @Test
        @DisplayName("실패: 알람 시간이 되지 않았으면 예외를 던진다")
        void fail_notAlarmTime() { ... }
    }
}
```

### 클래스 / 메서드 네이밍

- 루트 테스트 클래스: `{원본클래스명}Test`
- inner 테스트 클래스: `{메서드명}Test` (ex. `alarmOff` → `AlarmOffTest`)
- 테스트 메서드: `success` / `fail_{비즈니스_관점_실패이유}` (ex. `fail_weeklyLimitExceeded`, `fail_memberNotFound`)

### DisplayName 규칙

- 문장형으로 작성. "~테스트" 금지
- 결과까지 기술: `"성공: 주간 OFF 한도 내에서 알람을 끈다"` / `"실패: 한도 초과 시 예외를 던진다"`
- inner class의 `@DisplayName`: `"{메서드명} - {한글 기능 설명}"` (ex. `"alarmOff - 알람 끄기(OFF)"`)
- 도메인 용어 사용 (메서드 이름 관점 X, 정책 관점 O)

### BDD 스타일 (Given / When / Then)

모든 테스트는 `// given`, `// when`, `// then` 주석으로 구분한다.

### 테스트 픽스처

`src/test/java/akuma/whiplash/common/fixture/`에 enum 기반 픽스처 존재:
- `MemberFixture` — 테스트용 멤버 (`MEMBER_1` ~ `MEMBER_N`), `toMockEntity()` 제공
- `AlarmFixture` — 테스트용 알람 (`ALARM_01` ~ `ALARM_N`), `toMockEntity()` 제공
- `AlarmOccurrenceFixture`

### FCM 테스트

프로파일 `test`에서는 `MockFcmService`가 자동으로 등록되어 실제 FCM 요청을 보내지 않는다.

## 커밋 메시지 컨벤션

```
[#이슈번호] :Emoji: <type>: <subject>

<body>
- 파일명
  - 변경 내용

<footer>
- 해결: #이슈번호
```

| Type | Emoji | 설명 |
|---|---|---|
| Feature | ✨ | 새로운 기능 추가 |
| Fix | 🐛 | 버그 수정 |
| Docs | 📝 | 문서 수정 |
| Style | 🎨 | 코드 포맷팅 (로직 변경 없음) |
| Refactor | ♻️ | 리팩토링 |
| Test | ✅ | 테스트 코드 추가/수정 |
| Chore | 🔧 | 빌드, 패키지 매니저 수정 |

Subject 규칙: 50자 이하, 마침표 없음, 한글 개조식 또는 영문 동사원형 대문자 시작.

## 인프라 의존성

| 서비스 | 용도 | 설정 파일 |
|---|---|---|
| MySQL 8.0 | 메인 DB | `mysql.yml` |
| Redis 7.2 | 토큰 캐시, 알람 울림 상태 (`alarm:ringing` Sorted Set) | `redis.yml` |
| Firebase FCM | 푸시 알림 | `whiplash-firebase-key.json` |
| Google Sheets API | 알람 삭제 사유 로깅 | `oauth.yml` |
| Prometheus + Grafana | 메트릭 수집/시각화 | `docker-compose.yml` |
| Sentry | 에러 트래킹 | `sentry.yml` |


## 스프링 프로파일

`local` / `dev` / `qa` / `prod` — `--spring.profiles.active={profile}`으로 지정.
각 프로파일은 `resources/` 하위의 `mysql.yml`, `redis.yml` 등을 import한다.

## Claude Code 관련 질문 처리

claude-code-guide는 틀린 답을 낼 때가 있다. 사용자가 Claude Code 기능에 대해 추가 질문을 하면, 공식 문서를 curl로 직접 참조해서 답한다.

```bash
curl https://code.claude.com/docs/ko/overview.md
```

문서 URL 패턴: `https://code.claude.com/docs/ko/{페이지명}.md`

답변 후에는 `AskUserQuestion`으로 퀴즈를 내서 사용자가 직접 따라해보도록 안내한다.