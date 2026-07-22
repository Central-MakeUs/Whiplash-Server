# Claude Rule Adapter

프로젝트 공통 규칙의 단일 원본은 루트 `AGENTS.md`다. 이 파일은 레이어, 예외, 응답, 네이밍, mapper, entity, DTO, 시간, 테스트, Swagger, 커밋 규칙을 중복 정의하지 않는다.

Claude는 작업 전에 아래 순서로 확인한다.

1. `AGENTS.md`
2. `CONTEXT.md`
3. `docs/README.md`
4. 관련 `.claude/skills/`, ADR, history 문서
5. 실제 코드와 테스트

상세 비즈니스 정책은 공개 규칙 파일에 중복하지 않고 루트 `AGENTS.md`가 안내하는 로컬 문서에서 확인한다.
