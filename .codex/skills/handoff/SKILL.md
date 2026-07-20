---
name: handoff
description: 이 저장소에서 진행 중인 작업, 설계 결정, 변경 파일, 테스트 상태, 남은 TODO를 다음 작업자나 다음 세션에 넘길 인수인계 문서로 정리할 때 사용한다.
---

# Handoff

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 다음 작업자가 맥락을 다시 캐지 않도록 현재 상태를 압축한다.
- 결정된 사항과 미완료 사항을 분리한다.
- 테스트와 운영 주의사항을 명확히 남긴다.

## 포함할 내용

1. 작업 목적과 현재 상태
2. 주요 결정 사항
3. 변경된 파일과 각 파일의 의도
4. 남은 TODO
5. 실행한 테스트와 결과
6. 아직 실행하지 못한 테스트와 이유
7. 운영 민감 파일 또는 배포 주의사항

## 권장 형식

```markdown
# Handoff: {작업명}

## Context
{왜 이 작업을 하는지}

## Done
- {완료한 작업}

## Decisions
- {결정 사항}

## Changed Files
- `{path}`: {변경 의도}

## Remaining
- {남은 작업}

## Verification
- {실행한 테스트와 결과}

## Cautions
- {주의사항}
```

## 주의사항

- 운영 민감 파일(`application-prod*`, `env.properties`, `whiplash-firebase-key.json`, `deploy.sh`, `Jenkinsfile`) 관련 내용은 반드시 명시한다.
- 추정과 사실을 섞지 않는다.
- 실패한 테스트는 실패 원인과 다음 확인 지점을 함께 적는다.
