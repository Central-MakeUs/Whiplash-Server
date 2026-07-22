---
name: handle-exception
description: >
  Time Bomb Server에서 ErrorCode 추가, 예외 발생 코드 작성 시 사용.
  "에러코드 추가해줘", "예외 처리해줘", "ErrorCode 만들어줘", "예외 던져줘" 요청 시 반드시 참고.
---

# 예외 처리 및 ErrorCode 작성 스킬

## 포맷
```java
NAME(HttpStatus.STATUS, "DOMAIN_001", "~입니다."),
```
허용 상태: 400 / 401 / 403 / 404 / 409

## 코드 범위
| 범위               | 상태 |
|------------------|---|
| `DOMAIN_001~099` | 400 Bad Request |
| `DOMAIN_101~199` | 401 Unauthorized |
| `DOMAIN_301~399` | 403 Forbidden |
| `DOMAIN_401~499` | 404 Not Found |
| `DOMAIN_901~999` | 409 Conflict |

이미 외부 계약으로 사용 중인 ErrorCode는 범위가 다르더라도 형식 통일만을 위해 번호를 바꾸지 않는다. 포맷과 번호 정책의 단일 원본은 루트 `AGENTS.md`다.

## 예외 발생
```java
throw ApplicationException.from(AlarmErrorCode.ALARM_NOT_FOUND);

alarmRepository.findById(id)
    .orElseThrow(() -> ApplicationException.from(AlarmErrorCode.ALARM_NOT_FOUND));
```

## 파일 위치
| 도메인 | 파일 |
|---|---|
| Alarm | `domains/alarm/exception/AlarmErrorCode.java` |
| Auth | `domains/auth/exception/AuthErrorCode.java` |
| Member | `domains/member/exception/MemberErrorCode.java` |
| 공통 | `global/response/code/CommonErrorCode.java` |

## @CustomErrorCodes
모든 Controller 매핑 메서드에 발생 가능한 ErrorCode 명시 필수

## 신규 도메인 추가 시
1. `domains/{domain}/exception/{Domain}ErrorCode.java` 생성
2. `BaseErrorCode` implements
3. `global/annotation/swagger/CustomErrorCodes.java`에 배열 필드 추가
