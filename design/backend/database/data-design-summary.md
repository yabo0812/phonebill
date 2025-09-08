# 통신요금 관리 서비스 - 데이터 설계 종합

## 데이터 설계 요약

### 🎯 설계 목적
통신요금 관리 서비스의 마이크로서비스 아키텍처에서 각 서비스별 독립적인 데이터베이스 설계를 통해 데이터 독립성과 서비스 간 결합도를 최소화하고, 성능과 보안을 최적화한 데이터 아키텍처 구현

### 🏗️ 마이크로서비스 데이터 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Auth Service  │    │ Bill-Inquiry    │    │ Product-Change  │
│                 │    │   Service       │    │   Service       │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ phonebill_auth  │    │ bill_inquiry_db │    │product_change_db│
│                 │    │                 │    │                 │
│ • auth_users    │    │ • customer_info │    │ • pc_product_   │
│ • auth_services │    │ • bill_inquiry_ │    │   change_       │
│ • auth_permiss  │    │   history       │    │   history       │
│ • user_permiss  │    │ • kos_inquiry_  │    │ • pc_kos_       │
│ • login_history │    │   history       │    │   integration_  │
│ • permission_   │    │ • bill_info_    │    │   log           │
│   access_log    │    │   cache         │    │ • pc_circuit_   │
│ • auth_user_    │    │ • system_config │    │   breaker_state │
│   sessions      │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
                    ┌─────────────────┐
                    │  Redis Cache    │
                    │                 │
                    │ • 고객정보 캐시   │
                    │ • 상품정보 캐시   │
                    │ • 세션 정보      │
                    │ • 권한 정보      │
                    └─────────────────┘
```

### 📊 서비스별 데이터베이스 구성

#### 1. Auth Service (인증/인가)
- **데이터베이스**: `phonebill_auth`
- **핵심 테이블**: 7개
- **주요 기능**:
  - 사용자 인증 (BCrypt 암호화)
  - 계정 잠금 관리 (5회 실패 → 30분 잠금)
  - 권한 기반 접근 제어
  - 세션 관리 (JWT + 자동로그인)
  - 감사 로그 (로그인/권한 접근 이력)

#### 2. Bill-Inquiry Service (요금조회)
- **데이터베이스**: `bill_inquiry_db`  
- **핵심 테이블**: 5개
- **주요 기능**:
  - 요금조회 요청 이력 관리
  - KOS 시스템 연동 로그 추적
  - 조회 결과 캐싱 (성능 최적화)
  - 고객정보 임시 캐시
  - 시스템 설정 관리

#### 3. Product-Change Service (상품변경)
- **데이터베이스**: `product_change_db`
- **핵심 테이블**: 3개
- **주요 기능**:
  - 상품변경 이력 관리 (Entity 매핑)
  - KOS 연동 로그 추적
  - Circuit Breaker 상태 관리
  - 상품/고객정보 캐싱

### 🔐 데이터 독립성 원칙 구현

#### 서비스 간 데이터 분리
```yaml
데이터_독립성:
  - 각_서비스_전용_DB: 완전 분리된 데이터베이스
  - FK_관계_금지: 서비스 간 외래키 관계 없음
  - 캐시_기반_참조: Redis를 통한 외부 데이터 참조
  - 이벤트_동기화: 필요시 이벤트 기반 데이터 동기화

서비스_내부_관계만_허용:
  Auth:
    - auth_users ↔ auth_user_sessions
    - auth_permissions ↔ auth_user_permissions
  Bill-Inquiry:
    - bill_inquiry_history ↔ kos_inquiry_history
  Product-Change:
    - pc_product_change_history (단일 테이블 중심)
```

### ⚡ 성능 최적화 전략

#### 캐시 전략 (Redis)
```yaml
캐시_TTL_정책:
  고객정보: 4시간
  상품정보: 2시간
  세션정보: 24시간
  권한정보: 8시간
  가용상품목록: 24시간
  회선상태: 30분

캐시_키_전략:
  - "customer:{lineNumber}"
  - "product:{productCode}"
  - "session:{userId}"
  - "permissions:{userId}"
```

#### 인덱싱 전략
```yaml
전략적_인덱스:
  Auth: 20개 (성능 + 보안)
  Bill-Inquiry: 15개 (조회 성능)
  Product-Change: 12개 (이력 관리)

특수_인덱스:
  - JSONB_GIN_인덱스: JSON 데이터 검색
  - 복합_인덱스: 다중 컬럼 조회 최적화
  - 부분_인덱스: 조건부 데이터 최적화
```

#### 파티셔닝 준비
```yaml
파티셔닝_전략:
  월별_파티셔닝:
    - 이력_테이블: request_time 기준
    - 로그_테이블: created_at 기준
  자동_파티션_생성:
    - 트리거_기반_월별_파티션_생성
    - 3개월_이전_파티션_아카이브
```

### 🛡️ 보안 설계

#### 데이터 보호
```yaml
암호화:
  - 비밀번호: BCrypt + Salt
  - 민감정보: AES-256 컬럼 암호화
  - 전송구간: TLS 1.3

접근_제어:
  - 역할_기반_권한: RBAC 모델
  - 서비스_계정: 최소_권한_원칙
  - DB_접근: 연결풀_보안_설정

감사_추적:
  - 로그인_이력: 성공/실패 모든 기록
  - 권한_접근: 권한별 접근 로그
  - 데이터_변경: 모든 변경사항 추적
```

### 📈 모니터링 및 운영

#### 모니터링 지표
```yaml
성능_지표:
  - DB_응답시간: < 100ms
  - 캐시_히트율: > 90%
  - 동시_접속자: 실시간 모니터링

비즈니스_지표:
  - 요금조회_성공률: > 99%
  - 상품변경_성공률: > 95%
  - KOS_연동_성공률: > 98%

시스템_지표:
  - Circuit_Breaker_상태
  - 재시도_횟수
  - 오류_발생률
```

#### 백업 및 복구
```yaml
백업_전략:
  - 전체_백업: 주간 (일요일 02:00)
  - 증분_백업: 일간 (03:00)
  - 트랜잭션_로그: 15분간격

데이터_보관정책:
  - 요금조회_이력: 2년
  - 상품변경_이력: 3년
  - 로그인_이력: 1년
  - KOS_연동로그: 1년
  - 시스템_로그: 6개월
```

### 🔧 기술 스택

```yaml
데이터베이스:
  - 주_DB: PostgreSQL 14
  - 캐시: Redis 7
  - 연결풀: HikariCP

기술_특징:
  - JSONB: 유연한_데이터_구조
  - 트리거: 자동_업데이트_관리
  - 뷰: 복잡_쿼리_단순화
  - 함수: 비즈니스_로직_캡슐화

성능_도구:
  - 파티셔닝: 대용량_데이터_처리
  - 인덱싱: 쿼리_성능_최적화
  - 캐싱: Redis_활용_성능_향상
```

### 📋 결과물 목록

#### 설계 문서
- `auth.md` - Auth 서비스 데이터 설계서
- `bill-inquiry.md` - Bill-Inquiry 서비스 데이터 설계서  
- `product-change.md` - Product-Change 서비스 데이터 설계서

#### ERD 다이어그램
- `auth-erd.puml` - Auth 서비스 ERD
- `bill-inquiry-erd.puml` - Bill-Inquiry 서비스 ERD
- `product-change-erd.puml` - Product-Change 서비스 ERD

#### 스키마 스크립트
- `auth-schema.psql` - Auth 서비스 PostgreSQL 스키마
- `bill-inquiry-schema.psql` - Bill-Inquiry 서비스 PostgreSQL 스키마
- `product-change-schema.psql` - Product-Change 서비스 PostgreSQL 스키마

### 🎯 설계 완료 확인사항

✅ **데이터독립성원칙 준수**: 각 서비스별 독립된 데이터베이스  
✅ **클래스설계 연계**: Entity 클래스와 1:1 매핑 완료  
✅ **PlantUML 문법검사**: 모든 ERD 파일 검사 통과  
✅ **실행가능 스크립트**: 바로 실행 가능한 PostgreSQL DDL  
✅ **캐시전략 설계**: Redis 활용 성능 최적화 방안  
✅ **보안설계 완료**: 암호화, 접근제어, 감사추적 포함  
✅ **성능최적화**: 인덱싱, 파티셔닝, 캐싱 전략 완비  

## 다음 단계

1. **데이터베이스 설치**: 각 서비스별 PostgreSQL 인스턴스 설치
2. **Redis 설치**: 캐시 서버 설치 및 설정
3. **스키마 적용**: DDL 스크립트 실행
4. **모니터링 설정**: 성능 모니터링 도구 구성
5. **백업 설정**: 자동 백업 시스템 구성

---

**설계 완료일**: `2025-09-08`  
**설계자**: 백엔더 (이개발)  
**검토자**: 아키텍트 (김기획), QA매니저 (정테스트)