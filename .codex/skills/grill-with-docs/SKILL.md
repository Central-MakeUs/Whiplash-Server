---
name: grill-with-docs
description: 이 저장소에서 큰 기능 설계나 정책 변경을 시작하기 전에 기존 docs/PLAN.md, AGENTS.md, 도메인 규칙을 근거로 요구사항을 질문하고 구현 결정을 문서화할 때 사용한다.
---

# Grill With Docs

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 큰 기능을 구현하기 전에 애매한 요구사항을 줄인다.
- 기존 `docs/` 계획, API 계약, 도메인 규칙과 충돌하지 않게 한다.
- 질문 결과를 PRD 또는 PLAN.md에 반영할 수 있는 결정으로 만든다.

## 진행 순서

1. 관련 `docs/*/PLAN.md`, controller, DTO, service, exception을 먼저 읽는다.
2. 현재 시스템이 이미 결정한 정책과 새 요구사항의 충돌을 찾는다.
3. 구현에 영향을 주는 질문만 한다.
4. 답변을 API, 도메인 정책, 예외, 테스트 기준으로 정리한다.
5. 필요하면 [to-prd](../to-prd/SKILL.md) 형식으로 계획을 만든다.

## 질문해야 하는 경우

- API request/response 계약이 바뀌는 경우
- 결제, 알람 삭제, 위치 인증처럼 클라이언트 UX와 서버 정책이 함께 바뀌는 경우
- ErrorCode 또는 상태 코드 정책 선택이 필요한 경우
- DB migration이나 Redis key 정책이 필요한 경우
- 기존 PLAN.md와 다른 방향의 요구사항이 들어온 경우

## 질문하지 말아야 하는 경우

- 저장소 검색으로 확인 가능한 파일 위치나 네이밍
- AGENTS.md 또는 기존 skill에 이미 명시된 규칙
- 단순 오타 수정이나 작은 테스트 보강

## 출력 방식

- 결정된 내용과 남은 질문을 분리한다.
- 질문은 구현 선택지를 바꿀 만큼 중요한 것만 남긴다.
- 문서화가 필요하면 [to-prd](../to-prd/SKILL.md)를 사용한다.
