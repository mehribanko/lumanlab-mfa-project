# 통합 인증/인가 시스템

> 보호자, 파트너, 어드민이 공존하는 육아 플랫폼의 백엔드 인증 시스템

---

## 프로젝트 개요

3가지 사용자 유형(PARENT, ADMIN, MASTER)을 위한 통합 인증/인가 시스템입니다.

### 지원하는 클라이언트
- 모바일 앱
- 웹 
- 웹 

### 주요 특징
- JWT 기반 무상태 인증
- Refresh Token 회전 (재사용 방지)
- TOTP 기반 MFA (관리자 필수)
- Google 소셜 로그인
- 비밀번호 재설정 (이메일)
- 포괄적인 감사 로깅
- Role 기반 접근 제어

---

## 기술 스택

### 백엔드
- Java 17
- Spring Boot 3.2.0
- Spring Security (인증/인가)
- Spring Data JPA (ORM)
- Flyway (데이터베이스 migration용)

### DB
- PostgreSQL 16 (메인 데이터베이스)
- Redis 7 (세션 저장소)

### 보안
- JWT (io.jsonwebtoken 0.11.5) - Access/Refresh 토큰
- BCrypt (강도 12) 
- TOTP (dev.samstevens.totp)
- Google ZXing - QR 코드 생성

### 개발 도구
- IntelliJ IDEA 
- Gradle 8.5
- Docker Compose
- Postman

---

## 주요 기능

### 1. 인증

#### 회원가입 및 로그인
- 이메일 + 비밀번호 회원가입
- Role 선택 (PARENT, ADMIN, MASTER)
- 로그인 시 JWT 토큰 발급 (Access 15분, Refresh 7일)
- 실패 5회 시 계정 잠금 (15분 후에 다시 로그인 가능!)

#### 토큰 관리
- Access Token: 15분 만료, 사용자 정보 포함
- Refresh Token: 7일 만료, 회전 체인 방식
- 토큰 갱신 시 새 토큰 발급, 기존 토큰 폐기
- 재사용 방지 메커니즘

### 2. MFA 

#### TOTP 기반 2단계 인증
- Google Authenticator 사용
- QR 코드 자동 생성
- 6자리 코드, 30초 갱신
- ADMIN/MASTER role 필수 적용

#### MFA  진행하는 방식 
1. 사용자가 MFA 설정 요청
2. 서버가 TOTP 시크릿 생성
3. QR 코드 이미지 반환 (Base64)
4. 사용자가 앱으로 스캔
5. 코드 입력으로 검증 및 활성화

### 3. 소셜 로그인

#### Google OAuth 2.0
- Google 계정으로 로그인/가입
- 이메일 자동 매칭 및 계정 연결
- 기존 계정에 Google 연결 가능
- ID 토큰 검증

### 4. 비밀번호 관리

#### 비밀번호 변경
- 현재 비밀번호 검증 필요
- 새 비밀번호는 이전과 달라야 함
- 변경 완료 이메일 발송

#### 비밀번호 재설정
- 이메일로 재설정 링크 발송
- 토큰 1시간 유효
- 1회 사용 후 폐기
- 재설정 완료 이메일 발송

### 5. 감사 로깅

모든 보안 이벤트 기록합니다:
- 로그인/로그아웃
- MFA 설정/해제
- 비밀번호 변경/재설정
- 계정 잠금/해제
- 소셜 계정 연결/해제
- IP 주소, User Agent 포함

---

## 실행 방법

### 사전 요구사항

#### 1. 프로젝트 클론

#### 2. 환경 설정

##### application.yml 수정

```yaml
# JWT 시크릿 (최소 43자)
jwt:
  secret: jwt 키 세팅 해주세요

# 이메일 설정 (선택)
spring:
  mail:
    username: gmail 작성해주세요
    password: gmail용 app password 작성해주세요

# Google OAuth (선택)
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: oauth2 client id 작성해주세요
            client-secret: client 비밀 키를 작성해주세요
```

#### 3. Docker 서비스 시작

```
CONTAINER ID   IMAGE              PORTS
xxx            postgres:16        0.0.0.0:5432->5432/tcp
yyy            redis:7            0.0.0.0:6379->6379/tcp
```


### 4. 애플리케이션 실행

A. IntelliJ에서 실행

B. 터미널에서 실행

C. JAR 빌드 후 실행


### 5. 실행 확인

브라우저에서 접속:
```
http://localhost:8080/actuator/health
```

응답:
```json
{
  "status": "UP"
}
```

Swagger UI 접속:
```
http://localhost:8080/swagger-ui/index.html
```

---

## DB 스키마

```
users (사용자 정보)
 -id PK
 -email (unique)
 -password_hash
 -mfa_enabled
 -mfa_secret
 -failed_login_attempts
 -locked_until
 -status

user_roles 
 -id PK
 -user_id FK
 -role (PARENT/ADMIN/MASTER)

refresh_tokens 
 -id PK
 -user_id FK
 -token_hash
 -expires_at
 -rotated_from FK
 -revoked_at

social_accounts (소셜 계정)
 -id PK
 -user_id FK
 -provider GOOGLE
 -provider_user_id

password_reset_tokens (비밀번호 재설정)
 -id PK
 -user_id FK
 -token
 -expires_at
 -used_at

audit_logs (감사 로그)
-id PK
-user_id FK
-action
-details
-ip_address
-user_agent
```

---

## API 문서

### Swagger UI

애플리케이션 실행 후 접속:
```
http://localhost:8080/swagger-ui/index.html
```

### 주요 엔드포인트

#### 인증 (Authentication)

| 메서드 | 엔드포인트 | 설명 |         |
|--------|-----------|------|----------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/logout` | 로그아웃 | 

#### MFA 

| 메서드 | 엔드포인트 | 설명 |         |
|--------|-----------|------|----------|
| GET | `/api/v1/mfa/status` | MFA 상태 확인 | 
| POST | `/api/v1/mfa/setup` | MFA 설정 (QR 생성) |
| POST | `/api/v1/mfa/verify` | MFA 코드 검증 | 
| POST | `/api/v1/mfa/disable` | MFA 비활성화 | 

#### 사용자 (User)

| 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|--------|-----------|------|---------- |
| GET | `/api/v1/users/me` | 내 정보 조회 |

#### 소셜 로그인 (Social Auth)

| 메서드 | 엔드포인트 | 설명 |         |
|--------|-----------|------|----------|
| POST | `/api/v1/auth/social/google` | Google 로그인 | 
| POST | `/api/v1/auth/social/google/link` | Google 계정 연결 |
| GET | `/api/v1/auth/social/accounts` | 연결된 계정 목록 | 
| DELETE | `/api/v1/auth/social/accounts/{id}` | 계정 연결 해제 |

#### 비밀번호 (Password)

| 메서드 | 엔드포인트 | 설명 |         |
|--------|-----------|------|----------|
| POST | `/api/v1/password/change` | 비밀번호 변경 |  
| POST | `/api/v1/password/resetpwd` | 재설정 요청 | 
| POST | `/api/v1/password/reset` | 비밀번호 재설정 | 
| GET | `/api/v1/password/reset/validate` | 토큰 검증 | 

---

## 아키텍처

### 시스템 아키텍처

```
┌─────────────────────────────────────────────┐
│            애플리케이션                      │
│  (모바일 앱 | Partner console | Console)     │
└──────────────────┬──────────────────────────┘
                   │ HTTPS
                   ▼
┌─────────────────────────────────────────────┐
│         로드 Balancer / API Gateway          │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│          Spring Boot Application            │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │  Controllers (REST API)              │  │
│  └────────────┬─────────────────────────┘  │
│               ▼                            │
│  ┌──────────────────────────────────────┐  │
│  │  Security Layer                      │  │
│  │  - JWT Filter                        │  │
│  │  - Role-Based Access Control         │  │
│  └────────────┬─────────────────────────┘  │
│               ▼                            │
│  ┌──────────────────────────────────────┐  │
│  │  Services                            │  │
│  │  - Auth, MFA, Password, Social       │  │
│  └────────────┬─────────────────────────┘  │
│               ▼                            │
│  ┌──────────────────────────────────────┐  │
│  │  Repositories (Data Access)          │  │
│  └────────────┬─────────────────────────┘  │
└───────────────┼─────────────────────────────┘
                │
    ┌───────────┴──────────┐
    │                      │
    ▼                      ▼
┌──────────┐         ┌──────────┐
│PostgreSQL│         │  Redis   │
│(메인 DB) │         │ (세션)   │
└──────────┘         └──────────┘
```
---

### 주요 시퀀스 다이어그램

#### MFA 설정 및 로그인

```

사용자          백엔드                    Authenticator
  │              │                           │
  │─ MFA 설정 ──▶│                           │
  │              │─ TOTP 시크릿 생성          │
  │              │─ QR 코드 생성              │
  │◀─ QR 코드 ───│                           │
  │              │                           │
  │─────────────────── 스캔 ─────────────────▶│
  │                                          │
  │◀────────── 6자리 코드 생성 ───────────────│
  │              │                           │
  │─ 코드 입력 ──▶│                           │
  │              │─ 코드 확인                 │
  │◀─ MFA 활성화 │                           │
  │              │                           │
  │─ 로그인 ─────▶│                           │
  │              │─ 비밀번호 검증             │
  │◀─ MFA 필요 ──│                           │
  │              │                           │
  │              │                           │
  │◀────────── 새 코드 확인 ──────────────────│
  │              │                           │
  │─ 코드 입력 ──▶│                           │
  │              │─ TOTP 검증                 │
  │◀─ 로그인 완료 │                           │

```
---

## 테스트

시간 제약으로 인해 자동화된 단위/통합 테스트는 구현하지 못했습니다.
대신 다음과 같이 수동 테스트를 진행했습니다:

- Postman을 통한 모든 API 엔드포인트 테스트
- 실제 Google OAuth 플로우 테스트
- 이메일 발송 기능 테스트
- MFA QR 코드 생성 및 검증 테스트
- 큰 회전 및 재사용 방지 테스트
- 계정 잠금 및 해제 시나리오 테스트

---

## 운영 고려사항

### 1. 확장성

#### 수평 확장 가능
- **무상태 JWT 인증**: 서버 세션 없음
- **Redis 세션 저장소**: 분산 환경 지원
- **DB 연결 풀링**: 동시 요청 처리


### 2. 가용성 

#### Health Check
```bash
GET /actuator/health

# 응답
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### 3. 모니터링 

#### 메트릭 수집
- Actuator 엔드포인트
- Prometheus 호환 메트릭
- 애플리케이션 로그

### 4. 로그 관리

#### 로그 파일 로테이션
- 일일 로테이션
- 최대 10GB 보관
- 압축 저장

---

##  예상 운영 대응 시나리오

### 시나리오 1: 대량 로그인 실패

**증상**: 특정 사용자의 로그인 실패 급증

**대응 방법**:
1. 의심스러운 IP 주소 확인
2. 해당 계정 추가 보안 조치
3. Rate Limiting 강화
4. 사용자에게 비밀번호 재설정 안내

**예방**:
- Rate Limiting 구현
- IP 기반 차단 규칙
- 비정상 패턴 알림


### 시나리오 2: JWT 토큰 유출

**증상**: 비정상적인 위치에서 API 호출

**대응 방법**:
1. 해당 사용자의 모든 Refresh Token 폐기:
2. 사용자에게 알림 및 비밀번호 변경 요청
3. 감사 로그 상세 분석

**예방**:
- 짧은 Access Token 만료 시간 (15분)
- IP 주소 기반 이상 탐지


### 시나리오 3: MFA QR 코드 생성 실패

**증상**: QR 코드 이미지가 생성되지 않을때 아래 와 같이 대응을 고려함 

**대응 방법**:

1. 대체 방법 제공:
   - TOTP 시크릿 키를 텍스트로 제공
   - 사용자가 수동으로 입력

2. 라이브러리 확인:

3. 다른 QR 생성 대체:
   - 외부 QR API 사용

**예방**:
- QR 생성 로직 단위 테스트
- 에러 핸들링 강화
- 대체 방법 준비

---
## AI 활용

이 프로젝트는 개발할때 아래 와 같은 부분에서  GPT같은 AI 툴을 활용했습니다.

### 1. 보일러플레이트 코드 생성 
반복적인 엔티티 클래스와 DTO 작성에 AI를 활용했습니다.

Entity 클래스 (User, RefreshToken, SocialAccount 등)
DTO 클래스 (Request/Response 객체)
Repository 인터페이스

### 2. 데이터베이스 마이그레이션 
DDL SQL 쿼리와 Flyway 마이그레이션 스크립트 작성에 AI를 활용했습니다.

### 3. 최신 라이브러리 설정 
라이브러리 최신 버전 설정을 AI의 도움으로 적용했습니다.

### 4. Troubleshooting
개발 중 발생한 여러 에러를 AI와 함께  해결했습니다.

#### 문제 1: JavaMailSender Bean 생성 실패

AI 솔루션:
gradleimplementation 'org.springframework.boot:spring-boot-starter-mail'


#### 문제 2: Swagger 접근 불가

swagger 좀 더 예전 라이브러리 버전으로 바꾸고 문제 해결되었음

#### 문제 4: Google OAuth redirect_uri_mismatch

Error 400: redirect_uri_mismatch

AI 솔루션: Google Console에 정확한 리다이렉트 URI 등록
http://localhost:8080/login/oauth2/code/google

#### 5: Flyway 마이그레이션 실패

AI 솔루션:
- 마이그레이션 파일 순서 확인
- V6 마이그레이션 파일 생성
- 외래 키 의존성 순서 조정



---

## 개발자 정보

- **개발자**: 메리

---
