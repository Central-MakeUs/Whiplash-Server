---
name: to-prd
description: 이 저장소에서 기능 요청, 정책 변경, API 변경, 앱 요구사항을 docs/{번호}.{기능명}/PLAN.md 형식의 PRD 또는 구현 계획으로 정리할 때 사용한다.
---

# To PRD

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 모호한 기능 요청을 구현 가능한 계획으로 바꾼다.
- API 계약, DTO, 예외, 테스트, 마이그레이션 여부를 미리 결정한다.
- 기존 `docs/{번호}.{기능명}/PLAN.md` 흐름과 맞춘다.

## 작성 순서

1. 현재 동작과 요구사항의 gap을 정리한다.
2. 변경될 API, request, response, validation을 명시한다.
3. 레이어별 책임을 나눈다.
4. 필요한 ErrorCode와 `@CustomErrorCodes` 변경을 정리한다.
5. DB schema, migration, Redis key, 외부 API 영향 여부를 확인한다.
6. latency, availability, error rate 관점의 SLO 영향을 확인한다.
7. 테스트 계획을 controller, service, repository, integration 단위로 나눈다.
8. 결정된 사항과 아직 결정이 필요한 사항을 분리한다.

## PLAN.md 권장 구조

```text
# {기능명} 구현 계획

## 1. 현재 상태와 요구사항
## 2. API 계약
## 3. 응답/요청 DTO 정책
## 4. 레이어별 변경 방향
## 5. 예외 및 검증 정책
## 6. SLO / 운영 관점
## 7. 테스트 계획
## 8. 결정 사항 / 남은 질문
```

## 프로젝트 규칙 반영

- API 응답은 `ApplicationResponse<T>` 기준으로 작성한다.
- 예외는 [handle-exception](../handle-exception/SKILL.md) 규칙을 따른다.
- 새 도메인 구조는 [create-domain-layer](../create-domain-layer/SKILL.md) 규칙을 따른다.
- SLO 영향은 [slo-check](../slo-check/SKILL.md) 규칙을 따른다.
- 테스트 계획은 [write-test-code](../write-test-code/SKILL.md) 규칙을 따른다.

## 출력 방식

- 구현자가 추가 결정을 하지 않아도 될 정도로 구체적으로 쓴다.
- 불확실한 내용은 추정하지 말고 “결정 필요”로 남긴다.
- 문서를 실제로 생성할 때는 기존 `docs/` 번호 순서를 확인한 뒤 다음 번호를 사용한다.
