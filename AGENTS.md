# Time Bomb Server

이 문서는 이 저장소에서 작업할 때 항상 적용하는 공통 규칙이다.

## 프로젝트 컨텍스트

- 현재 제품명: `Time Bomb`
- 이전 제품명: `Whiplash` -> `눈 떠!` -> `Time Bomb`
- 저장소명과 Java 베이스 패키지의 `Whiplash`, `akuma.whiplash`는 호환성을 위해 유지하는 기술 식별자다.
- 스택: Java 17, Spring Boot 3.5, MySQL 8, Redis 7.2, Firebase FCM
- 주요 도메인: `ad`, `alarm`, `auth`, `member`, `device`, `payment`, `place`
- 베이스 패키지: `akuma.whiplash`

## 단일 원본과 충돌 처리

- 저장소 공통 개발 규칙의 단일 원본은 루트 `AGENTS.md`다.
- 공개 가능한 제품명과 공통 도메인 용어의 단일 원본은 루트 `CONTEXT.md`다.
- 아키텍처와 운영 정책의 선택 이유는 `docs/adr/`의 최신 `Accepted` ADR을 따른다.
- 실제 구현 상태는 source code, test, Flyway migration을 기준으로 확인한다.
- `docs/history/`는 특정 작업 당시의 계획과 이력이며 현재 정책을 자동으로 대체하지 않는다.
- 규칙, ADR, 문서, 코드가 충돌하면 임의로 하나를 선택하지 않는다. 실제 구현과 목표 정책의 차이를 사용자에게 알리고 작업 범위를 확인한다.
- `.claude`, `.codex` 등 도구별 지침이 이 문서와 충돌하면 이 문서를 우선한다.

## 문서 라우팅

- 모든 작업: `AGENTS.md`
- 제품명과 공개 가능한 공통 용어 확인: `CONTEXT.md`
- 문서 구조와 읽기 순서 확인: `docs/README.md`
- 제품 목표, 범위, KPI, SLO 확인: `docs/PRD.md`
- 전역 시스템 구조와 외부 연동 확인: `docs/ARCHITECTURE.md`
- 개인정보, 위치 데이터, 감사/운영 로그 변경: `docs/PRIVACY_AND_AUDIT_LOGGING_POLICY.md`
- 구조적 결정 또는 정책 선택: 관련 `docs/adr/*.md`
- 기능별 과거 계획, API 계약, 인수인계: 관련 `docs/history/{번호}-{기능명}/`
- 반복 가능한 작업 절차: `.codex/skills/{skill-name}/SKILL.md`
- 실제 정책 검증: `src/test/`
- 자동 검증과 배포 절차: `.github/workflows/`

공개 저장소에서 Git으로 추적하는 지식 문서는 루트 `CONTEXT.md`와 `docs/README.md`뿐이다. 그 외 `docs/**`와 `CONTEXT.local.md`는 로컬 지식이며 Git에서 제외한다. 이 경계를 임의로 바꾸지 않으며, Git 제외 여부는 보안 경계가 아니므로 API key, token, password, 개인정보 원문, 운영 secret은 어느 문서에도 기록하지 않는다.

## 절대 규칙

- 신규 기능 개발, 기존 기능 리팩토링, 버그 수정, API/도메인 정책 변경 작업은 먼저 `.codex/skills/development-workflow/SKILL.md`를 적용한다.
- 예외는 반드시 `ApplicationException.from(ErrorCode)`를 사용한다.
- 도메인 `XxxErrorCode`의 허용 `HttpStatus`는 `400`, `401`, `403`, `404`, `409`만 사용한다. framework/system 오류를 표현하는 `CommonErrorCode`는 예외다.
- API 응답은 반드시 `ApplicationResponse<T>`를 사용한다.
- 레이어 방향은 `presentation -> application -> domain -> persistence`를 지킨다.
- 네이밍 규칙:
  - Service / UseCase: `getXxx`, `createXxx`, `removeXxx`, `modifyXxx`
  - Repository: `findByXxx`, `countByXxx`, `existsByXxx`, `insertXxx`, `updateXxx`, `deleteXxx`

## 레이어 규칙

- 역방향 의존성을 만들지 않는다.
- `presentation`은 controller를 담당한다.
- `application`은 DTO, mapper, use case, event, listener, scheduler, orchestration을 담당한다.
- `domain`은 비즈니스 서비스, enum/constant, 도메인 로직을 담당한다.
- `persistence`는 entity와 repository를 담당한다.

## 구현 원칙

- 입출력과 동작이 동일한 의미 없는 래퍼 메서드나 클래스를 만들지 않는다.
- 기능을 추가하기 전에 코드베이스 전체에서 기존 함수와 구현을 검색하고, 동일한 책임과 의미를 가진 구현을 우선 재사용한다.
- 동일한 책임과 의미를 가진 함수는 코드베이스에 중복으로 만들지 않는다. 단, 레이어나 도메인 책임이 다른 로직을 형태가 비슷하다는 이유만으로 통합하지 않는다.
- 한 번만 사용되고 가독성을 높이지 않는 지역 변수나 타입 변수는 만들지 않고 인라인으로 표현한다.
- 현재 책임을 수행하는 데 필수적이지 않은 파라미터는 추가하지 않는다.
- 클래스와 메서드의 책임을 명확히 분리한다. 요구사항이나 구현 방식이 기존 아키텍처와 충돌하거나 설계상 부자연스러우면 구현 전에 문제와 대안을 알린다.

## 예외 및 응답 규칙

- 예외는 `ApplicationException.from(XxxErrorCode.SOME_ERROR)`로 발생시킨다.
- 신규 ErrorCode 포맷:

```java
NAME(HttpStatus.STATUS, "DOMAIN_001", "~입니다.")
```

- 신규 코드 번호는 `001~099=400`, `101~199=401`, `301~399=403`, `401~499=404`, `901~999=409` 범위를 사용한다.
- 이미 외부 계약으로 사용 중인 ErrorCode는 번호 범위가 다르더라도 형식 통일만을 위해 변경하지 않는다.

- Controller 메서드는 `ApplicationResponse.onSuccess(result)` 또는 `ApplicationResponse.onSuccess()`를 반환한다.
- 모든 매핑 메서드에는 `@CustomErrorCodes`를 선언한다.

## Mapper 규칙

- 엔티티/DTO 변환은 `{Domain}Mapper.mapToXxx()` static 메서드로 처리한다.
- Service나 UseCase 내부에서 변환용 builder를 인라인으로 작성하지 않는다.
- Mapper 클래스는 아래 형태를 따른다.

```java
public class XxxMapper {
    private XxxMapper() {
        throw new IllegalArgumentException();
    }
}
```

## Entity 및 Repository 규칙

- FK id 필드보다 `@ManyToOne(fetch = FetchType.LAZY)` 객체 참조를 우선한다.
- 중첩 프로퍼티 Repository 쿼리는 `_`로 경로를 구분한다.

```java
findByMember_IdAndDeviceId(Long memberId, String deviceId)
```

## DTO 규칙

- PK 필드는 `alarmId`, `memberId`처럼 도메인명을 포함한다.
- List 응답 필드명은 `alarms`처럼 복수형 도메인명을 사용한다.
- Enum 값은 기본적으로 `.name()`을 사용한다.
- 날짜/시간 값은 `ISO_LOCAL_DATE` 또는 `ISO_LOCAL_DATE_TIME` 형식을 따른다.
- 페이지네이션 필드는 `page`, `size`, `sortType`을 사용한다.

## 시간 처리 규칙

- 사용자 시간 계산은 관련 도메인 정책의 명시적인 `ZoneId`를 사용하며 `ZoneId.systemDefault()` 또는 고정 `Asia/Seoul`에 의존하지 않는다.
- 새 비즈니스 로직에서는 `LocalDateTime.now()`, `LocalDate.now()`, `LocalTime.now()`를 직접 호출하지 않고 `TimeProvider` 또는 주입된 `Clock`을 사용한다.
- 절대 시각은 `Instant` 또는 UTC로 변환 가능한 값으로 다루고, 사용자 반복 일정은 `LocalDate`, `LocalTime`, `LocalDateTime`과 명시적인 `ZoneId`로 다룬다.
- 테스트에서는 고정 `Clock` 또는 명시적 상수 시간을 사용한다. 기존 코드의 직접 `now()` 호출은 새 코드의 선례로 삼지 않는다.
- 세부 정책은 로컬 문서 `docs/adr/0003-device-timezone-alarm-policy.md`를 따른다.

## 테스트 규칙

- 구조:

```text
{ClassName}Test
  @Nested {MethodName}Test
    success()
    fail_{reason}()
```

- `@DisplayName`은 문장형으로 작성한다.
- `~테스트` 표현은 사용하지 않는다.
- 권장 prefix:
  - `성공: ...`
  - `실패: ...`
- `// given`, `// when`, `// then` 주석을 포함한다.
- inner class `@DisplayName`은 `"{메서드명} - {기능}"` 형식을 따른다.
- 테스트 엔티티는 `src/test/java/akuma/whiplash/common/fixture/`의 fixture 사용을 우선한다.
- 에러 응답은 `status`, `isSuccess`, `code` 중심으로 검증하고, 문자열보다 ErrorCode enum을 사용한다.
- 변경 가능한 에러 메시지는 계약인 경우에만 검증한다.

## 커밋 규칙

```text
[#이슈] :Emoji: Type: 제목
```

- 제목은 50자 이하로 쓰고 마침표를 붙이지 않는다.
- Type은 `Feature`, `Fix`, `Docs`, `Style`, `Refactor`, `Test`, `Chore` 중 하나를 사용한다.

## 운영 주의사항

- 개인정보, 위치 데이터, 감사 로그, 운영 로그 정책은 로컬 문서 `docs/PRIVACY_AND_AUDIT_LOGGING_POLICY.md`를 따른다.
- 운영 민감 파일은 직접 수정하지 않는다.
- 특히 아래 파일은 수정 전 반드시 재확인한다.
  - `application-prod*`
  - `env.properties`
  - `whiplash-firebase-key.json`
  - `deploy.sh`
  - `Jenkinsfile`

## 자주 쓰는 커맨드

```bash
./gradlew test
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=local'
docker-compose up -d
```
