# agents.md

## 🧼 CleanCodeAgent
**목표**: 클린 코드 원칙에 기반하여 가독성 있고 유지보수 쉬운 코드 작성 유도

**역할**:
- 변수, 메서드, 클래스명은 의미 있는 이름을 사용하고 축약어는 지양합니다.
- Controller/Service는 `getUser`, `createUser`, `removeUser` 등 동사 기반으로 명명하고,
  Repository는 `findByUser`, `insertUser`, `deleteUser`처럼 명확히 구분합니다.
- 한 메서드는 한 가지 역할만 하도록 구조화합니다.
- 비즈니스 로직과 DB 접근 로직이 명확히 분리되었는지 확인합니다.

---

## 🧪 TestAgent
**목표**: 테스트 코드가 정책과 동작을 명확하게 설명하고, 시나리오 기반으로 구성되도록 합니다.

**역할**:
- 테스트명은 `@DisplayName`을 사용하고, "음료를 추가하면 주문 목록에 담긴다"와 같이 **문장형**으로 작성합니다.
- 테스트는 `Given-When-Then` 구조로 작성되었는지 확인합니다.
- 테스트 설명은 도메인 관점에서 작성되었는지 확인합니다.
  - ex) "영업 시작 전에는 주문을 생성할 수 없다."

---

## 📦 DTOAgent
**목표**: 요청/응답 객체의 필드명이 일관되고 의미가 명확하도록 유지합니다.

**역할**:
- 요청/응답 객체의 필드명은 DB 컬럼명을 기준으로 하며, 필요시 `userNickname`, `adminNickname` 등 식별자를 붙입니다.
- 모든 PK는 `alarmId`, `userId`처럼 도메인명을 포함하여 명명합니다.
- 리스트 응답 필드는 `alarms`, `tickets`처럼 복수형 도메인명으로 작성합니다.
- Enum은 `.name()` 값 그대로, 날짜는 ISO_LOCAL_DATE 또는 ISO_LOCAL_DATE_TIME 형식으로 반환합니다.

---

## 🧾 CommitAgent

**목표**  
Git 커밋 메시지가 일관되고 이력을 추적하기 쉬운 형태로 작성되도록 합니다.

**역할**
- 커밋 메시지 포맷은 아래와 같은 구조를 따릅니다:

  ```text
  [#이슈번호] :이모지: Type: 커밋 제목

  <body>
  - 변경된 파일 및 설명

  <footer>
  - 해결: #이슈번호
  ```
  
| Type | Emoji | Description |
| --- | --- | --- |
| Feature | ✨ (sparkles) | 새로운 기능 추가 |
| Fix | 🐛 (bug) | 버그 수정 |
| Docs | 📝 (memo) | 문서 수정 |
| Style | 🎨 (art) | 코드 포맷팅, 세미콜론 누락 등 구조 변경 없음 |
| Refactor | ♻️ (recycle) | 리팩토링 (동작 변경 없이 코드 개선 포함) |
| Test | ✅ (white_check_mark) | 테스트 코드 추가 또는 수정 |
| Chore | 🔧 (wrench) | 빌드 설정, 패키지 관리 등 잡일 처리 |
| Wip | 🚧 (construction) | 진행 중인 작업 커밋 (가능하면 지양) |
| Rename | 🚚 (truck) | 파일 또는 폴더명 변경 |

---

## 🗂️ ArchitectureAgent

**목표**: 도메인 기반 구조를 따르고 레이어별 책임이 명확하게 구분되도록 합니다.

**역할**:

- domains 하위에 도메인 단위로 분리되어 있는지 확인 (`user`, `ticket` 등)
- 각 디렉토리는 역할에 따라 적절히 분리되어야 합니다:
    - `application`: DTO, Mapper, UseCase
    - `domain`: 비즈니스 상수 및 서비스
    - `persistence`: entity, repository
    - `presentation`: controller
- presentation → usecase → service → repository 방향을 지켜야 합니다.

---

## ⚠️ ErrorCodeAgent

**목표**: 에러 코드가 일관된 포맷을 따르며 Swagger에도 명확히 명시되도록 합니다.

**역할**:

- 에러 코드는 `Domain_x0n` 형태로 작성되었는지 확인합니다.
- 각 도메인에서 발생 가능한 에러는 `@CustomErrorCodes`로 명시합니다.
- 허용된 HttpStatus만 사용합니다: 400, 401, 403, 404, 409
- message는 `~입니다.` 형태의 문장으로 작성되어야 합니다.
- 애플리케이션에서 예외를 발생시킬 때 throw ApplicationException.from(CommonErrorCode.INTERNAL_SERVER_ERROR); 와 같이 ApplicationException의 from 메서드에 BaseErrorCode를 implements한 Enum의 value를 넣어 발생시킵니다.

---

## 💬 StyleAgent

**목표**: 코드 스타일, 정렬, 불필요한 주석, 일관성 없는 포맷을 자동 감지하고 지적합니다.

**역할**:

- 세미콜론 누락, 들여쓰기 불일치, import 순서 등의 형식 오류를 검토합니다.
- 사용되지 않는 변수나 주석은 제거를 제안합니다.
- 한 줄이 너무 길거나 중첩이 과도한 경우 구조 개선을 권장합니다.
