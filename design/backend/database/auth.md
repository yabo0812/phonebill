# Auth 서비스 데이터베이스 설계서

## 1. 설계 개요

### 1.1 설계 목적
Auth 서비스의 사용자 인증 및 인가 기능 구현을 위한 독립적인 데이터베이스 설계

### 1.2 설계 원칙
- **서비스 독립성**: Auth 서비스 전용 데이터베이스 구성
- **마이크로서비스 패턴**: 다른 서비스와 직접적인 FK 관계 없음
- **캐시 우선 전략**: 타 서비스 데이터는 Redis 캐시로만 참조
- **보안 강화**: 민감 정보 암호화 저장
- **감사 추적**: 모든 인증/인가 활동 이력 관리

### 1.3 주요 기능 요구사항
- **UFR-AUTH-010**: 사용자 로그인 (ID/Password 인증, 계정 잠금)
- **UFR-AUTH-020**: 사용자 인가 (서비스별 접근 권한 확인)

## 2. 데이터베이스 아키텍처

### 2.1 데이터베이스 정보
- **DB 이름**: `phonebill_auth`
- **DBMS**: PostgreSQL 15
- **문자셋**: UTF-8
- **타임존**: Asia/Seoul

### 2.2 서비스 독립성 전략
- **직접 데이터 공유 금지**: 다른 서비스 DB와 직접 연결하지 않음
- **캐시 기반 참조**: 필요한 외부 데이터는 Redis 캐시를 통해서만 접근
- **이벤트 기반 동기화**: 필요 시 메시징을 통한 데이터 동기화

## 3. 테이블 설계

### 3.1 사용자 계정 관리

#### auth_users (사용자 계정)
```sql
-- 사용자 기본 정보 및 인증 정보
CREATE TABLE auth_users (
    user_id VARCHAR(50) PRIMARY KEY,           -- 사용자 ID (로그인 ID)
    password_hash VARCHAR(255) NOT NULL,       -- 암호화된 비밀번호 (BCrypt)
    password_salt VARCHAR(100) NOT NULL,       -- 비밀번호 솔트
    customer_id VARCHAR(50) NOT NULL,          -- 고객 식별자 (외부 참조용)
    line_number VARCHAR(20),                   -- 회선번호 (캐시에서 조회)
    account_status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, LOCKED, SUSPENDED, INACTIVE
    failed_login_count INTEGER DEFAULT 0,      -- 로그인 실패 횟수
    last_failed_login_at TIMESTAMP,           -- 마지막 실패 시간
    account_locked_until TIMESTAMP,           -- 계정 잠금 해제 시간
    last_login_at TIMESTAMP,                  -- 마지막 로그인 시간
    last_password_changed_at TIMESTAMP,       -- 비밀번호 마지막 변경 시간
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_id)
);
```

#### auth_user_sessions (사용자 세션)
```sql
-- 사용자 세션 관리
CREATE TABLE auth_user_sessions (
    session_id VARCHAR(100) PRIMARY KEY,       -- 세션 ID (UUID)
    user_id VARCHAR(50) NOT NULL,             -- 사용자 ID
    session_token VARCHAR(500) NOT NULL,       -- JWT 토큰
    refresh_token VARCHAR(500),                -- 리프레시 토큰
    client_ip VARCHAR(45),                     -- 클라이언트 IP (IPv6 지원)
    user_agent TEXT,                           -- User-Agent 정보
    auto_login_enabled BOOLEAN DEFAULT FALSE,  -- 자동 로그인 여부
    expires_at TIMESTAMP NOT NULL,            -- 세션 만료 시간
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES auth_users(user_id) ON DELETE CASCADE
);
```

### 3.2 권한 관리

#### auth_services (서비스 정의)
```sql
-- 시스템 내 서비스 정의
CREATE TABLE auth_services (
    service_code VARCHAR(30) PRIMARY KEY,      -- 서비스 코드
    service_name VARCHAR(100) NOT NULL,        -- 서비스 이름
    service_description TEXT,                  -- 서비스 설명
    is_active BOOLEAN DEFAULT TRUE,            -- 서비스 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### auth_permissions (권한 정의)
```sql
-- 권한 정의 테이블
CREATE TABLE auth_permissions (
    permission_id SERIAL PRIMARY KEY,          -- 권한 ID
    service_code VARCHAR(30) NOT NULL,         -- 서비스 코드
    permission_code VARCHAR(50) NOT NULL,      -- 권한 코드
    permission_name VARCHAR(100) NOT NULL,     -- 권한 이름
    permission_description TEXT,               -- 권한 설명
    is_active BOOLEAN DEFAULT TRUE,            -- 권한 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_code) REFERENCES auth_services(service_code),
    UNIQUE(service_code, permission_code)
);
```

#### auth_user_permissions (사용자 권한)
```sql
-- 사용자별 권한 할당
CREATE TABLE auth_user_permissions (
    user_permission_id SERIAL PRIMARY KEY,     -- 사용자권한 ID
    user_id VARCHAR(50) NOT NULL,             -- 사용자 ID
    permission_id INTEGER NOT NULL,           -- 권한 ID
    granted_by VARCHAR(50),                   -- 권한 부여자
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,                     -- 권한 만료일 (NULL = 무기한)
    is_active BOOLEAN DEFAULT TRUE,           -- 권한 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES auth_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES auth_permissions(permission_id),
    UNIQUE(user_id, permission_id)
);
```

### 3.3 보안 및 감사

#### auth_login_history (로그인 이력)
```sql
-- 로그인 시도 이력
CREATE TABLE auth_login_history (
    history_id SERIAL PRIMARY KEY,             -- 이력 ID
    user_id VARCHAR(50),                       -- 사용자 ID (실패 시 NULL 가능)
    login_type VARCHAR(20) NOT NULL,           -- LOGIN, LOGOUT, AUTO_LOGIN
    login_status VARCHAR(20) NOT NULL,         -- SUCCESS, FAILURE, LOCKED
    client_ip VARCHAR(45),                     -- 클라이언트 IP
    user_agent TEXT,                           -- User-Agent 정보
    failure_reason VARCHAR(100),               -- 실패 사유
    session_id VARCHAR(100),                   -- 세션 ID (성공 시)
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES auth_users(user_id) ON DELETE SET NULL
);
```

#### auth_permission_access_log (권한 접근 로그)
```sql
-- 권한 기반 접근 로그
CREATE TABLE auth_permission_access_log (
    log_id SERIAL PRIMARY KEY,                 -- 로그 ID
    user_id VARCHAR(50) NOT NULL,             -- 사용자 ID
    service_code VARCHAR(30) NOT NULL,         -- 접근한 서비스
    permission_code VARCHAR(50) NOT NULL,      -- 확인된 권한
    access_status VARCHAR(20) NOT NULL,        -- GRANTED, DENIED
    client_ip VARCHAR(45),                     -- 클라이언트 IP
    session_id VARCHAR(100),                   -- 세션 ID
    requested_resource VARCHAR(200),           -- 요청 리소스
    denial_reason VARCHAR(100),                -- 거부 사유
    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES auth_users(user_id) ON DELETE CASCADE
);
```

## 4. 인덱스 설계

### 4.1 성능 최적화 인덱스
```sql
-- 사용자 조회 최적화
CREATE INDEX idx_auth_users_customer_id ON auth_users(customer_id);
CREATE INDEX idx_auth_users_account_status ON auth_users(account_status);
CREATE INDEX idx_auth_users_last_login ON auth_users(last_login_at);

-- 세션 관리 최적화
CREATE INDEX idx_auth_sessions_user_id ON auth_user_sessions(user_id);
CREATE INDEX idx_auth_sessions_expires_at ON auth_user_sessions(expires_at);
CREATE INDEX idx_auth_sessions_token ON auth_user_sessions(session_token);

-- 권한 조회 최적화
CREATE INDEX idx_auth_user_permissions_user_id ON auth_user_permissions(user_id);
CREATE INDEX idx_auth_user_permissions_active ON auth_user_permissions(user_id, is_active);
CREATE INDEX idx_auth_permissions_service ON auth_permissions(service_code, is_active);

-- 로그 조회 최적화
CREATE INDEX idx_auth_login_history_user_id ON auth_login_history(user_id);
CREATE INDEX idx_auth_login_history_attempted_at ON auth_login_history(attempted_at);
CREATE INDEX idx_auth_permission_log_user_id ON auth_permission_access_log(user_id);
CREATE INDEX idx_auth_permission_log_accessed_at ON auth_permission_access_log(accessed_at);
```

### 4.2 보안 관련 인덱스
```sql
-- 계정 잠금 관련 조회 최적화
CREATE INDEX idx_auth_users_failed_login ON auth_users(failed_login_count, last_failed_login_at);
CREATE INDEX idx_auth_users_locked_until ON auth_users(account_locked_until) WHERE account_locked_until IS NOT NULL;

-- IP 기반 보안 모니터링
CREATE INDEX idx_auth_login_history_ip_status ON auth_login_history(client_ip, login_status, attempted_at);
```

## 5. 제약조건 및 트리거

### 5.1 데이터 무결성 제약조건
```sql
-- 계정 상태 체크 제약조건
ALTER TABLE auth_users ADD CONSTRAINT chk_account_status 
    CHECK (account_status IN ('ACTIVE', 'LOCKED', 'SUSPENDED', 'INACTIVE'));

-- 로그인 상태 체크 제약조건
ALTER TABLE auth_login_history ADD CONSTRAINT chk_login_status 
    CHECK (login_status IN ('SUCCESS', 'FAILURE', 'LOCKED'));

-- 로그인 타입 체크 제약조건
ALTER TABLE auth_login_history ADD CONSTRAINT chk_login_type 
    CHECK (login_type IN ('LOGIN', 'LOGOUT', 'AUTO_LOGIN'));

-- 접근 상태 체크 제약조건
ALTER TABLE auth_permission_access_log ADD CONSTRAINT chk_access_status 
    CHECK (access_status IN ('GRANTED', 'DENIED'));
```

### 5.2 자동 업데이트 트리거
```sql
-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 각 테이블에 updated_at 트리거 적용
CREATE TRIGGER update_auth_users_updated_at BEFORE UPDATE ON auth_users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_auth_services_updated_at BEFORE UPDATE ON auth_services 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_auth_permissions_updated_at BEFORE UPDATE ON auth_permissions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_auth_user_permissions_updated_at BEFORE UPDATE ON auth_user_permissions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## 6. 보안 설계

### 6.1 암호화 전략
- **비밀번호**: BCrypt 해시 + 개별 솔트
- **토큰**: JWT 기반 인증 토큰
- **세션**: 안전한 세션 ID 생성 (UUID)
- **개인정보**: 필요 시 AES-256 암호화

### 6.2 계정 보안 정책
- **계정 잠금**: 5회 연속 실패 시 30분 잠금
- **세션 타임아웃**: 30분 비활성 시 자동 만료
- **토큰 갱신**: 리프레시 토큰을 통한 안전한 토큰 갱신

## 7. 캐시 전략

### 7.1 Redis 캐시 설계
```
Cache Key Pattern: auth:{category}:{identifier}
- auth:user:{user_id} -> 사용자 기본 정보 (TTL: 30분)
- auth:permissions:{user_id} -> 사용자 권한 목록 (TTL: 1시간)
- auth:session:{session_id} -> 세션 정보 (TTL: 세션 만료시간)
- auth:failed_attempts:{user_id} -> 실패 횟수 (TTL: 30분)
```

### 7.2 캐시 무효화 전략
- **권한 변경 시**: 해당 사용자 권한 캐시 삭제
- **계정 잠금/해제 시**: 사용자 정보 캐시 삭제
- **로그아웃 시**: 세션 캐시 삭제

## 8. 데이터 관계도 요약

### 8.1 핵심 관계
- `auth_users` (1) : (N) `auth_user_sessions`
- `auth_users` (1) : (N) `auth_user_permissions`
- `auth_services` (1) : (N) `auth_permissions`
- `auth_permissions` (1) : (N) `auth_user_permissions`
- `auth_users` (1) : (N) `auth_login_history`
- `auth_users` (1) : (N) `auth_permission_access_log`

### 8.2 외부 서비스 연동
- **고객 정보**: Bill-Inquiry 서비스의 고객 데이터를 캐시로만 참조
- **회선 정보**: Product-Change 서비스의 회선 데이터를 캐시로만 참조
- **서비스 메타데이터**: 각 서비스의 메뉴/기능 정보를 캐시로 관리

## 9. 성능 고려사항

### 9.1 예상 데이터 볼륨
- **사용자 수**: 10만 명 (초기), 100만 명 (목표)
- **일일 로그인**: 10만 회
- **세션 동시 접속**: 1만 개
- **로그 보관 기간**: 1년 (압축 보관)

### 9.2 성능 최적화
- **커넥션 풀**: 20개 커넥션 (초기)
- **읽기 전용 복제본**: 조회 성능 향상
- **파티셔닝**: 로그 테이블 월별 파티셔닝
- **아카이빙**: 1년 이상 로그 별도 보관

## 10. 관련 문서
- **ERD 다이어그램**: [auth-erd.puml](./auth-erd.puml)
- **스키마 스크립트**: [auth-schema.psql](./auth-schema.psql)
- **유저스토리**: [../../userstory.md](../../userstory.md)
- **API 설계서**: [../api/auth-service-api.yaml](../api/auth-service-api.yaml)