---
name: review-test
description: 이 저장소에서 작성된 테스트 코드가 프로젝트 컨벤션을 지키는지 리뷰할 때 사용한다. nested 구조, success/fail 네이밍, DisplayName, BDD 주석, 어노테이션 선택, fixture 사용을 점검한다.
---

# Review Test

루트 [AGENTS.md](../../../AGENTS.md)와 `write-test-code` 규칙을 전제로 사용한다.

## 리뷰 목적

- 테스트 구조 일관성 점검
- 네이밍 규칙 점검
- `@DisplayName` 규칙 점검
- BDD 주석 존재 여부 점검
- 테스트 슬라이스 어노테이션 선택 점검
- fixture 사용 방식 점검

## 1. 구조

중점 확인:

- 루트 클래스명이 `{원본클래스명}Test`인가
- 메서드마다 `@Nested` inner class가 있는가
- inner class명이 `{메서드명}Test` 형식인가

## 2. 메서드 네이밍

중점 확인:

- 성공 케이스가 `success()`인가
- 실패 케이스가 `fail_{이유}()` 형식인가

## 3. DisplayName

중점 확인:

- 문장형인가
- `~테스트`가 없는가
- 성공/실패 prefix가 적절한가
- inner class DisplayName이 `"{메서드명} - {기능}"` 형식인가

## 4. BDD 주석

중점 확인:

- `// given`
- `// when`
- `// then`

세 주석이 모두 존재하는가

## 5. 어노테이션 선택

중점 확인:

- Controller 테스트에 `@WebMvcTest` 계열을 썼는가
- Service 테스트에 Mockito 기반 단위 테스트를 썼는가
- Repository 테스트에 `@PersistenceTest`를 썼는가
- 전체 플로우 테스트에 `@IntegrationTest`를 썼는가
- Controller 협력 객체를 `@MockitoBean`으로 주입했는가

## 6. Fixture 사용

중점 확인:

- 서비스 테스트에서 `toMockEntity()`를 쓰는가
- persistence / integration 테스트에서 `toEntity()`를 쓰는가
- 이미 있는 fixture 대신 직접 builder를 남발하지 않는가

## 출력 방식

- 위반 사항은 파일/라인과 함께 구체적으로 적는다.
- 가능하면 수정 방향을 예시와 함께 제안한다.
- 문제 없음이면 컨벤션상 큰 이슈 없음을 명시한다.
