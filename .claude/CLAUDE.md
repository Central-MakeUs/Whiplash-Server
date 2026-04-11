# Whiplash

## 스택
Java 17, Spring Boot 3.5, MySQL 8, Redis 7.2, Firebase FCM

## 도메인
alarm / auth / member / place

## 절대 규칙
- 예외: `ApplicationException.from(ErrorCode)` 만
- HttpStatus: 400/401/403/404/409 만
- 응답: `ApplicationResponse<T>` 필수
- 레이어: `presentation → application → domain → persistence`
- 메서드: Service=`getXxx/createXxx/removeXxx/modifyXxx` / Repository=`findBy/insertXxx/deleteXxx/updateXxx`

## 커맨드
```
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local'
docker-compose up -d
```

## 참조
- 규칙 전체: `.claude/rules/RULES.md`
- 도메인 패턴: `.claude/skills/create-domain-layer/SKILL.md`
- 테스트 패턴: `.claude/skills/write-test-code/SKILL.md`
- ErrorCode: `.claude/skills/handle-exception/SKILL.md`
- 환경/인프라: `.claude/skills/ask-env/SKILL.md`
