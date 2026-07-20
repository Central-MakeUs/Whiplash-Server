---
name: zoom-out
description: 이 저장소에서 특정 도메인, 클래스, API, 흐름이 전체 아키텍처에서 어떤 역할을 하는지 설명하고 레이어 의존, 도메인 연결, 리스크를 요약할 때 사용한다.
---

# Zoom Out

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 좁은 코드 조각을 전체 시스템 맥락에서 설명한다.
- 도메인 간 연결과 데이터 흐름을 빠르게 파악한다.
- 변경 전 영향 범위와 리스크를 확인한다.

## 확인 순서

1. 사용자가 지정한 파일, 클래스, API, 도메인을 찾는다.
2. controller → use case → service → repository/entity 흐름을 따라간다.
3. 관련 DTO, mapper, exception, config, external client를 확인한다.
4. 다른 도메인과 연결되는 지점을 정리한다.
5. 변경 시 깨질 수 있는 API 계약, 테스트, 운영 설정을 정리한다.

## 설명에 포함할 내용

- 이 코드의 책임
- 요청/응답 또는 이벤트 흐름
- 레이어별 주요 참여자
- 의존하는 외부 시스템(MySQL, Redis, FCM, 결제, 장소 검색 등)
- 현재 구조의 장점과 리스크
- 변경 전에 확인해야 할 테스트

## 관련 skill

- 레이어 위반이나 네이밍 위반을 깊게 보려면 [review-architecture](../review-architecture/SKILL.md)를 사용한다.
- 성능 영향은 [review-performance](../review-performance/SKILL.md)를 사용한다.
- 인증/인가나 민감 정보는 [review-security](../review-security/SKILL.md)를 사용한다.

## 출력 방식

- 처음에 한 문장 요약을 둔다.
- 그 다음 흐름, 의존성, 리스크 순서로 정리한다.
- 추정이 필요한 부분은 “추정”이라고 표시한다.
