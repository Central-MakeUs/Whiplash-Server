# Time Bomb Server - Claude Adapter

이 파일은 Claude 전용 진입점이다. 프로젝트 규칙을 별도로 정의하지 않는다.

## 작업 시작 시 읽기

1. 루트 `AGENTS.md`: 저장소 공통 규칙과 문서 라우팅의 단일 원본
2. 루트 `CONTEXT.md`: 현재 제품명과 공개 가능한 공통 용어
3. `docs/README.md`: 작업 종류별 문서 선택 방법
4. 작업과 관련된 코드, 테스트, ADR, history 문서

## 우선순위

- `.claude`의 지침, skill, agent가 루트 `AGENTS.md`와 충돌하면 `AGENTS.md`를 따른다.
- 제품명과 도메인 용어가 충돌하면 `CONTEXT.md`를 따른다.
- 실제 코드와 문서가 충돌하면 임의로 수정하지 말고 차이를 보고한다.

## Claude 전용 자산

- 반복 작업: `.claude/skills/`
- 리뷰 역할: `.claude/agents/`
- 자동 검사: `.claude/hooks/`, `.claude/settings.json`

이 자산은 도구별 실행 방법만 제공하며 공통 개발 규칙의 단일 원본이 아니다.
