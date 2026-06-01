---
name: write-test-code
description: 이 저장소에서 controller, service, repository, Redis, integration 테스트를 작성하거나 수정할 때 사용한다. TDD red-green-refactor 흐름, 어노테이션 선택, fixture 사용, nested 테스트 구조, DisplayName 규칙, 현재 코드베이스의 테스트 패턴을 다룬다.
---

# Write Test Code

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 기능 추가나 버그 수정을 테스트로 먼저 고정한다.
- Whiplash 테스트 컨벤션에 맞는 테스트를 작성한다.
- 실패 테스트를 green으로 만든 뒤 중복과 구조를 정리한다.

## TDD 진행 순서

가능한 경우 red-green-refactor 흐름을 따른다.

1. 요구사항을 성공/실패 시나리오로 나눈다.
2. 가장 핵심 계약을 검증하는 테스트 하나를 먼저 작성한다.
3. SLO 영향이 있는 기능이면 latency, availability, error rate 관점의 검증 필요성을 확인한다.
4. 테스트가 의도한 이유로 실패하는지 확인한다.
5. 최소 구현으로 green을 만든다.
6. 기존 규칙에 맞게 리팩터링한다.
7. 필요한 실패 케이스와 경계값 테스트를 추가한다.

## 구현 시 지킬 규칙

- 예외는 `ApplicationException.from(ErrorCode)`로 발생시킨다.
- 새 ErrorCode나 validation이 필요하면 [handle-exception](../handle-exception/SKILL.md)를 따른다.
- 새 도메인 레이어가 필요하면 [create-domain-layer](../create-domain-layer/SKILL.md)를 따른다.
- 성능 약속이나 운영 관측이 필요한 기능이면 [slo-check](../slo-check/SKILL.md)를 따른다.
- 테스트 통과만을 위해 production 코드를 우회하거나 테스트 전용 분기를 만들지 않는다.

## 테스트 어노테이션 선택

| Purpose | Preferred annotation |
|---|---|
| Controller request/response validation | `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` |
| Service business logic | `@ExtendWith(MockitoExtension.class)` |
| Repository / JPQL | `@PersistenceTest` |
| End-to-end app flow | `@IntegrationTest` |
| Redis slice | `@DataRedisTest` + `RedisContainerInitializer` |

`@PersistenceTest`, `@IntegrationTest`, `@MockitoBean`은 현재 코드베이스에서 이미 사용 중이다.

## Controller 테스트 가이드

- 기존 테스트 스타일에 맞춰 security 관련 협력 객체는 제외 대상을 검토한다.
- 협력 객체는 `@MockitoBean`으로 주입한다.
- `SecurityContext`를 수동 설정했다면 테스트 후 정리한다.

## 구조 및 네이밍

```text
{ClassName}Test
  @Nested {MethodName}Test
    success()
    fail_{reason}()
```

- Root class name: `{OriginalClassName}Test`
- Nested class name: `{MethodName}Test`
- Success method: `success()`
- Failure method: `fail_{businessReason}()`

## DisplayName 규칙

- 한국어 문장형으로 작성한다.
- `~테스트`는 사용하지 않는다.
- 아래 형식을 사용한다.
  - `성공: ...`
  - `실패: ...`
- Nested class DisplayName은 `"{메서드명} - {기능}"` 형식을 따른다.

## BDD 주석

항상 아래 주석을 포함한다.

```java
// given
// when
// then
```

## 에러 검증 규칙

- 컨트롤러/통합 테스트에서 에러 응답은 `status`, `isSuccess`, `code` 중심으로 검증한다.
- 에러 코드 검증은 직접 문자열보다 ErrorCode enum의 `getCustomCode()`를 우선 사용한다.
- 에러 메시지(`message`)는 사용자 노출 문구라 변경 가능성이 높으므로 String literal로 직접 검증하지 않는다.
- 메시지까지 계약으로 보장해야 하는 경우에만 ErrorCode enum의 `getMessage()`로 검증한다.
- 서비스 단위 테스트에서 `ApplicationException`을 검증할 때는 `hasMessage(...)`보다 `getCode()`로 ErrorCode enum을 비교한다.

```java
// Controller / integration
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.isSuccess").value(false))
.andExpect(jsonPath("$.code").value(ALARM_DELETE_REQUIRES_PAYMENT.getCustomCode()));

// Service unit
assertThatThrownBy(() -> alarmCommandService.removeAlarmByAd(memberId, alarmId, request))
    .isInstanceOfSatisfying(ApplicationException.class, e ->
        assertThat(e.getCode()).isEqualTo(ALARM_DELETE_REQUIRES_PAYMENT)
    );
```

## Fixture 규칙

- 서비스 단위 테스트는 `toMockEntity()`를 사용한다.
- Persistence / integration 테스트는 `toEntity()`를 사용한다.

사용 가능한 fixture:

- [MemberFixture.java](../../../src/test/java/akuma/whiplash/common/fixture/MemberFixture.java)
- [MemberDeviceFixture.java](../../../src/test/java/akuma/whiplash/common/fixture/MemberDeviceFixture.java)
- [AlarmFixture.java](../../../src/test/java/akuma/whiplash/common/fixture/AlarmFixture.java)
- [AlarmOccurrenceFixture.java](../../../src/test/java/akuma/whiplash/common/fixture/AlarmOccurrenceFixture.java)
- [PaymentFixture.java](../../../src/test/java/akuma/whiplash/common/fixture/PaymentFixture.java)

## FCM 관련 메모

- `local`, `test` 프로파일에서는 mock 기반 동작이 포함된다.
- `INVALID_` prefix 토큰은 실패 케이스로 활용한다.

## 저장소 내 참고 테스트

- Controller test: [AlarmControllerTest.java](../../../src/test/java/akuma/whiplash/domains/alarm/presentation/AlarmControllerTest.java)
- Integration test: [AlarmControllerIntegrationTest.java](../../../src/test/java/akuma/whiplash/domains/alarm/presentation/AlarmControllerIntegrationTest.java)
- Repository test: [AlarmRepositoryTest.java](../../../src/test/java/akuma/whiplash/domains/alarm/persistence/repository/AlarmRepositoryTest.java)

## 마무리 전 체크

- 실패 테스트가 먼저 있었고, 최종적으로 관련 테스트가 통과하는가?
- 성공 케이스와 의미 있는 실패 케이스가 포함되는가?
- 가장 적절한 테스트 슬라이스를 선택했는가?
- 가능한 경우 ad-hoc builder 대신 fixture helper를 사용했는가?
- nested 구조와 네이밍을 정확히 맞췄는가?
- `given/when/then` 주석을 포함했는가?
- 에러 응답은 code 중심으로, 서비스 예외는 ErrorCode enum 중심으로 검증했는가?
