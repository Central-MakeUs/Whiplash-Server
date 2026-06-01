---
name: diagnose
description: 이 저장소에서 Spring Boot, JPA, Redis, Firebase FCM, 인증/인가, 테스트 실패, 운영성 버그를 재현하고 원인 분석부터 최소 수정 및 회귀 테스트까지 진행할 때 사용한다.
---

# Diagnose

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 버그를 먼저 재현한다.
- 로그, 테스트, 설정, 데이터 흐름을 근거로 가설을 세운다.
- 최소 수정으로 해결하고 회귀 테스트를 남긴다.

## 진단 순서

1. 증상과 기대 동작을 한 문장으로 정리한다.
2. 관련 controller, use case, service, repository, config, test를 검색한다.
3. 실패를 재현할 수 있는 테스트 또는 명령을 먼저 찾는다.
4. 가설을 1~3개로 좁히고, 각 가설을 확인할 근거를 수집한다.
5. 가장 작은 범위로 수정한다.
6. 같은 문제가 다시 생기지 않도록 회귀 테스트를 작성하거나 보강한다.
7. 수정 후 관련 테스트를 실행하고 남은 리스크를 정리한다.

## 영역별 확인 포인트

### Spring MVC / Security

- `@Valid`, `@RequestParam`, `@PathVariable` 검증이 누락되어 500으로 번지는지 확인한다.
- 인증 사용자와 요청의 `memberId`가 일치해야 하는 흐름인지 확인한다.
- Controller 응답은 `ApplicationResponse<T>`를 유지한다.

### JPA

- Lazy loading, 트랜잭션 경계, N+1 가능성을 확인한다.
- Repository 쿼리명은 `findByXxx`, `existsByXxx`, `countByXxx` 규칙을 따른다.
- 중첩 프로퍼티는 `_` 경로 표기를 사용한다.

### Redis

- TTL 누락, key prefix 충돌, `keys()` 사용 여부를 확인한다.
- local/test profile에서 Redis 대체 설정이나 Testcontainers 사용 여부를 확인한다.

### Firebase FCM

- local/test profile의 mock 동작과 운영 설정을 구분한다.
- FCM token, credential, payload가 로그에 노출되지 않도록 확인한다.

## 관련 skill

- 테스트 작성은 [write-test-code](../write-test-code/SKILL.md)를 따른다.
- 예외 추가나 변경은 [handle-exception](../handle-exception/SKILL.md)를 따른다.
- 장애나 성능 문제가 SLO 위반인지 판단해야 하면 [slo-check](../slo-check/SKILL.md)를 함께 사용한다.
- 성능 리스크가 의심되면 [review-performance](../review-performance/SKILL.md)를 함께 사용한다.
- 보안 리스크가 의심되면 [review-security](../review-security/SKILL.md)를 함께 사용한다.

## 출력 방식

- 원인, 근거, 수정 방향, 검증 결과 순으로 짧게 정리한다.
- 아직 확인하지 못한 가설은 추정이라고 명시한다.
