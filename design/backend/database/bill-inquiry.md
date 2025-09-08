# Bill-Inquiry 서비스 데이터 설계서

## 1. 개요

### 1.1 설계 목적
Bill-Inquiry 서비스의 요금 조회 기능을 위한 독립적인 데이터베이스 설계

### 1.2 설계 원칙
- **서비스 독립성**: Bill-Inquiry 서비스 전용 데이터베이스 구성
- **데이터 격리**: 타 서비스와 데이터 공유 금지, 캐시를 통한 성능 최적화  
- **외래키 제한**: 서비스 내부에서만 FK 관계 설정
- **이력 관리**: 모든 요청/처리 이력의 완전한 추적

### 1.3 주요 기능
- UFR-BILL-010: 요금조회 메뉴 접근
- UFR-BILL-020: 요금조회 신청  
- UFR-BILL-030: KOS 요금조회 서비스 연동
- UFR-BILL-040: 요금조회 결과 전송

## 2. 데이터베이스 구성

### 2.1 데이터베이스 정보
- **데이터베이스명**: bill_inquiry_db
- **DBMS**: PostgreSQL 14
- **문자셋**: UTF8
- **타임존**: Asia/Seoul

### 2.2 스키마 구성
- **public**: 기본 스키마 (비즈니스 테이블)
- **cache**: 캐시 데이터 스키마 (Redis 보조용)
- **audit**: 감사 및 이력 스키마

## 3. 테이블 설계

### 3.1 고객정보 테이블 (customer_info)
**목적**: 캐시에서 가져온 고객 기본 정보 임시 저장

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| customer_id | VARCHAR(50) | PRIMARY KEY | 고객 식별자 |
| line_number | VARCHAR(20) | NOT NULL | 회선번호 |
| customer_name | VARCHAR(100) | | 고객명 |
| status | VARCHAR(10) | NOT NULL DEFAULT 'ACTIVE' | 고객상태 (ACTIVE, INACTIVE) |
| operator_code | VARCHAR(10) | NOT NULL | 사업자 코드 |
| cached_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 캐시 저장 시각 |
| expires_at | TIMESTAMP | NOT NULL | 캐시 만료 시각 |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정일시 |

### 3.2 요금조회 요청 이력 테이블 (bill_inquiry_history)
**목적**: MVNO에서 MP로의 요금조회 요청 이력 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 이력 ID |
| request_id | VARCHAR(50) | NOT NULL UNIQUE | 요청 식별자 |
| user_id | VARCHAR(50) | NOT NULL | 요청 사용자 ID |
| line_number | VARCHAR(20) | NOT NULL | 회선번호 |
| inquiry_month | VARCHAR(7) | | 조회월 (YYYY-MM, null이면 당월) |
| request_time | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 요청일시 |
| process_time | TIMESTAMP | | 처리완료일시 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PROCESSING' | 처리상태 |
| result_summary | TEXT | | 결과 요약 |
| bill_info_json | JSONB | | 요금정보 JSON |
| error_message | TEXT | | 오류 메시지 |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정일시 |

**인덱스**:
- `idx_bill_history_user_line`: (user_id, line_number)
- `idx_bill_history_request_time`: (request_time DESC)
- `idx_bill_history_status`: (status)
- `idx_bill_history_inquiry_month`: (inquiry_month)

**상태값 (status)**:
- `PROCESSING`: 처리중
- `COMPLETED`: 완료
- `FAILED`: 실패
- `TIMEOUT`: 타임아웃

### 3.3 KOS 연동 이력 테이블 (kos_inquiry_history)
**목적**: MP에서 KOS로의 요금조회 연동 이력 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 이력 ID |
| bill_request_id | VARCHAR(50) | | 요금조회 요청 ID (FK) |
| line_number | VARCHAR(20) | NOT NULL | 회선번호 |
| inquiry_month | VARCHAR(7) | | 조회월 |
| request_time | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | KOS 요청일시 |
| response_time | TIMESTAMP | | KOS 응답일시 |
| result_code | VARCHAR(10) | | KOS 응답코드 |
| result_message | TEXT | | KOS 응답메시지 |
| kos_data_json | JSONB | | KOS 응답 데이터 JSON |
| error_detail | TEXT | | 오류 상세 정보 |
| retry_count | INTEGER | NOT NULL DEFAULT 0 | 재시도 횟수 |
| circuit_breaker_state | VARCHAR(20) | | Circuit Breaker 상태 |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정일시 |

**인덱스**:
- `idx_kos_history_line_month`: (line_number, inquiry_month)
- `idx_kos_history_request_time`: (request_time DESC)
- `idx_kos_history_result_code`: (result_code)
- `idx_kos_history_bill_request`: (bill_request_id)

### 3.4 요금정보 캐시 테이블 (bill_info_cache)
**목적**: KOS에서 조회한 요금정보의 임시 캐시 (Redis 보조용)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| cache_key | VARCHAR(100) | PRIMARY KEY | 캐시 키 (line_number:inquiry_month) |
| line_number | VARCHAR(20) | NOT NULL | 회선번호 |
| inquiry_month | VARCHAR(7) | NOT NULL | 조회월 |
| bill_info_json | JSONB | NOT NULL | 요금정보 JSON |
| cached_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 캐시 저장 시각 |
| expires_at | TIMESTAMP | NOT NULL | 캐시 만료 시각 |
| access_count | INTEGER | NOT NULL DEFAULT 1 | 접근 횟수 |
| last_accessed_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 최종 접근 시각 |

**인덱스**:
- `idx_cache_line_month`: (line_number, inquiry_month)
- `idx_cache_expires`: (expires_at)

### 3.5 시스템 설정 테이블 (system_config)
**목적**: Bill-Inquiry 서비스 관련 시스템 설정 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| config_key | VARCHAR(100) | PRIMARY KEY | 설정 키 |
| config_value | TEXT | NOT NULL | 설정 값 |
| description | VARCHAR(500) | | 설정 설명 |
| config_type | VARCHAR(20) | NOT NULL DEFAULT 'STRING' | 설정 타입 |
| is_active | BOOLEAN | NOT NULL DEFAULT true | 활성화 여부 |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정일시 |

**설정 예시**:
- `bill.cache.ttl.hours`: 요금정보 캐시 TTL (기본 4시간)
- `kos.connection.timeout.ms`: KOS 연결 타임아웃
- `kos.retry.max.attempts`: KOS 최대 재시도 횟수
- `bill.inquiry.available.months`: 조회 가능한 개월 수

## 4. 외래키 관계

### 4.1 서비스 내부 관계
- `kos_inquiry_history.bill_request_id` → `bill_inquiry_history.request_id`
  - KOS 연동 이력과 요금조회 요청 이력 연결
  - ON DELETE CASCADE로 요금조회 이력 삭제 시 KOS 이력도 삭제

### 4.2 외부 서비스와의 관계
- **Auth 서비스**: user_id는 참조만 하고 FK 관계 설정하지 않음
- **캐시 데이터**: Redis를 통한 데이터 공유, DB 직접 참조 없음

## 5. 캐시 전략

### 5.1 Redis 캐시 키 전략
- **고객정보**: `customer:info:{user_id}` (TTL: 1시간)
- **요금정보**: `bill:info:{line_number}:{inquiry_month}` (TTL: 4시간)
- **가용조회월**: `bill:available:months` (TTL: 24시간)

### 5.2 캐시 무효화 정책
- 요금조회 완료 시: 해당 회선/월 캐시 갱신
- 고객정보 변경 시: 고객정보 캐시 삭제
- 시스템 설정 변경 시: 관련 캐시 전체 삭제

## 6. 데이터 보안

### 6.1 개인정보 보호
- **암호화 컬럼**: customer_name, bill_info_json
- **접근 제어**: 사용자별 회선번호 권한 확인
- **로그 마스킹**: 개인정보 포함 로그는 마스킹 처리

### 6.2 데이터 보관 정책
- **요금조회 이력**: 2년 보관 후 아카이브
- **KOS 연동 이력**: 1년 보관 후 삭제
- **캐시 데이터**: TTL 만료 후 자동 삭제
- **오류 로그**: 6개월 보관

## 7. 성능 최적화

### 7.1 인덱스 전략
- **복합 인덱스**: 자주 함께 조회되는 컬럼들
- **부분 인덱스**: 활성 데이터만 대상으로 하는 인덱스
- **JSONB 인덱스**: 요금정보 JSON 검색용 GIN 인덱스

### 7.2 파티셔닝 전략
- **bill_inquiry_history**: 월별 파티셔닝 (request_time 기준)
- **kos_inquiry_history**: 월별 파티셔닝 (request_time 기준)

### 7.3 통계 정보 관리
- **자동 통계 수집**: 주요 테이블 자동 분석
- **쿼리 플랜 모니터링**: 성능 저하 쿼리 식별

## 8. 모니터링 및 알람

### 8.1 성능 모니터링
- 테이블별 용량 및 성장률 추적
- 슬로우 쿼리 모니터링
- 캐시 히트율 모니터링

### 8.2 비즈니스 모니터링  
- 요금조회 성공률
- KOS 연동 응답시간
- Circuit Breaker 상태

## 9. 데이터 백업 및 복구

### 9.1 백업 전략
- **전체 백업**: 주 1회 (일요일 새벽)
- **증분 백업**: 일 1회 (매일 새벽)
- **트랜잭션 로그 백업**: 15분마다

### 9.2 복구 전략
- **Point-in-Time 복구**: 특정 시점 데이터 복구
- **테이블 단위 복구**: 개별 테이블 복구
- **응급 복구**: 1시간 내 서비스 복구

## 10. 관련 산출물

- **ERD 설계서**: [bill-inquiry-erd.puml](./bill-inquiry-erd.puml)
- **스키마 스크립트**: [bill-inquiry-schema.psql](./bill-inquiry-schema.psql)
- **API 설계서**: [../api/bill-inquiry-service-api.yaml](../api/bill-inquiry-service-api.yaml)
- **클래스 설계서**: [../class/bill-inquiry.puml](../class/bill-inquiry.puml)