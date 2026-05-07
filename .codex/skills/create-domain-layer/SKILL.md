---
name: create-domain-layer
description: 이 저장소에서 새 도메인을 만들거나, 기존 도메인에 entity, repository, service, use case, mapper, DTO, exception, controller를 추가 또는 확장할 때 사용한다. 저장소의 레이어 구조와 네이밍 규칙을 따른다.
---

# Create Domain Layer

루트 [AGENTS.md](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/AGENTS.md) 규칙을 전제로 사용한다.

## 권장 패키지 구조

```text
domains/{domain}/
  application/
    dto/
      request/
      response/
      etc/
    mapper/
    usecase/
  domain/
    constant/
    service/
  persistence/
    entity/
    repository/
  presentation/
  exception/
```

도메인에 따라 orchestration이나 비동기 처리가 필요하면 `application/` 아래에 `event/`, `listener/`, `service/`가 추가될 수 있다.

## 권장 생성 순서

1. Entity
2. Repository
3. Domain constants / enums
4. ErrorCode
5. CommandService / QueryService
6. DTO
7. Mapper
8. UseCase
9. Controller

## Entity 규칙

- 프로젝트의 공통 base entity를 우선 활용한다.
- 코드베이스에서 쓰는 `@SuperBuilder`, `@DynamicInsert`, protected no-args constructor 패턴을 우선 맞춘다.
- raw FK id 대신 연관 엔티티 참조를 사용한다.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "member_id", nullable = false)
private MemberEntity member;
```

## Repository 규칙

- 중첩 프로퍼티 탐색은 `_`를 사용한다.

```java
Optional<MemberDeviceEntity> findByMember_IdAndDeviceId(Long memberId, String deviceId);
```

## Service 규칙

- 쓰기와 읽기를 분리한다.
  - `CommandServiceImpl`: `@Service @Transactional`
  - `QueryServiceImpl`: `@Service @Transactional(readOnly = true)`
- 인터페이스와 구현체를 쌍으로 유지한다.

## UseCase 규칙

- UseCase는 command/query service를 조합해 orchestration 한다.
- 특별한 이유가 없으면 트랜잭션 경계는 domain service 중심으로 둔다.

## Controller 규칙

- `@RestController`를 사용한다.
- `ApplicationResponse`를 반환한다.
- 모든 매핑 메서드에 `@CustomErrorCodes`를 선언한다.

## Mapper 규칙

- static mapper 메서드를 사용한다.
- Service나 UseCase 내부에서 변환 builder를 인라인으로 작성하지 않는다.

## 저장소 내 참고 파일

- Entity example: [AlarmEntity.java](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/src/main/java/akuma/whiplash/domains/alarm/persistence/entity/AlarmEntity.java)
- Command service example: [AlarmCommandServiceImpl.java](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/src/main/java/akuma/whiplash/domains/alarm/domain/service/AlarmCommandServiceImpl.java)
- Query service example: [AlarmQueryServiceImpl.java](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/src/main/java/akuma/whiplash/domains/alarm/domain/service/AlarmQueryServiceImpl.java)
- Use case example: [AlarmUseCase.java](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/src/main/java/akuma/whiplash/domains/alarm/application/usecase/AlarmUseCase.java)
- Controller example: [AlarmController.java](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/src/main/java/akuma/whiplash/domains/alarm/presentation/AlarmController.java)
- ErrorCode example: [AlarmErrorCode.java](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/src/main/java/akuma/whiplash/domains/alarm/exception/AlarmErrorCode.java)

## 마무리 전 체크

- import가 레이어 방향을 깨지 않는지 확인한다.
- service/repository 메서드 네이밍이 `AGENTS.md` 규칙을 따르는지 확인한다.
- DTO 필드명이 필요한 곳에서 도메인 prefix를 포함하는지 확인한다.
- 모든 controller 매핑에 `@CustomErrorCodes`가 있는지 확인한다.
