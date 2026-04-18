# Rules

## 레이어 의존
`presentation → application → domain → persistence` (역방향 금지)

## 예외
`throw ApplicationException.from(XxxErrorCode.SOME_ERROR);`
ErrorCode enum: `NAME(HttpStatus.STATUS, "Domain_x001", "~입니다.")`
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

## Swagger
모든 매핑 메서드에 `@CustomErrorCodes` 명시 필수

## 커밋
```
[#이슈] :Emoji: Type: 제목(50자↓, 마침표X)
```
Type: Feature✨ / Fix🐛 / Docs📝 / Style🎨 / Refactor♻️ / Test✅ / Chore🔧
