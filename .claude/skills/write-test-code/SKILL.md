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

## FCM 테스트
`local` 프로파일 → `MockFcmService(@Primary)` 자동 등록, FCM 실제 호출 없음
`INVALID_` prefix 토큰 → 실패 처리

## 체크리스트
- [ ] 어노테이션 목적에 맞는가?
- [ ] `@Nested` inner class가 메서드마다 있는가?
- [ ] `success()` / `fail_{이유}()` 네이밍인가?
- [ ] `@DisplayName` 문장형, "~테스트" 없는가?
- [ ] `// given` / `// when` / `// then` 있는가?
- [ ] 서비스→`toMockEntity()`, Persistence→`toEntity()` 구분했는가?
