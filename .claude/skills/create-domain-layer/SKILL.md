---
name: create-domain-layer
description: >
  Whiplash 프로젝트에서 새 도메인 생성, 레이어 추가(Entity/Repository/Service/UseCase/Controller) 시 사용.
  "도메인 만들어줘", "레이어 추가해줘", "UseCase 만들어줘", "Service 구현해줘", "Controller 추가해줘" 요청 시 반드시 참고.
---

# 도메인 생성 스킬

## 패키지 구조
```
domains/{domain}/
  application/
    dto/        # request, response (record)
    mapper/     # static 변환 메서드
    usecase/    # @UseCase 클래스
  domain/
    constant/   # enum
    service/    # CommandService + QueryService (인터페이스 + Impl)
  persistence/
    entity/     # BaseTimeEntity 상속, @SuperBuilder @DynamicInsert
    repository/ # JpaRepository
  presentation/ # Controller
  exception/    # {Domain}ErrorCode
```

## 생성 순서
Entity → Repository → enum → ErrorCode → Service → DTO → Mapper → UseCase → Controller

## 핵심 패턴

### Entity
`BaseTimeEntity` 상속, `@SuperBuilder`, `@DynamicInsert`, `@NoArgsConstructor(access = PROTECTED)`

연관 엔티티는 ID(Long) 컬럼 대신 `@ManyToOne` 객체 참조를 사용한다:
```java
// ❌ 금지
@Column(name = "member_id", nullable = false)
private Long memberId;

// ✅ 올바른 패턴
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "member_id", nullable = false)
private MemberEntity member;
```

### Repository
중첩 엔티티의 ID로 조회할 때는 `_`로 프로퍼티 경로를 구분한다:
```java
// member 필드의 id로 조회
Optional<MemberDeviceEntity> findByMember_IdAndDeviceId(Long memberId, String deviceId);
```

### Mapper
엔티티 ↔ DTO 변환은 반드시 Mapper의 static 메서드로 처리한다. Service 내 인라인 빌더 금지.
```java
public class AuthMapper {
    private AuthMapper() { throw new IllegalArgumentException(); }

    // SocialMemberInfo → MemberEntity (신규 회원 저장용)
    public static MemberEntity mapToMemberEntity(SocialMemberInfo info) { ... }

    // MemberEntity + request → MemberDeviceEntity (연관 엔티티 포함)
    public static MemberDeviceEntity mapToMemberDeviceEntity(MemberEntity member, SocialLoginRequest request) {
        return MemberDeviceEntity.builder()
            .member(member)   // Long memberId ❌ → MemberEntity ✅
            .deviceId(request.deviceId())
            ...
            .build();
    }

    // MemberEntity → LoginResponse.MemberInfo
    public static MemberInfo mapToMemberInfo(MemberEntity member, boolean isNewMember) { ... }
}
```

### CommandService / QueryService
- Command: `@Service @Transactional` — 상태 변경
- Query: `@Service @Transactional(readOnly = true)` — 조회
- 인터페이스 + Impl 쌍으로 구성

### UseCase
`@UseCase` (= `@Component`), CommandService + QueryService 조합

### Controller
`@RestController`, `@CustomErrorCodes` 필수, `ApplicationResponse.onSuccess(...)` 반환

## 참고 파일
- Entity: `domains/alarm/persistence/entity/AlarmEntity.java`
- Service: `domains/alarm/domain/service/AlarmCommandServiceImpl.java`
- UseCase: `domains/alarm/application/usecase/AlarmUseCase.java`
- Controller: `domains/alarm/presentation/AlarmController.java`
- ErrorCode: `domains/alarm/exception/AlarmErrorCode.java`
