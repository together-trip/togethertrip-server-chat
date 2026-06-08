# Work: 채팅 서버 공통 로깅 AOP 구현

작성일: 2026-06-08
브랜치: `feature/issue-6-chat-logging-aop`
이슈: https://github.com/together-trip/togethertrip-server-chat/issues/6
PR: https://github.com/together-trip/togethertrip-server-chat/pull/7

## 작업

채팅 서버에 공통 로깅 기반을 추가했다.

현재 `chat` 서버는 skeleton 상태지만, 이후 WebSocket 세션, 채팅방, 메시지 처리 로직이 붙을 때 로그 기준이 흔들리지 않도록 요청 식별자, 채팅 컨텍스트, Service/Handler 실행 시간, 민감정보 마스킹을 먼저 구성했다.

## 배경

채팅 서버는 HTTP 요청뿐 아니라 WebSocket 연결과 메시지 송수신 흐름을 다루게 된다.

운영 중에는 다음 정보를 빠르게 확인해야 한다.

- 어떤 요청 또는 세션에서 장애가 발생했는지
- 어떤 사용자, 채팅방, 메시지 타입의 처리 흐름인지
- 메시지 처리 시간이 얼마나 걸렸는지
- 채팅 메시지 원문이나 인증 토큰이 로그에 남지 않았는지

## 수정

- `spring-aop`, `aspectjweaver` 의존성을 추가했다.
- HTTP 요청 단위 `X-Request-Id` 생성/유지 필터를 추가했다.
- 요청 완료 시 method, path, status, elapsedMs를 로그로 남기도록 했다.
- 채팅 특화 로그 컨텍스트를 추가했다.
  - `requestId`
  - `userId`
  - `sessionId`
  - `chatRoomId`
  - `messageType`
- Service/Handler 계층 실행 시간 AOP를 추가했다.
- 예외 발생 시 실행 시간, exception type, 마스킹된 message를 남기도록 했다.
- 채팅 메시지 원문, 토큰, 전화번호, 이메일 마스킹 유틸을 추가했다.
- 요청 필터, 마스킹, AOP 단위 테스트를 추가했다.

## 변경 파일

- `build.gradle.kts`
  - AOP 의존성 추가
- `src/main/kotlin/com/togethertrip/chat/global/logging/ChatLoggingContext.kt`
  - 채팅 로그 컨텍스트 키 관리
- `src/main/kotlin/com/togethertrip/chat/global/logging/RequestLoggingFilter.kt`
  - 요청 단위 requestId와 완료 로그 처리
- `src/main/kotlin/com/togethertrip/chat/global/logging/ServiceLoggingAspect.kt`
  - Service/Handler 실행 시간 로그 처리
- `src/main/kotlin/com/togethertrip/chat/global/logging/SensitiveDataMasker.kt`
  - 메시지 원문과 민감정보 마스킹
- `src/test/kotlin/com/togethertrip/chat/global/logging/*`
  - 로깅 단위 테스트

## 테스트

```bash
./gradlew test
```

검증 결과:

- 전체 테스트 통과

## 위험과 확인 사항

- 실제 WebSocket endpoint와 channel interceptor가 생기면 세션 연결/해제 시점에 `sessionId`, `chatRoomId` 컨텍스트를 연결해야 한다.
- 채팅 메시지 원문은 기본 로그 출력 대상에서 제외한다.
- 채팅방 권한 실패와 rate limit 실패 로그는 실제 정책 구현 시 별도 분기로 보강한다.

## 관련 이슈

- https://github.com/together-trip/togethertrip-server-chat/issues/6
