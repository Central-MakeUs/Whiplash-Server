---
name: review-security
description: 이 저장소에서 보안 취약점(SQL Injection, XSS, 시크릿 하드코딩, 명령어 인젝션, 입력 검증, 인증/인가, 민감 데이터 로깅, 오류 정보 노출)을 리뷰할 때 사용한다.
---

# Review Security

루트 [AGENTS.md](/Users/user/dev/cmc/nuntteo/code/Whiplash-Server/AGENTS.md) 규칙을 전제로 사용한다.

Java 17, Spring Boot 3.5, JPA, Redis 환경 기준으로 OWASP Top 10 관점에서 리뷰한다.

## 리뷰 절차

1. 위험 패턴이 있는지 먼저 검색한다.
2. 의심 파일을 열어 실제 취약점인지 확인한다.
3. 심각도와 함께 정리한다.

## 1. SQL Injection

중점 확인:

- native query를 문자열 연결로 조합하는가
- `EntityManager.createQuery` 또는 `createNativeQuery`에 사용자 입력이 직접 들어가는가
- `JdbcTemplate` 쿼리를 문자열 결합으로 만드는가

## 2. XSS

중점 확인:

- 사용자 입력을 HTML 응답에 직접 렌더링하는가
- `HttpServletResponse` writer로 사용자 데이터를 직접 출력하는가
- 보안 헤더 비활성화가 있는가

## 3. 시크릿 하드코딩

중점 확인:

- 비밀번호, API 키, 토큰, 시크릿이 코드나 설정에 하드코딩돼 있는가
- 운영 자격 증명이 추적 가능한 파일에 노출돼 있는가

## 4. 명령어 인젝션

중점 확인:

- `Runtime.getRuntime().exec`
- `ProcessBuilder`
- 사용자 입력이 프로세스 실행 인자로 직접 들어가는지 여부

## 5. 입력 검증

중점 확인:

- `@RequestBody` DTO에 validation이 부족한가
- `@Valid` 또는 `@Validated` 누락이 있는가
- 숫자/enum 파싱 실패가 500으로 번질 가능성이 있는가

## 6. 인증 / 인가

중점 확인:

- `permitAll` 범위가 과한가
- `memberId` 등 식별자를 path parameter로 받아도 현재 사용자 검증이 없는가
- QA/비운영 전용 기능이 운영에서 열릴 가능성이 있는가

## 7. 민감 데이터 로깅

중점 확인:

- 토큰, 비밀번호, 시크릿, FCM 토큰을 평문으로 로깅하는가
- 요청 DTO 전체를 그대로 로그에 남기는가

## 8. 오류 정보 노출

중점 확인:

- 예외 메시지나 스택트레이스를 그대로 응답하는가
- 내부 테이블/컬럼/구현 정보가 외부로 노출되는가

## 출력 방식

- 심각도 순으로 정리한다.
- 파일 경로와 근거를 남긴다.
- 실제 악용 가능성과 수정 방향을 함께 적는다.
