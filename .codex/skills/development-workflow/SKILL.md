---
name: development-workflow
description: 이 저장소에서 영향도에 비례해 신규 기능, 주요 변경, 국소 버그 수정, 리팩토링의 설계·테스트·검증 경로를 선택할 때 사용하는 워크플로우 스킬이다.
---

# Development Workflow

## Overview

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

이 스킬은 기능 개발과 리팩토링 작업을 시작할 때 설계, 구현, 테스트, 검증 순서를 자동으로 끌고 오기 위한 오케스트레이터다. 필요한 세부 규칙은 기존 저장소 스킬을 함께 사용한다.

## Workflow

1. 작업 유형을 분류한다.
   - API 계약, DB schema, Redis key, 외부 API, 결제/FCM/스케줄러, ErrorCode, 도메인 정책 또는 여러 레이어를 변경: 이 워크플로우 전체를 적용한다.
   - 국소 버그 수정·순수 리팩토링: 관련 테스트로 기존 동작을 먼저 확인하고, 설계 문서와 전체 테스트는 영향도에 따라 결정한다.
   - 단순 오타, 주석, 문서, 에이전트 지침 변경: 영향 범위와 문서/링크 검증만 짧게 남긴다. 애플리케이션 테스트와 PLAN.md는 요구하지 않는다.

2. 코드 변경일 때 설계 필요 여부를 결정한다.
   - API 계약, DB schema, Redis key, 외부 API, 결제/FCM/스케줄러, ErrorCode, 도메인 정책, SLO 영향이 있으면 [to-prd](../to-prd/SKILL.md)를 사용해 `docs/history/{도메인 번호}. {한글 도메인}/{0001}-{feature-slug}/PLAN.md`를 생성하거나 갱신한다.
   - 운영 의미가 큰 정책이나 기존 설계와 충돌 가능성이 있으면 [grill-with-docs](../grill-with-docs/SKILL.md)로 문서와 코드를 대조한다.
   - PLAN.md가 필요 없으면 “설계 문서 생략 사유”를 작업 메모나 최종 응답에 남긴다.

3. 코드 변경일 때 테스트 계획을 먼저 세운다.
   - [write-test-code](../write-test-code/SKILL.md)를 사용해 성공/실패 시나리오와 테스트 슬라이스를 정한다.
   - controller, service, repository, Redis, integration 중 변경 책임에 맞는 테스트를 선택한다.
   - SLO 영향이 있으면 [slo-check](../slo-check/SKILL.md)로 latency, availability, error rate 관점의 검증 항목을 정리한다.

4. 코드 변경일 때 Red를 확인한다.
   - 신규 기능, 버그 수정, 정책 변경은 구현 전에 핵심 계약을 검증하는 실패 테스트를 먼저 작성한다.
   - 관련 테스트 명령으로 의도한 이유의 실패를 확인한다.
   - 순수 리팩토링처럼 red가 부자연스러운 경우, 기존 관련 테스트를 먼저 실행해 기준선을 잡고 예외 사유를 남긴다.

5. 구현한다.
   - 레이어, 예외, 응답, mapper, entity/repository, DTO 규칙은 AGENTS.md를 따른다.
   - 새 도메인이나 레이어 확장이 있으면 [create-domain-layer](../create-domain-layer/SKILL.md)를 사용한다.
   - 새 ErrorCode, validation, `@CustomErrorCodes` 변경이 있으면 [handle-exception](../handle-exception/SKILL.md)를 사용한다.

6. 코드 변경이면 Green과 Refactor를 완료한다.
   - 먼저 변경 범위의 관련 테스트를 통과시킨다.
   - 중복, mapper 위치, 트랜잭션 경계, 레이어 의존을 정리한다.
   - 테스트 자체의 구조와 네이밍은 [review-test](../review-test/SKILL.md) 기준으로 확인한다.

7. 변경 범위에 맞는 검증을 실행한다.
   - 애플리케이션 코드를 변경하면 기본 명령은 `./gradlew test`다.
   - 문서·스킬만 변경하면 YAML frontmatter, 상대 링크, 지침 간 충돌과 diff를 검토한다.
   - 전체 테스트를 실행할 수 없으면 미실행 사유, 대신 실행한 관련 테스트, 남은 리스크를 최종 응답과 PR 설명에 남긴다.
   - CI/CD나 배포 영향이 있으면 `.github/workflows`, 운영 민감 파일, 환경 설정 변경 여부를 재확인한다.

8. 마무리 산출물을 정리한다.
   - 변경된 설계 문서, 구현 파일, 테스트 파일, 실행한 테스트와 결과를 요약한다.
   - PR 본문이 필요하면 [write-pr-description](../write-pr-description/SKILL.md)를 사용한다.
   - 다음 작업자에게 넘겨야 할 미완료 상태가 있으면 [handoff](../handoff/SKILL.md)를 사용한다.

## Completion Checklist

- 코드 변경에서 PLAN.md 생성/갱신 필요 여부를 판단했는가?
- 코드 변경에서 PLAN.md가 필요 없으면 생략 사유를 남겼는가?
- 코드 변경에서 red 테스트 또는 기존 기준선 테스트를 먼저 확인했는가?
- 코드 변경에서 관련 테스트를 통과시켰는가?
- 코드 변경에서 `./gradlew test`를 실행했거나, 미실행 사유와 리스크를 남겼는가?
- 문서·스킬만 변경했다면 링크와 지침 간 충돌을 검토했는가?
- 최종 응답에 문서, 구현, 테스트, 검증 결과가 함께 정리되었는가?

## Related Skills

- [to-prd](../to-prd/SKILL.md): 기능 요청과 설계를 PLAN.md로 정리한다.
- [write-test-code](../write-test-code/SKILL.md): 테스트 작성과 Red-Green-Refactor를 수행한다.
- [review-test](../review-test/SKILL.md): 테스트 컨벤션을 점검한다.
- [slo-check](../slo-check/SKILL.md): 운영성/SLO 영향을 점검한다.
- [write-pr-description](../write-pr-description/SKILL.md): PR 본문을 작성한다.
