# Repo Context

## 리포 역할

`chat`은 TogetherTrip의 Spring Boot WebSocket 채팅 서버다.

## 책임 범위

- WebSocket 연결과 세션 인증
- 여행 단위 채팅방 접근 제어
- 메시지 송수신, 저장, 조회 정책
- 연결 상태, 재접속, 중복 전송 방지
- 채팅 알림이 필요한 경우 notification 연동 요청

## 아키텍처 원칙

- 채팅 bounded context를 독립적으로 유지한다.
- feature-based MVC 패턴을 기본으로 하고, WebSocket/API 진입점, Service, Repository, domain 책임을 분리한다.
- 인증/인가 확인은 연결 시점과 메시지 처리 시점 모두 고려한다.
- 외부 시스템 연동은 Service 내부의 명시적인 클라이언트/설정으로 분리하고, 별도 아키텍처 계층 패키지는 만들지 않는다.

## 통신 규칙

- `app -> gateway -> chat` 흐름의 WebSocket/API 진입을 기본으로 한다.
- `chat -> notification` 채팅 알림 요청을 허용한다.
- 다른 서비스의 DB에 직접 접근하지 않는다.
- 여행/동행자 권한 확인은 명시적인 API 또는 검증 경계로 처리한다.

## 핵심 도메인 주의점

- 여행 동행자만 해당 여행 채팅방에 접근할 수 있다.
- 제거/탈퇴 사용자의 접근 권한은 즉시 차단되어야 한다.
- 메시지 작성자 표시는 탈퇴/퇴장 사용자 정책과 충돌하지 않아야 한다.
- 채팅 알림이 있다면 자기 자신은 알림 대상에서 제외한다.
