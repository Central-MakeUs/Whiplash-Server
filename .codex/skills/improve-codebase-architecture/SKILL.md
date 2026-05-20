---
name: improve-codebase-architecture
description: 이 저장소에서 전체 코드베이스 또는 특정 도메인의 구조 개선 후보를 탐색하고, 레이어 의존, 트랜잭션 경계, DTO/mapper 위치, 도메인 분리 개선안을 제안할 때 사용한다.
---

# Improve Codebase Architecture

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 단일 파일 리뷰보다 넓은 범위에서 구조 개선 후보를 찾는다.
- 현재 동작을 유지하면서 레이어 방향과 책임 분리를 개선한다.
- 큰 리팩터링은 작은 단계로 나눈다.

## 탐색 순서

1. 대상 도메인의 package 구조와 public API를 확인한다.
2. controller → application → domain → persistence 의존 방향을 확인한다.
3. application DTO가 domain service나 persistence로 새는지 확인한다.
4. UseCase와 service의 트랜잭션 경계를 확인한다.
5. mapper 위치와 변환 책임이 규칙에 맞는지 확인한다.
6. 개선 후보를 위험도와 효과 기준으로 나눈다.

## 개선 후보 분류

- Safe: 테스트 보강 후 바로 가능한 작은 구조 정리
- Medium: API 계약은 유지하지만 여러 레이어를 함께 바꾸는 작업
- Large: migration, 외부 API, 클라이언트 계약, 운영 영향이 있는 작업

## 관련 skill

- 위반 사항을 리뷰 형식으로 지적하려면 [review-architecture](../review-architecture/SKILL.md)를 사용한다.
- 실제 새 도메인이나 레이어 추가는 [create-domain-layer](../create-domain-layer/SKILL.md)를 따른다.
- 성능이나 보안 관점은 각각 [review-performance](../review-performance/SKILL.md), [review-security](../review-security/SKILL.md)를 함께 사용한다.

## 출력 방식

- 개선 후보를 우선순위 순으로 정리한다.
- 각 후보는 현재 문제, 개선 방향, 영향 범위, 권장 테스트를 포함한다.
- 운영 민감 파일 변경이 필요한 제안은 별도 주의사항으로 분리한다.
