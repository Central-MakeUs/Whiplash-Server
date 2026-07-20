---
name: handle-exception
description: 이 저장소에서 ErrorCode enum을 추가/수정하거나, 예외 발생 로직, controller 에러 선언, validation과 에러 매핑을 다룰 때 사용한다. 프로젝트의 HttpStatus 및 ErrorCode 규칙을 맞춘다.
---

# Handle Exception

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## ErrorCode 포맷

```java
NAME(HttpStatus.STATUS, "DOMAIN_x001", "~입니다."),
```

허용 상태 코드는 아래와 같다.

- `400`
- `401`
- `403`
- `404`
- `409`

## 코드 범위

| Range | Status |
|---|---|
| `DOMAIN_001~099` | 400 Bad Request |
| `DOMAIN_101~199` | 401 Unauthorized |
| `DOMAIN_301~399` | 403 Forbidden |
| `DOMAIN_401~499` | 404 Not Found |
| `DOMAIN_901~999` | 409 Conflict |

## 예외 발생 방식

```java
throw ApplicationException.from(AlarmErrorCode.ALARM_NOT_FOUND);

alarmRepository.findById(id)
    .orElseThrow(() -> ApplicationException.from(AlarmErrorCode.ALARM_NOT_FOUND));
```

## Controller 선언 규칙

- 모든 controller 매핑 메서드는 `@CustomErrorCodes`로 발생 가능한 에러를 선언한다.
- 새 도메인 ErrorCode enum을 추가하면 `CustomErrorCodes`에 해당 enum 배열 필드가 필요한지 확인한다.

## 주요 파일 위치

- Alarm: [AlarmErrorCode.java](../../../src/main/java/akuma/whiplash/domains/alarm/exception/AlarmErrorCode.java)
- Auth: [AuthErrorCode.java](../../../src/main/java/akuma/whiplash/domains/auth/exception/AuthErrorCode.java)
- Member: [MemberErrorCode.java](../../../src/main/java/akuma/whiplash/domains/member/exception/MemberErrorCode.java)
- Common: [CommonErrorCode.java](../../../src/main/java/akuma/whiplash/global/response/code/CommonErrorCode.java)
- Swagger annotation: [CustomErrorCodes.java](../../../src/main/java/akuma/whiplash/global/annotation/swagger/CustomErrorCodes.java)

## 체크리스트

- 상태 코드가 허용 범위 안에 있는가?
- 코드 번호가 의도한 상태 범위와 맞는가?
- 메시지 문구가 기존 톤과 일관적인가?
- 예외를 `ApplicationException.from(...)`으로 발생시키는가?
- controller 메서드에 새 에러를 `@CustomErrorCodes`로 선언했는가?
