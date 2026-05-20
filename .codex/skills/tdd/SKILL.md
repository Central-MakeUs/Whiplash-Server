---
name: tdd
description: 이 저장소에서 기능 추가나 버그 수정을 테스트 주도 방식(red-green-refactor)으로 진행하고, Whiplash 테스트 컨벤션에 맞는 실패 테스트부터 작성할 때 사용한다.
---

# TDD

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 구현 전에 실패하는 테스트로 요구사항을 고정한다.
- 가장 작은 구현으로 테스트를 통과시킨다.
- 통과 후 중복과 구조를 정리한다.

## 진행 순서

1. 요구사항을 성공/실패 시나리오로 나눈다.
2. 가장 핵심 계약을 검증하는 테스트 하나를 먼저 작성한다.
3. SLO 영향이 있는 기능이면 latency, availability, error rate 관점의 검증 필요성을 확인한다.
4. 테스트가 의도한 이유로 실패하는지 확인한다.
5. 최소 구현으로 green을 만든다.
6. 기존 규칙에 맞게 리팩터링한다.
7. 필요한 실패 케이스와 경계값 테스트를 추가한다.

## 테스트 선택

- Controller 요청/응답 검증: `@WebMvcTest`
- Service 비즈니스 로직: `@ExtendWith(MockitoExtension.class)`
- Repository / JPQL: `@PersistenceTest`
- End-to-end 흐름: `@IntegrationTest`
- Redis slice: `@DataRedisTest` + `RedisContainerInitializer`

상세한 어노테이션, fixture, DisplayName, nested 구조는 [write-test-code](../write-test-code/SKILL.md)를 따른다.

## 구현 시 지킬 규칙

- 예외는 `ApplicationException.from(ErrorCode)`로 발생시킨다.
- 새 ErrorCode나 validation이 필요하면 [handle-exception](../handle-exception/SKILL.md)를 따른다.
- 새 도메인 레이어가 필요하면 [create-domain-layer](../create-domain-layer/SKILL.md)를 따른다.
- 성능 약속이나 운영 관측이 필요한 기능이면 [slo-check](../slo-check/SKILL.md)를 따른다.
- 테스트 통과만을 위해 production 코드를 우회하거나 테스트 전용 분기를 만들지 않는다.

## 완료 기준

- 실패 테스트가 먼저 있었고, 최종적으로 관련 테스트가 통과한다.
- 성공 케이스와 의미 있는 실패 케이스가 포함된다.
- 테스트 이름은 `success()` 또는 `fail_{reason}()` 형식을 따른다.
- `// given`, `// when`, `// then` 주석을 포함한다.
