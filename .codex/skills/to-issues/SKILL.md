---
name: to-issues
description: 이 저장소에서 PRD, PLAN.md, 기능 설계를 GitHub 이슈 초안으로 분해하고 domain, controller, service, test, migration 단위 TODO로 정리할 때 사용한다.
---

# To Issues

루트 [AGENTS.md](../../../AGENTS.md) 규칙을 전제로 사용한다.

## 목적

- 큰 계획을 독립적으로 처리 가능한 GitHub 이슈 단위로 쪼갠다.
- 기존 `.github/ISSUE_TEMPLATE/ISSUE.md` 형식을 따른다.
- 실제 이슈 생성은 사용자 요청이 있을 때만 수행하고, 기본은 초안 작성이다.

## 분해 기준

- API 계약 변경
- 도메인 로직 변경
- persistence / migration 변경
- 외부 연동 변경
- 테스트 추가 또는 보강
- 문서와 운영 확인 작업

작은 작업은 하나의 이슈로 묶고, 서로 다른 배포 위험이나 리뷰 대상이 있으면 분리한다.

## 이슈 초안 형식

```markdown
## 🎈 About Issue 🎈
{작업 목적과 배경}

## ✅ Todo
- [ ] {구체 작업}
- [ ] {테스트 또는 검증}

## 📚 Reference
- {관련 PLAN.md, PR, API 문서}
```

## 작성 규칙

- Todo는 구현자가 바로 작업할 수 있는 동사형 문장으로 쓴다.
- 각 이슈는 완료 기준이 명확해야 한다.
- 레이어 규칙, 예외 규칙, 테스트 규칙을 필요한 Todo에 포함한다.
- GitHub 이슈 번호나 라벨은 확인 가능한 정보가 없으면 추정하지 않는다.

## 관련 skill

- 계획 원문 작성은 [to-prd](../to-prd/SKILL.md)를 따른다.
- 도메인 작업 분해는 [create-domain-layer](../create-domain-layer/SKILL.md)를 참고한다.
- 테스트 작업 분해는 [write-test-code](../write-test-code/SKILL.md)를 참고한다.
