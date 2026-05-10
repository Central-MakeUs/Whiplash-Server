# Whiplash-Server

이 문서는 이 저장소에서 작업할 때 항상 적용하는 공통 규칙이다.

## 프로젝트 컨텍스트

- 스택: Java 17, Spring Boot 3.5, MySQL 8, Redis 7.2, Firebase FCM
- 주요 도메인: `alarm`, `auth`, `member`, `device`, `payment`, `place`
- 베이스 패키지: `akuma.whiplash`

## 절대 규칙

- 예외는 반드시 `ApplicationException.from(ErrorCode)`를 사용한다.
- 허용 `HttpStatus`는 `400`, `401`, `403`, `404`, `409`만 사용한다.
- API 응답은 반드시 `ApplicationResponse<T>`를 사용한다.
- 레이어 방향은 `presentation -> application -> domain -> persistence`를 지킨다.
- 네이밍 규칙:
  - Service / UseCase: `getXxx`, `createXxx`, `removeXxx`, `modifyXxx`
  - Repository: `findByXxx`, `countByXxx`, `existsByXxx`, `insertXxx`, `updateXxx`, `deleteXxx`

## 레이어 규칙

- 역방향 의존성을 만들지 않는다.
- `presentation`은 controller를 담당한다.
- `application`은 DTO, mapper, use case, event, listener, scheduler, orchestration을 담당한다.
- `domain`은 비즈니스 서비스, enum/constant, 도메인 로직을 담당한다.
- `persistence`는 entity와 repository를 담당한다.

## 예외 및 응답 규칙

- 예외는 `ApplicationException.from(XxxErrorCode.SOME_ERROR)`로 발생시킨다.
- ErrorCode 포맷:

```java
NAME(HttpStatus.STATUS, "DOMAIN_x001", "~입니다.")
```

- Controller 메서드는 `ApplicationResponse.onSuccess(result)` 또는 `ApplicationResponse.onSuccess()`를 반환한다.
- 모든 매핑 메서드에는 `@CustomErrorCodes`를 선언한다.

## Mapper 규칙

- 엔티티/DTO 변환은 `{Domain}Mapper.mapToXxx()` static 메서드로 처리한다.
- Service나 UseCase 내부에서 변환용 builder를 인라인으로 작성하지 않는다.
- Mapper 클래스는 아래 형태를 따른다.

```java
public class XxxMapper {
    private XxxMapper() {
        throw new IllegalArgumentException();
    }
}
```

## Entity 및 Repository 규칙

- FK id 필드보다 `@ManyToOne(fetch = FetchType.LAZY)` 객체 참조를 우선한다.
- 중첩 프로퍼티 Repository 쿼리는 `_`로 경로를 구분한다.

```java
findByMember_IdAndDeviceId(Long memberId, String deviceId)
```

## DTO 규칙

- PK 필드는 `alarmId`, `memberId`처럼 도메인명을 포함한다.
- List 응답 필드명은 `alarms`처럼 복수형 도메인명을 사용한다.
- Enum 값은 기본적으로 `.name()`을 사용한다.
- 날짜/시간 값은 `ISO_LOCAL_DATE` 또는 `ISO_LOCAL_DATE_TIME` 형식을 따른다.
- 페이지네이션 필드는 `page`, `size`, `sortType`을 사용한다.

## 테스트 규칙

- 구조:

```text
{ClassName}Test
  @Nested {MethodName}Test
    success()
    fail_{reason}()
```

- `@DisplayName`은 문장형으로 작성한다.
- `~테스트` 표현은 사용하지 않는다.
- 권장 prefix:
  - `성공: ...`
  - `실패: ...`
- `// given`, `// when`, `// then` 주석을 포함한다.

## 운영 주의사항

- 운영 민감 파일은 직접 수정하지 않는다.
- 특히 아래 파일은 수정 전 반드시 재확인한다.
  - `application-prod*`
  - `env.properties`
  - `whiplash-firebase-key.json`
  - `deploy.sh`
  - `Jenkinsfile`

## 자주 쓰는 커맨드

```bash
./gradlew test
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=local'
docker-compose up -d
```
