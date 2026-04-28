# Rules

## 레이어 의존
`presentation → application → domain → persistence` (역방향 금지)

## 예외
`throw ApplicationException.from(XxxErrorCode.SOME_ERROR);`
ErrorCode enum: `NAME(HttpStatus.STATUS, "Domain_{에러코드맨뒤숫자1개}01", "~입니다.")`
허용 상태: 400 / 401 / 403 / 404 / 409

## 응답
`ApplicationResponse.onSuccess(result)` / `ApplicationResponse.onSuccess()`

## 메서드 명명

| 동작 | Service/UseCase | Repository |
|---|---|---|
| 조회 | `getXxx` | `findByXxx` `countByXxx` `existsByXxx` |
| 생성 | `createXxx` | `insertXxx` |
| 삭제 | `removeXxx` | `deleteXxx` |
| 수정 | `modifyXxx` | `updateXxx` |

## Mapper
- 엔티티 ↔ DTO 변환 `{Domain}Mapper.mapToXxx()` static 메서드를 사용
- Service/UseCase 내 인라인 빌더 직접 사용 금지
- Mapper 클래스: `public class XxxMapper { private XxxMapper() { throw new IllegalArgumentException(); } }`

## 엔티티 FK 참조
- 연관 엔티티는 `Long xxxId` 컬럼이 아닌 `@ManyToOne(fetch = FetchType.LAZY)` 객체 참조 사용
- Repository 쿼리 메서드에서 중첩 프로퍼티 접근은 `_`로 구분
  - ✅ `findByMember_IdAndDeviceId(Long memberId, String deviceId)`
  - ❌ `findByMemberIdAndDeviceId(Long memberId, String deviceId)`

## DTO 필드
- PK: 도메인명 포함 (`alarmId`, `memberId`)
- List 응답 필드명: `{도메인}s` (`alarms`, `tickets`)
- Enum: `.name()` 그대로
- 날짜: `ISO_LOCAL_DATE` / `ISO_LOCAL_DATE_TIME`
- 페이지네이션: `page`, `size`, `sortType`

## 시간 / 타임존
- 기본 비즈니스 타임존은 `Asia/Seoul` 로 통일하고, `ZoneId.systemDefault()` 에 의존하지 않는다.
- 비즈니스 로직, 엔티티 메서드, 스케줄러, JWT 만료 계산에서 `LocalDateTime.now()` / `LocalDate.now()` / `LocalTime.now()` 를 직접 호출하지 않는다.
- 현재 시각이 필요하면 공용 `Clock` 또는 `TimeProvider` 를 주입받아 사용한다.
- 테스트에서 `now()` 직접 호출 금지. 고정 시각(`fixed Clock`) 또는 명시적 상수 시간(`LocalDateTime.of(...)`)을 사용한다.
- 절대 시각이 중요한 값은 `Instant`/epoch 기반으로 다루고, 사용자/도메인 일정 시각은 `LocalDate`, `LocalTime`, `LocalDateTime` 으로 다룬다.
- DB 저장 전 "이 값이 절대 시각인지, 한국 로컬 일정 시각인지"를 먼저 결정한다. 서로 다른 의미의 시간을 같은 `LocalDateTime` 컬럼에 혼용하지 않는다.
- API/이벤트/토큰/외부 연동처럼 서버 밖으로 나가는 시간값은 타임존 해석이 모호하지 않게 한다.
- 경과 시간 측정은 `System.currentTimeMillis()` 또는 `Instant` 사용을 허용하지만, 비즈니스 판단이나 DB 저장 시간 계산에는 사용하지 않는다.
- JPA Auditing(`createdAt`, `updatedAt`)과 수동 저장 시간(`lastLoginAt`, `lastActiveAt`, `processedAt`)은 같은 기준 시간 공급자를 사용한다.
- 자정 경계, 요일 계산, 알람 반복 규칙, 예약 발송 윈도우처럼 날짜가 바뀌는 로직은 타임존을 포함한 테스트 케이스를 반드시 추가한다.

## 테스트 구조
```
{클래스명}Test
  └── @Nested {메서드명}Test
        ├── success()
        └── fail_{이유}()
```

## 테스트 규칙
- `@DisplayName`: 문장형, "~테스트" 금지, `"성공: ..."` / `"실패: ..."` 형식
- inner class `@DisplayName`: `"{메서드명} - {기능}"`
- BDD: `// given` / `// when` / `// then` 주석 필수
- 테스트 엔티티 생성은 테스트 클래스 내부 `buildXxx()` 헬퍼보다 Fixture 메서드 사용을 우선한다.
- Fixture로 표현이 안 되는 케이스는 테스트에서 직접 빌더를 추가하지 말고 `src/test/java/akuma/whiplash/common/fixture/` 아래 Fixture에 `toEntity(...)` / `toMockEntity(...)` 오버로드를 추가한다.
- 권장 형태: `AlarmFixture.ALARM_01.toEntity(member)`, `AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toEntity(alarm)`

## Swagger
모든 매핑 메서드에 `@CustomErrorCodes` 명시 필수

## 커밋
```
[#이슈] :Emoji: Type: 제목(50자↓, 마침표X)
```
Type: Feature✨ / Fix🐛 / Docs📝 / Style🎨 / Refactor♻️ / Test✅ / Chore🔧
