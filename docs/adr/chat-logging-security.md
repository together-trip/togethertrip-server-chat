# 채팅 로그 보안과 MDC 수명주기

## 상태

Accepted

## 배경

채팅 메시지와 인증 정보는 개인정보 또는 민감정보가 될 수 있다. 장애 추적에는 요청·세션·채팅방 단위 상관관계가 필요하지만, 메시지 원문이나 임의 객체의 문자열 표현을 로그에 남겨서는 안 된다. MDC는 thread-local 상태이므로 HTTP 요청과 향후 WebSocket 메시지 처리가 끝날 때 이전 상태를 복원하지 않으면 다른 작업의 상관관계가 섞일 수 있다.

## 결정

### 허용하는 HTTP 로그

- method
- query를 제외한 URI path
- status
- elapsedMs
- exceptionType
- 검증된 requestId와 인증·인가 이후 확인된 식별자 metadata

다음 값은 INFO, WARN, ERROR, DEBUG 수준과 관계없이 공통 요청/AOP 로그에 남기지 않는다.

- query string, request/response body, header 원문
- 채팅 메시지와 첨부 설명 원문
- access/refresh token, Authorization 값
- 전화번호, 이메일 등 개인정보
- Throwable, exception message, 임의 DTO의 `toString()` 결과

AOP 인자 로그는 문자열의 타입·길이, collection/map/array의 타입·크기, 표준 숫자·boolean·enum 같은 allowlist metadata만 허용한다.

### MDC 키

console 로그에는 다음 키를 표시하고, 값이 없으면 `-`를 출력한다.

- `requestId`
- `userId`
- `sessionId`
- `chatRoomId`
- `messageType`

외부 request ID는 허용 문자와 길이를 검증한다. 나머지 값도 인증·인가가 끝난 뒤 신뢰할 수 있는 내부 식별자만 설정해야 하며 메시지 원문을 MDC 값으로 사용하지 않는다.

### HTTP 수명주기

1. 필터 진입 시 기존 MDC map을 복사한다.
2. 검증된 request ID와 요청 범위 값을 설정한다.
3. 요청 완료 로그를 남긴다.
4. `finally`에서 필터 진입 전 MDC map을 복원한다.

### 향후 WebSocket 수명주기

WebSocket 연결 하나가 특정 thread를 계속 점유한다고 가정하지 않는다. handshake, inbound message, outbound handler 각각의 처리 경계에서 다음 순서를 지킨다.

1. 현재 worker thread의 MDC map을 복사한다.
2. 세션 인증과 여행별 채팅방 접근 권한을 먼저 검증한다.
3. 검증된 `userId`, `sessionId`, `chatRoomId`, `messageType`만 설정한다.
4. handler를 호출한다. payload 또는 destination query 원문은 로그에 전달하지 않는다.
5. 성공·실패와 관계없이 `finally`에서 이전 MDC map을 복원한다.

비동기 실행이 필요하면 제출 시점의 MDC 복사본을 명시적으로 전달하고, worker 작업 종료 후 worker의 이전 MDC를 복원한다. 연결 종료 시 한 번만 `clear`하는 방식은 thread pool 재사용과 맞지 않으므로 사용하지 않는다.

## 결과

- 공통 애플리케이션 로그만으로 메시지 원문과 예외 메시지 기반 stack trace를 확인할 수 없다.
- 장애 추적은 상관관계 ID, 예외 타입, 안전한 도메인 오류 코드와 metric을 사용한다.
- 제한된 보안 저장소에 stack trace를 추가하려면 접근 권한, 보존 기간, 마스킹 정책을 포함한 별도 결정이 필요하다.
- WebSocket endpoint나 interceptor를 추가할 때 MDC 설정·복원과 원문 비노출 회귀 테스트를 함께 추가해야 한다.
