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
- 아키텍처와 운영 정책의 선택 이유는 `docs/adr/README.md`에서 도메인을 찾은 뒤 관련 최신 `Accepted` ADR을 따른다.
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
- 구조적 결정 또는 정책 선택: `docs/adr/README.md`에서 도메인을 찾은 뒤 관련 `docs/adr/{도메인 번호}. {한글 도메인}/{번호}-{결정명}.md`
- 기능별 과거 계획, API 계약, 인수인계: `docs/history/README.md`에서 도메인을 찾은 뒤 관련 `docs/history/{도메인 번호}. {한글 도메인}/{번호}-{기능명}/`
- AI 활용 개발 프로세스의 효과 평가: `docs/AI_DEVELOPMENT_PROCESS_EVALUATION.md`
- 반복 가능한 작업 절차: `.codex/skills/{skill-name}/SKILL.md`
- 에이전트 작업 지침을 변경하거나 품질을 비교: `.codex/skills/evaluate-agent-workflow/SKILL.md`
- 실제 정책 검증: `src/test/`
- 자동 검증과 배포 절차: `.github/workflows/`

공개 저장소에서 Git으로 추적하는 지식 문서는 루트 `CONTEXT.md`와 `docs/README.md`뿐이다. 그 외 `docs/**`와 `CONTEXT.local.md`는 로컬 지식이며 Git에서 제외한다. 이 경계를 임의로 바꾸지 않으며, Git 제외 여부는 보안 경계가 아니므로 API key, token, password, 개인정보 원문, 운영 secret은 어느 문서에도 기록하지 않는다.

## 절대 규칙

- API·DB·외부 연동·도메인 정책을 변경하거나 여러 레이어에 영향을 주는 작업은 먼저 `.codex/skills/development-workflow/SKILL.md`를 적용한다. 문서·주석·국소 수정은 변경 범위에 맞는 경량 검증만 수행한다.
- 예외는 반드시 `ApplicationException.from(ErrorCode)`를 사용한다.
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

## 세부 구현 규칙 라우팅

- ErrorCode, validation, controller 오류 선언을 변경할 때는 `handle-exception` 스킬을 사용한다.
- 도메인 레이어, entity, repository, DTO, mapper, controller를 추가하거나 확장할 때는 `create-domain-layer` 스킬을 사용한다.
- 테스트를 작성하거나 변경할 때는 `write-test-code` 스킬을 사용하고, 테스트 리뷰에는 `review-test` 스킬을 사용한다.
- 각 스킬이 다루는 상세 형식, 예시, 체크리스트는 해당 스킬을 단일 원본으로 삼는다.

## 시간 처리 규칙

- 사용자 시간 계산은 관련 도메인 정책의 명시적인 `ZoneId`를 사용하며 `ZoneId.systemDefault()` 또는 고정 `Asia/Seoul`에 의존하지 않는다.
- 새 비즈니스 로직에서는 `LocalDateTime.now()`, `LocalDate.now()`, `LocalTime.now()`를 직접 호출하지 않고 `TimeProvider` 또는 주입된 `Clock`을 사용한다.
- 절대 시각은 `Instant` 또는 UTC로 변환 가능한 값으로 다루고, 사용자 반복 일정은 `LocalDate`, `LocalTime`, `LocalDateTime`과 명시적인 `ZoneId`로 다룬다.
- 테스트에서는 고정 `Clock` 또는 명시적 상수 시간을 사용한다. 기존 코드의 직접 `now()` 호출은 새 코드의 선례로 삼지 않는다.
- 세부 정책은 로컬 문서 `docs/adr/02. 알람/0003-device-timezone-alarm-policy.md`를 따른다.

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
