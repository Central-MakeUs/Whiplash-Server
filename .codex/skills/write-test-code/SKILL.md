---
name: write-test-code
description: 이 저장소에서 controller, service, repository, Redis, integration 테스트를 작성하거나 수정할 때 사용한다. 어노테이션 선택, fixture 사용, nested 테스트 구조, DisplayName 규칙, 현재 코드베이스의 테스트 패턴을 다룬다.
---

# Write Test Code

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

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

- 가장 적절한 테스트 슬라이스를 선택했는가?
- 가능한 경우 ad-hoc builder 대신 fixture helper를 사용했는가?
- nested 구조와 네이밍을 정확히 맞췄는가?
- `given/when/then` 주석을 포함했는가?
- 에러 응답은 code 중심으로, 서비스 예외는 ErrorCode enum 중심으로 검증했는가?
