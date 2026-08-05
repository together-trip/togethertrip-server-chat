# Work Plan

## 작업

GitHub Issue #6의 공통 로깅 기반을 운영 로그에 안전하게 사용할 수 있도록 보완한다.

## 배경

현재 구현은 요청 상관관계와 Service/Handler 실행 시간을 남기지만, query string과 임의 객체의 문자열 표현, 예외 메시지가 로그에 포함될 수 있다. 또한 MDC 값을 설정해도 console pattern에 출력 설정이 없어 실제 로그에서 상관관계를 확인하기 어렵고, 요청 종료 시 기존 MDC 전체를 지운다.

## 범위

- HTTP 로그는 method, path, status, elapsedMs와 exceptionType만 기록한다.
- query, body, header 값과 Throwable/exception message 원문을 기록하지 않는다.
- 외부 `X-Request-Id`는 길이와 허용 문자 규칙을 모두 만족할 때만 사용한다.
- 요청 진입 전 MDC를 보존하고 요청 종료 후 복원한다.
- AOP 인자 요약은 안전한 primitive/enum 및 collection/map/array의 크기 같은 allowlist metadata로 제한한다.
- console pattern에 `requestId`, `userId`, `sessionId`, `chatRoomId`, `messageType`을 기본값과 함께 출력한다.
- 로그 캡처 및 실제 Spring proxy 기반 회귀 테스트를 추가한다.
- WebSocket MDC lifecycle과 로그 보안 정책을 문서화한다.

## 제외 범위

- WebSocket endpoint, STOMP interceptor 또는 실제 채팅 도메인 구현
- 외부 observability 제품 연동
- JSON structured logging 전환
- 운영 배포와 GitHub Issue/PR 상태 변경

## 설계

- 요청 path는 `request.requestURI`만 사용하고 query string은 접근하거나 출력하지 않는다.
- request ID는 영문 대소문자, 숫자, `.`, `_`, `:`, `-`만 허용하며 1~100자로 제한한다.
- 필터는 `MDC.getCopyOfContextMap()` 결과를 보관하고 `finally`에서 원래 상태로 복원한다.
- HTTP 실패 로그는 예외 타입만 필드로 남기며 Throwable 자체를 logger에 전달하지 않는다.
- AOP는 문자열 내용과 임의 객체의 `toString()`을 사용하지 않는다. 문자열은 타입과 길이만, primitive/enum은 값, collection/map/array는 타입과 크기만 기록한다.
- console pattern은 각 MDC 키가 없을 때 `-`를 출력한다.

## 테스트 계획

1. 구현 전 실패하는 회귀 테스트를 먼저 추가한다.
2. ListAppender로 query, token, 메시지 원문, 예외 메시지와 stack trace가 출력되지 않는지 검증한다.
3. 유효/무효/과길이 request ID와 기존 MDC 복원을 검증한다.
4. Spring context의 Service/Handler fixture를 호출해 실제 proxy와 pointcut 적용 및 안전한 인자 요약을 검증한다.
5. console pattern에 모든 상관관계 키와 기본값이 있는지 검증한다.
6. 최종적으로 `./gradlew --no-daemon clean test`와 `git diff --check`를 실행한다.

## 위험과 확인 사항

- 예외 메시지와 stack trace를 공통 요청 로그에서 제외하면 진단 정보가 줄지만, 사용자 입력이 예외에 포함되는 현재 경계에서는 개인정보 비노출을 우선한다.
- 추후 제한된 보안 로그 저장소를 도입할 때 stack trace 정책을 별도 ADR로 재검토한다.
- WebSocket 기능이 추가되면 연결·메시지 처리 각각에서 MDC를 설정하고 반드시 이전 컨텍스트를 복원해야 한다.
