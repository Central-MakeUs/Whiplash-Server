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
