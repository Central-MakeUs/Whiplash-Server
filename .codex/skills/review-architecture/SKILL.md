---
name: review-architecture
description: 이 저장소에서 아키텍처, 레이어 의존, 메서드 네이밍, ErrorCode 형식, DTO 규칙, CustomErrorCodes 선언, 트랜잭션 경계를 리뷰할 때 사용한다.
---

# Review Architecture

루트 [AGENTS.md](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/AGENTS.md) 규칙을 전제로 사용한다.

## 리뷰 목적

- 레이어 구조 위반 탐지
- Service / Repository 네이밍 규칙 위반 탐지
- ErrorCode 형식 및 예외 사용 방식 점검
- DTO 필드 네이밍 규칙 점검
- `@CustomErrorCodes` 누락 점검
- 트랜잭션 경계 점검

## 리뷰 절차

1. 대상 파일을 읽는다.
2. 레이어 구조와 import 방향을 확인한다.
3. 메서드 네이밍과 예외 처리 방식을 확인한다.
4. DTO, Controller, 트랜잭션 규칙을 확인한다.
5. 위반 사항이 있으면 파일/라인 근거와 함께 정리한다.

## 1. 레이어 의존 방향

허용 방향:

`presentation -> application -> domain -> persistence`

중점 확인:

- `domain/service`가 `application` 또는 `presentation`을 참조하는가
- `persistence`가 상위 레이어 비즈니스 로직을 참조하는가
- UseCase가 과도하게 persistence를 직접 다루는가

## 2. 메서드 네이밍 규칙

Service / UseCase에서 기대하는 이름:

- `getXxx`
- `createXxx`
- `removeXxx`
- `modifyXxx`

Repository에서 기대하는 이름:

- `findByXxx`
- `countByXxx`
- `existsByXxx`
- `insertXxx`
- `updateXxx`
- `deleteXxx`

중점 확인:

- Service에서 Repository 전용 prefix를 쓰고 있지 않은가
- Repository에서 Service 전용 prefix를 쓰고 있지 않은가

## 3. ErrorCode 및 예외 처리

중점 확인:

- `ApplicationException.from(...)`을 사용하는가
- 허용된 `HttpStatus`만 사용하는가
- ErrorCode 포맷이 일관적인가
- message가 프로젝트 톤과 맞는가

## 4. DTO 규칙

중점 확인:

- PK 필드가 `alarmId`, `memberId`처럼 도메인 prefix를 포함하는가
- List 응답 필드가 복수형인가
- Enum 응답이 `.name()` 기준과 어긋나지 않는가
- 날짜/시간 필드가 기존 응답 형식과 일관적인가

## 5. Controller 및 Swagger

중점 확인:

- Controller가 `ApplicationResponse`를 반환하는가
- 모든 매핑 메서드에 `@CustomErrorCodes`가 선언되어 있는가

## 6. 트랜잭션 경계

중점 확인:

- `CommandServiceImpl`은 `@Transactional`을 사용하는가
- `QueryServiceImpl`은 `@Transactional(readOnly = true)`를 사용하는가
- UseCase에 불필요한 트랜잭션이 과하게 걸려 있지 않은가

## 출력 방식

- 문제 발견 시: 파일 경로, 라인, 문제 원인, 수정 방향 순서로 정리한다.
- 문제 없음: 큰 위반 없음과 함께 잔여 리스크가 있으면 짧게 덧붙인다.
