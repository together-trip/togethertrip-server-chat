# Review Report

## 요약

GitHub Issue #6 안전 로깅 보완 변경을 `origin/develop...fix/issue-6-safe-logging` 범위에서 검토했다. HTTP 요청 로그, request ID 신뢰 경계, MDC 수명주기, Service/Handler AOP, console pattern과 테스트를 확인했으며 병합을 막는 발견 사항은 없다.

## 발견 사항

| 심각도 | 파일 | 내용 | 제안 |
| --- | --- | --- | --- |
| 없음 | 전체 변경 | 차단, 높음, 중간 심각도의 미해결 사항 없음 | 현재 테스트와 정책을 유지한다. |

## 보안 검토

- `RequestLoggingFilter`는 query, body, 일반 header를 읽거나 기록하지 않고 URI path만 기록한다.
- HTTP 실패 로그는 `exceptionType`만 기록하며 Throwable과 exception message를 logger에 전달하지 않는다.
- 외부 `X-Request-Id`는 `[A-Za-z0-9._:-]{1,100}` 규칙을 만족하지 않으면 새 UUID로 교체한다.
- 필터는 진입 전 MDC를 복사하고 성공·실패와 관계없이 `finally`에서 복원한다.
- AOP는 문자열 내용과 임의 DTO의 `toString()`을 호출하지 않는다. collection, map, array도 원소나 key 대신 타입과 크기만 기록한다.
- 숫자, boolean, enum 외 값은 allowlist metadata로만 요약한다. `Char`는 값도 기록하지 않는다.
- 불완전한 denylist 방식의 `SensitiveDataMasker`와 테스트를 제거해 향후 재사용 위험을 없앴다.
- console pattern은 `requestId`, `userId`, `sessionId`, `chatRoomId`, `messageType`을 표시하고 값이 없으면 `-`를 출력한다.
- ListAppender 테스트가 query, header, body, 메시지, DTO와 예외 원문 및 Throwable proxy 비노출을 검증한다.

## 확인한 명령

```text
./gradlew --no-daemon test
./gradlew --no-daemon clean test
git diff --check
rg -n 'SensitiveDataMasker|queryString|exception\.message' src/main
```

회귀 테스트를 구현 전에 실행했을 때 14개 중 7개가 예상대로 실패했다. 구현과 보안 정리 후 clean test는 16개 모두 성공했다.

## 남은 위험

- 공통 애플리케이션 로그에서 stack trace를 남기지 않으므로 상세 진단 정보가 줄어든다. 제한된 보안 로그 저장소가 필요하면 별도 접근 권한·보존·마스킹 정책을 먼저 정해야 한다.
- 실제 WebSocket endpoint와 interceptor가 아직 없으므로 WebSocket runtime의 MDC 전파는 검증 대상이 아니다. 구현 시 `docs/adr/chat-logging-security.md`의 메시지 단위 snapshot/restore 테스트가 필요하다.
- `userId`, `sessionId`, `chatRoomId`, `messageType`은 인증·인가 후 확인된 내부 식별자만 MDC에 전달해야 한다. 메시지 원문을 해당 키에 넣지 않는 정책을 코드 리뷰에서 계속 확인한다.
