# Time Bomb Documentation Guide

이 문서는 공개 저장소에서 사람과 AI 에이전트가 문서의 역할과 Git 추적 범위를 이해하도록 안내한다.

## Git 추적 범위

공개 저장소에서 지식 문서로 추적하는 파일은 다음 두 개다.

```text
CONTEXT.md                        공개 가능한 최소 제품 맥락과 공통 용어
docs/README.md                    문서 역할, 읽기 순서와 보안 원칙
```

루트 `AGENTS.md`는 지식 문서가 아니라 저장소 작업 규칙의 단일 원본으로 별도 추적한다.

## 로컬 문서 구조

`docs/README.md`를 제외한 `docs/**`는 Git에서 제외된 로컬 지식이다. 별도의 `private/` 하위 디렉터리를 만들지 않고 아래 구조를 유지한다.

```text
docs/
├── README.md                     공개 문서 안내
├── PRD.md                        제품 목표와 범위
├── ARCHITECTURE.md               상세 시스템 구조
├── PRIVACY_AND_AUDIT_LOGGING_POLICY.md
│                                 개인정보와 감사·운영 로그 정책
├── AI_DEVELOPMENT_PROCESS_EVALUATION.md
│                                 AI 활용 개발 프로세스의 효과 평가 기준
├── TODO.md                       로컬 backlog
├── adr/                          도메인별 장기 결정과 선택 이유
│   ├── README.md                 도메인 탐색과 전체 시간순 인덱스
│   └── {번호}. {한글 도메인}/   도메인 인덱스와 ADR 문서
└── history/                      도메인별 과거 계획과 인수인계
    ├── README.md                 도메인 탐색과 전체 시간순 인덱스
    └── {번호}. {한글 도메인}/   도메인 인덱스와 번호가 붙은 작업 묶음
```

## 작업 시 읽기 순서

1. 루트 `AGENTS.md`에서 공통 개발 규칙과 문서 라우팅을 확인한다.
2. 루트 `CONTEXT.md`에서 공개 가능한 최소 제품 맥락과 용어를 확인한다.
3. 로컬 문서가 존재하면 `docs/adr/README.md`에서 작업 영역의 최신 `Accepted` ADR을 확인한다. 과거 작업은 `docs/history/README.md`의 도메인별 또는 시간순 인덱스에서 찾는다.
4. 실제 코드, 테스트와 Flyway migration으로 현재 구현 상태를 검증한다.

로컬 문서가 없는 clone에서는 세부 정책을 추정하지 않는다. 코드와 테스트로 확인할 수 없으면 저장소 소유자에게 질문한다.

## 단일 원본

| 확인하려는 내용 | 기준 |
|---|---|
| 공통 개발 규칙 | 루트 `AGENTS.md` |
| 공개 제품명과 공통 용어 | 루트 `CONTEXT.md` |
| 상세 제품 목표와 정책 | 로컬 `docs/PRD.md`와 정책 문서 |
| 아키텍처 결정과 선택 이유 | 로컬 `docs/adr/README.md`에서 연결하는 최신 `Accepted` ADR |
| AI 활용 개발 프로세스의 효과 평가 | 로컬 `docs/AI_DEVELOPMENT_PROCESS_EVALUATION.md` |
| 현재 구현 동작 | source code, test, Flyway migration |
| 과거 결정과 작업 과정 | 로컬 `docs/history/` |

## 공개 저장소 보안 원칙

- `CONTEXT.md`와 이 문서에는 저장소가 공개되어도 괜찮은 내용만 기록한다.
- 상세 아키텍처, 인증·인가 방식, 데이터 구조, 내부 상태값, 임계값, Redis key, 장애 대응, 방어 로직과 운영 토폴로지는 공개 문서에 기록하지 않는다.
- `docs/**`의 Git 제외 여부를 사용자의 명시적 요청 없이 변경하지 않는다.
- Git 제외는 보안 경계가 아니다. API key, password, token, private key, 결제 receipt, 개인정보 원문, 현재 위치 원문과 운영 secret은 로컬 문서에도 기록하지 않는다.
- 실제 값 대신 명백한 placeholder를 사용한다.

## 문서 작성 원칙

- 사실, 결정, 추정과 TODO를 구분한다.
- 현재 상태와 목표 상태가 다르면 각각 명시한다.
- 코드 전체를 복사하지 않고 책임, 계약, 결정 이유와 관련 파일 경로를 남긴다.
- 장기간 유지할 결정은 `docs/adr/{도메인 번호}. {한글 도메인}/{번호}-{결정명}.md`, 특정 작업의 계획과 인수인계는 `docs/history/{도메인 번호}. {한글 도메인}/{번호}-{기능명}/`에 둔다.
- ADR 번호는 도메인별로 다시 시작하지 않고 모든 도메인에서 이어지는 4자리 전역 번호를 사용한다.
- history 작업 번호는 도메인별로 다시 시작하지 않고 모든 도메인에서 이어지는 4자리 전역 번호를 사용한다.
