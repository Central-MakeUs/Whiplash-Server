# Privacy and Audit Logging Policy

## 1. 문서 목적

이 문서는 Whiplash-Server에서 개인정보, 위치 데이터, 감사 로그, 운영 로그를 다룰 때 따르는 내부 개발 정책이다.

법적 개인정보 처리방침 문구가 아니라 서버 구현, 코드 리뷰, 로그 점검, 기능 설계 시 참고하는 엔지니어링 기준이다. 외부 고지 문구와 스토어 제출 문구는 이 문서를 기준 자료로 삼되 별도 법무 검토를 거쳐 확정한다.

## 2. 기본 원칙

- 서비스 기능 제공에 필요한 데이터만 저장한다.
- 같은 목적을 달성할 수 있다면 원문보다 결과, 상태, 집계값, 최소 식별자를 우선 저장한다.
- 감사 테이블에는 행위 증적에 필요한 식별자와 결과만 저장한다.
- 일반 운영 로그에는 민감 식별자, 인증/결제 토큰, 위치 원문, provider raw response 전체를 남기지 않는다.
- 예외 메시지, 실패 사유, metric label에도 민감 식별자 원문을 포함하지 않는다.

## 3. 데이터 분류

| 분류 | 데이터 | 정책 | 비고 |
|---|---|---|---|
| 저장 가능 | 회원 식별 정보, email, provider user id | 인증과 계정 식별에 필요한 범위에서 저장 | 외부 노출과 로그 원문 기록은 피한다 |
| 저장 가능 | `deviceId`, `fcmToken` | 디바이스 식별, 알림 발송, 로그아웃/토큰 교체 처리를 위해 저장 | 운영 로그에서는 마스킹한다 |
| 저장 가능 | 알람 목적지 좌표와 주소 | 알람 생성, 목록, 체크인 거리 계산을 위해 저장 | 체크인 현재 좌표와 구분한다 |
| 저장 가능 | `paymentId`, `adProofToken` 등 결제/광고 감사 식별자 | 중복 처리 방지, 분쟁 대응, 감사 추적을 위해 필요한 테이블에 저장 | 일반 운영 로그에서는 원문을 남기지 않는다 |
| 저장 금지 | 체크인 요청 시점의 사용자 현재 위치 원문 | 거리 계산 중 일시적으로만 사용 | DB, 감사 로그, 운영 로그에 원문 저장하지 않는다 |
| 저장 금지 | 실시간 위치 이력, 이동 경로, background location stream | 현재 서비스 목적 범위 밖 | 별도 기능 요구와 정책 검토 없이 수집하지 않는다 |
| 저장 금지 | provider raw response 전체 | 필요한 표준 DTO와 식별자만 추출 | 장애 분석용 로그에도 전체 payload를 남기지 않는다 |
| 저장 금지 | 인증 token, 결제 receipt, secret, API key 원문 로그 | 원문 로그 금지 | 검증 결과와 내부 식별자 중심으로 기록한다 |
| 주의 대상 | `latitude`, `longitude` | 목적지 좌표인지 현재 위치 좌표인지 먼저 구분 | 현재 위치 좌표는 원문 저장 금지 |
| 주의 대상 | `deviceId`, `fcmToken`, `paymentId`, `adProofToken`, email, provider user id | 저장 목적과 로그 노출 여부를 함께 검토 | `LogUtils` 마스킹 또는 `@NoMethodLog` 적용 대상 |

## 4. 위치 데이터 정책

위치 데이터는 목적지 좌표와 현재 위치 좌표를 반드시 구분한다.

알람 목적지 좌표와 주소는 알람 기능 제공에 필요한 데이터이므로 `alarm` 데이터로 저장한다. 체크인 API는 DB에 저장된 목적지 좌표와 요청으로 받은 현재 좌표를 메모리에서 거리 계산한 뒤 성공 여부만 판단한다.

체크인 요청의 현재 좌표는 인증 처리 중 일시적으로만 사용한다. DB, 감사 로그, 일반 운영 로그에 현재 좌표 원문을 저장하지 않는다. abuse 탐지가 필요하더라도 원문 좌표 대신 결과, 실패 횟수, 거리 구간, 국가/지역 수준의 최소 데이터 사용을 우선 검토한다.

기존 `alarm_deactivation_log.request_latitude`, `alarm_deactivation_log.request_longitude` 컬럼은 deprecated 상태로 본다. 신규 JPA 엔티티와 저장 경로에서는 이 컬럼을 매핑하지 않으며, 기존 데이터 삭제나 컬럼 제거는 보관 기간과 감사 필요성을 결정한 뒤 별도 migration으로 진행한다.

## 5. 감사 로그 정책

감사 로그는 사용자의 행위, 서버의 판정 결과, 중복 처리 방지, 분쟁 대응을 재구성할 수 있을 만큼만 저장한다.

저장 가능한 감사 정보:

- 행위 대상: `memberId`, `alarmId`, `occurrenceId`
- 요청 기기: `deviceId`
- 처리 방식: 체크인, 결제, 광고 등 action type
- 처리 결과: 성공/실패, 실패 코드, 처리 시각
- 결제/광고 중복 방지에 필요한 `paymentId`, `adProofToken`

저장하지 않는 감사 정보:

- 체크인 요청 현재 좌표 원문
- 인증 token, 결제 receipt, provider secret
- provider raw response 전체
- 실패 원인 설명에 포함된 민감 식별자 원문

실패 사유를 문자열로 만들 때는 `paymentId=...`, `token=...`, `latitude=...`처럼 민감 값을 직접 이어 붙이지 않는다. 필요하면 내부 코드, provider error code, enum, boolean 결과처럼 원문을 포함하지 않는 값으로 남긴다.

## 6. 운영 로그 정책

HTTP logging, method logging, scheduler logging, provider client logging은 디버깅 가능한 수준의 맥락만 남기고 민감 원문을 남기지 않는다.

- request/response body와 query string은 `LogUtils` 마스킹 정책을 거친다.
- 민감 문자열 인자를 받는 서비스는 필요하면 `@NoMethodLog`로 method logging을 억제한다.
- 외부 provider 장애 로그에는 provider 종류, 내부 요청 id, 실패 코드, 처리 결과를 남기고 raw response 전체와 token 원문은 제외한다.
- metric label에는 위치 원문, token, `deviceId`, `paymentId`, `adProofToken` 같은 고카디널리티 또는 민감 값을 사용하지 않는다.

마스킹 또는 로그 억제 우선 대상:

- `latitude`, `longitude`
- `deviceId`, `fcmToken`
- `paymentId`, `adProofToken`
- `email`, provider user id
- `authorization`, access/refresh token, API key, secret, receipt

## 7. 구현 및 리뷰 체크리스트

새 API, DTO, Entity, 로그, 외부 provider 연동을 추가하거나 수정할 때 아래 항목을 확인한다.

- 저장하는 데이터가 서비스 기능 제공 또는 감사에 필요한가
- 목적지 좌표와 현재 위치 좌표를 구분했는가
- 현재 위치 원문이 DB, 감사 로그, 운영 로그에 저장되지 않는가
- 결제/광고 식별자가 필요한 테이블에만 저장되고 일반 운영 로그에는 노출되지 않는가
- 실패 사유와 예외 메시지에 민감 식별자 원문을 포함하지 않는가
- request body, query string, method args가 `LogUtils` 또는 `@NoMethodLog`로 보호되는가
- provider raw response 전체를 저장하거나 로그로 남기지 않는가
- 회원 탈퇴, 알람 삭제, 토큰 교체 시 보관/삭제 기준이 기존 정책과 충돌하지 않는가
