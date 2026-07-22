---
name: slo-check
description: 이 저장소에서 신규 기능 개발, 기능 수정, PRD 작성, API 변경, 스케줄러/Redis/FCM/외부 API 연동 작업을 할 때 latency, availability, error rate 관점의 SLO 영향을 점검하고 필요한 metric, 테스트, 운영 확인 항목을 정리할 때 사용한다.
---

# SLO Check

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 기능 구현에 치중하다가 처리 성능 목표를 놓치지 않도록 한다.
- 새 기능이나 수정이 latency, availability, error rate에 어떤 영향을 주는지 점검한다.
- 필요한 경우 Prometheus/Micrometer, Actuator, Sentry, 로그로 관측 가능한 지표를 정한다.
- 엄격한 운영 문서보다 “이번 변경에서 최소한 무엇을 지켜야 하는가”를 빠르게 확인한다.

## 언제 사용하나

- 사용자 요청이 많은 API를 추가하거나 수정할 때
- 알람 울림, 알림 발송, 결제, 장소 검색처럼 실패가 사용자 경험에 바로 드러나는 흐름을 바꿀 때
- Redis, FCM, 외부 API, scheduler, batch성 로직을 추가하거나 수정할 때
- `to-prd`, `write-test-code`, 성능 리뷰 중 SLO 영향이 있을 수 있다고 판단될 때

## 점검 순서

1. 핵심 사용자 행동 또는 시스템 작업을 한 문장으로 정의한다.
2. 해당 흐름에서 latency, availability, error rate 중 중요한 축을 고른다.
3. 목표값이 필요하면 p95/p99, 성공률, 에러율 형태로 초안을 만든다.
4. 이미 측정 가능한 metric이 있는지 확인한다.
5. metric이 없으면 최소 관측 방법을 정한다.
6. 구현 또는 테스트에서 방어해야 할 성능/실패 시나리오를 정리한다.

## 질문

- 이 기능이 느려지면 사용자가 바로 느끼는가?
- 이 기능이 실패하면 앱의 핵심 가치가 깨지는가?
- 어떤 부하 상황에서도 지켜야 할 약속이 있는가?
- latency SLO가 필요한가? 예: 일반 REST API는 `p95 < 500ms`를 초안으로 검토
- availability SLO가 필요한가? 예: `성공률 99% 이상`
- error rate SLO가 필요한가? 예: `5xx 1% 이하`, `FCM 발송 실패율 N% 이하`
- 현재 Prometheus/Micrometer/Actuator/Sentry/로그로 측정 가능한가?
- 없다면 이번 작업에서 metric을 추가해야 하는가, 아니면 후속 작업으로 분리할 것인가?

## Latency 목표값 참고

아래 값은 ISO 표준이 아니라 업계 실무에서 경험적으로 쓰이는 도메인별 기준이다. 실제 SLO는 사용자 경험, 트래픽 규모, 외부 API 의존성, 인프라 비용을 함께 보고 조정한다.

| 도메인 | 통용 기준 | 이유 |
|---|---:|---|
| HFT / 고빈도 거래 | `p99 < 500us` | 마이크로초 단위 경쟁 |
| 결제 / 체크아웃 | `p99 < 200ms` | 1% 느림도 매출에 직접 영향 |
| 일반 REST API | `p95 < 500ms` | 가장 널리 쓰이는 기준 |
| 내부 마이크로서비스 | `p95 < 100ms` | 서비스 간 호출은 더 엄격하게 관리 |
| 분석 파이프라인 | `p95 < 5s` | 응답을 기다리는 사용자가 없거나 적음 |

Time Bomb Server에서 별도 근거가 없으면 사용자-facing REST API는 `p95 < 500ms`를 기본 초안으로 두고, 알람/결제/장소 검색처럼 사용자 경험에 민감한 흐름은 더 엄격한 목표가 필요한지 질문한다.

## Time Bomb Server 기준 관측 후보

- HTTP API latency/error: Actuator의 `http.server.requests`
- scheduler 처리 시간: Micrometer `Timer`
- 시도/성공/실패 횟수: Micrometer `Counter`
- 운영 예외 추적: Sentry
- 요청/응답 흐름 추적: `HttpLoggingFilter`와 애플리케이션 로그

## 출력 방식

```text
SLO 관점:
- 핵심 흐름: {사용자 행동 또는 시스템 작업}
- 중요한 축: latency / availability / error rate 중 선택
- 목표 초안: {필요한 경우 p95/p99, 성공률, 에러율}
- 관측 방법: {기존 metric 또는 추가할 metric}
- 테스트/검증: {성능 또는 실패율을 방어할 방법}
- 후속 작업: {이번 범위에서 제외할 SLO/모니터링 작업}
```

## 관련 skill

- 기능 계획에 포함할 때는 [to-prd](../to-prd/SKILL.md)를 따른다.
- 성능 구현 리스크는 [review-performance](../review-performance/SKILL.md)를 함께 사용한다.
- 실제 성능/장애 원인 분석은 [diagnose](../diagnose/SKILL.md)를 사용한다.
