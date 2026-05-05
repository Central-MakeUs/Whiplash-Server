---
name: write-test-code
description: >
  프로젝트 테스트 작성 시 어노테이션 선택, Fixture 활용, FCM 주의사항 안내.
  "테스트 작성해줘", "단위 테스트", "통합 테스트", "@WebMvcTest", "Mock 테스트" 요청 시 반드시 참고.
---

# 테스트 코드 작성 스킬

## 어노테이션 선택

| 목적 | 어노테이션 |
|---|---|
| 컨트롤러 요청/검증 | `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` |
| 서비스 비즈니스 로직 | `@ExtendWith(MockitoExtension.class)` |
| Repository / JPQL | `@PersistenceTest` (MySQL Testcontainer) |
| 전체 플로우 | `@IntegrationTest` (MySQL + Redis Testcontainer) |
| Redis 슬라이스 | `@DataRedisTest` + `RedisContainerInitializer` |

## @WebMvcTest 필수 설정
```java
excludeFilters = @Filter(type = ASSIGNABLE_TYPE,
    classes = {SecurityConfig.class, JwtAuthenticationFilter.class})
// 협력 빈: @MockitoBean
// SecurityContext 수동 설정 후 @AfterEach에서 clearContext()
```

## Fixture 사용 규칙

| 테스트 종류 | 메서드 |
|---|---|
| 서비스 단위 테스트 | `MemberFixture.MEMBER_1.toMockEntity()` (id 포함) |
| Persistence / Integration | `memberRepository.save(MemberFixture.MEMBER_1.toEntity())` |

파일 위치: `src/test/java/akuma/whiplash/common/fixture/`
- `MemberFixture` MEMBER_1~20
- `AlarmFixture` ALARM_01~20
- `AlarmOccurrenceFixture`

### Fixture 작성 원칙
- 테스트 클래스 내부에 `buildOccurrence()`, `buildAlarm()` 같은 엔티티 생성 헬퍼를 만들지 않는다.
- 엔티티가 필요하면 먼저 Fixture의 `toMockEntity()` / `toEntity()`를 사용한다.
- 기존 Fixture로 표현이 안 되는 케이스가 있으면 테스트 안에서 빌더를 직접 쓰지 말고 Fixture에 `toEntity(...)` 오버로드나 전용 메서드를 추가한다.
- 테스트 본문은 아래처럼 Fixture 호출만으로 읽히는 형태를 우선한다.

```java
AlarmEntity alarm = AlarmFixture.ALARM_01.toMockEntity();
AlarmEntity savedAlarm = alarmRepository.save(AlarmFixture.ALARM_01.toEntity(member));
AlarmOccurrenceEntity occurrence =
    AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toEntity(savedAlarm);
```

## FCM 테스트
`local` 프로파일 → `MockFcmService(@Primary)` 자동 등록, FCM 실제 호출 없음
`INVALID_` prefix 토큰 → 실패 처리

## 에러 응답 검증 규칙

- 컨트롤러/통합 테스트에서 에러 응답은 `status`, `isSuccess`, `code` 중심으로 검증한다.
- 에러 메시지(`message`)는 사용자 노출 문구라 변경 가능성이 높으므로 String literal로 직접 검증하지 않는다.
- 메시지 검증이 꼭 필요하면 직접 문자열 대신 ErrorCode enum의 `getMessage()`를 사용한다.
- 에러 코드도 직접 문자열보다 ErrorCode enum의 `getCustomCode()`를 우선 사용한다.

```java
// ✅ 권장
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.isSuccess").value(false))
.andExpect(jsonPath("$.code").value(ALARM_DELETE_REQUIRES_PAYMENT.getCustomCode()));

// ⚠️ 메시지까지 계약으로 보장해야 하는 경우에만 사용
.andExpect(jsonPath("$.message").value(ALARM_DELETE_REQUIRES_PAYMENT.getMessage()));

// ❌ 지양: 문구 변경만으로 테스트가 깨짐
.andExpect(jsonPath("$.message").value("결제 삭제가 필요한 알람입니다."));
```

## 체크리스트
- [ ] 어노테이션 목적에 맞는가?
- [ ] `@Nested` inner class가 메서드마다 있는가?
- [ ] `success()` / `fail_{이유}()` 네이밍인가?
- [ ] `@DisplayName` 문장형, "~테스트" 없는가?
- [ ] `// given` / `// when` / `// then` 있는가?
- [ ] 서비스→`toMockEntity()`, Persistence→`toEntity()` 구분했는가?
- [ ] 테스트 내부 생성 헬퍼 대신 Fixture 메서드를 사용했는가?
- [ ] 에러 응답 검증에서 메시지 String literal 대신 ErrorCode enum 또는 code 중심 검증을 사용했는가?
