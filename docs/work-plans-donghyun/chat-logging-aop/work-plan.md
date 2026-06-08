# Work Plan

## 작업

이슈 #6 `feat: 채팅 서버 공통 로깅 AOP 구현`을 진행한다.

- GitHub Issue: https://github.com/together-trip/togethertrip-server-chat/issues/6

채팅 서버 특성에 맞춰 HTTP 요청 로그, WebSocket/Handler 확장용 로그 컨텍스트, Service/Handler 실행 시간 AOP, 민감정보 마스킹을 구현한다.

## 배경

`chat` 서버는 TogetherTrip의 WebSocket 채팅 서버다.

향후 채팅방 입장, 메시지 송수신, 메시지 조회, 채팅 알림 연동이 붙으면 단순 HTTP 로그만으로는 장애 원인을 추적하기 어렵다.

운영 중에는 다음 정보를 일관되게 확인할 수 있어야 한다.

- 어떤 요청 또는 세션에서 장애가 발생했는지
- 어떤 사용자, 세션, 채팅방의 처리 흐름인지
- 메시지 처리 시간이 얼마나 걸렸는지
- 메시지 원문, 토큰, 개인정보가 로그에 노출되지 않았는지

## 범위

- HTTP 요청 단위 `X-Request-Id` 생성/유지 로직을 추가한다.
- 요청별 requestId를 응답 헤더에 포함한다.
- MDC 기반 로그 컨텍스트를 추가한다.
- WebSocket/Handler 확장을 고려해 `sessionId`, `chatRoomId`, `messageType` 로그 키를 둔다.
- Service/Handler 계층 실행 시간 AOP를 추가한다.
- 정상 처리, 예외 처리 로그를 구분한다.
- 채팅 메시지 원문, 토큰, 전화번호, 이메일을 마스킹한다.
- 관련 단위 테스트를 추가한다.

## 제외 범위

- 실제 채팅 도메인 기능 구현.
- WebSocket endpoint, STOMP 설정, channel interceptor 구현.
- 채팅방 권한 검증 구현.
- rate limit 정책 구현.
- notification 서버 연동 구현.
- 외부 관측 도구 연동.

## 설계

- `global/logging` 패키지에 로깅 공통 컴포넌트를 둔다.
- `RequestLoggingFilter`가 requestId 생성/유지, 응답 헤더 설정, 요청 완료 로그를 담당한다.
- `ChatLoggingContext`가 `requestId`, `userId`, `sessionId`, `chatRoomId`, `messageType` 키를 관리한다.
- `ServiceLoggingAspect`는 `com.togethertrip.chat..service..*`, `com.togethertrip.chat..handler..*`를 대상으로 한다.
- AOP는 실행 시간과 예외 요약을 남기고, argument 상세는 `DEBUG`에서만 제한적으로 요약한다.
- `SensitiveDataMasker`는 메시지 원문과 인증/개인정보성 값을 원문 그대로 남기지 않는다.

## 테스트 계획

- requestId가 없는 요청은 새 requestId를 생성하는지 검증한다.
- requestId가 있는 요청은 기존 값을 유지하는지 검증한다.
- 요청 종료 후 MDC가 정리되는지 검증한다.
- Service/Handler AOP가 정상 결과를 그대로 반환하는지 검증한다.
- Service/Handler AOP가 예외를 삼키지 않고 다시 던지는지 검증한다.
- 메시지 원문, 토큰, 전화번호, 이메일이 마스킹되는지 검증한다.
- 최종 검증 명령은 `./gradlew test`다.

## 위험과 확인 사항

- 채팅 메시지 원문은 개인정보가 될 수 있으므로 기본 로그 출력 대상에서 제외한다.
- WebSocket 기능이 실제로 붙으면 session lifecycle에 맞춰 MDC 정리 지점을 다시 확인해야 한다.
- 권한 실패와 rate limit 실패 로그는 실제 정책 구현 시 별도 분기로 보강한다.
