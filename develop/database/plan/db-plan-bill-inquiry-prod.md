# Bill-Inquiry 서비스 운영환경 데이터베이스 설치 계획서

## 1. 개요

### 1.1 설치 목적
- Bill-Inquiry 서비스의 운영환경 데이터베이스 구성
- Azure Database for PostgreSQL Flexible Server 활용한 관리형 데이터베이스 구축
- 고가용성, 고성능, 엔터프라이즈급 보안을 제공하는 운영환경 데이터베이스 시스템 구축

### 1.2 대상 서비스
- **서비스명**: Bill-Inquiry Service (요금 조회 서비스)  
- **데이터베이스**: `bill_inquiry_db`
- **운영환경**: Azure 운영환경 (99.9% 가용성 목표)
- **예상 사용량**: Peak 1,000 동시 사용자 지원

### 1.3 참조 문서
- 물리 아키텍처 설계서: `design/backend/physical/physical-architecture-prod.md`
- 데이터 설계서: `design/backend/database/bill-inquiry.md`
- 데이터 설계 종합: `design/backend/database/data-design-summary.md`
- 스키마 스크립트: `design/backend/database/bill-inquiry-schema.psql`

## 2. Azure Database for PostgreSQL Flexible Server 구성

### 2.1 기본 설정

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 서버 이름 | phonebill-bill-inquiry-prod-pg | Bill-Inquiry 운영환경 PostgreSQL |
| 리전 | Korea Central | 주 리전 |
| PostgreSQL 버전 | 14 | 안정적인 LTS 버전 |
| 서비스 티어 | General Purpose | 범용 용도 (운영환경) |
| 컴퓨팅 크기 | Standard_D4s_v3 | 4 vCPU, 16GB RAM |
| 스토리지 | 256GB Premium SSD | 고성능 SSD, 자동 확장 활성화 |

### 2.2 고가용성 구성

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 고가용성 모드 | Zone Redundant HA | 영역 간 중복화 |
| 주 가용 영역 | 1 | Korea Central 가용 영역 1 |
| 대기 가용 영역 | 2 | Korea Central 가용 영역 2 |
| 자동 장애조치 | 활성화 | 60초 이내 자동 장애조치 |
| SLA | 99.99% | 고가용성 보장 |

### 2.3 백업 및 복구

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 백업 보존 기간 | 35일 | 최대 보존 기간 |
| 지리적 중복 백업 | 활성화 | Korea South 리전에 복제 |
| Point-in-Time 복구 | 활성화 | 5분 단위 복구 가능 |
| 자동 백업 시간 | 02:00 KST | 트래픽이 적은 시간대 |

## 3. 네트워크 및 보안 구성

### 3.1 네트워크 설정

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 연결 방법 | Private Access (VNet 통합) | VNet 내부 전용 접근 |
| 가상 네트워크 | phonebill-vnet | 기존 VNet 활용 |
| 서브넷 | Database Subnet (10.0.2.0/24) | 데이터베이스 전용 서브넷 |
| Private Endpoint | 활성화 | 보안 강화된 연결 |
| DNS 영역 | privatelink.postgres.database.azure.com | Private DNS 영역 |

### 3.2 보안 설정

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| TLS 버전 | 1.2 이상 | 암호화 통신 강제 |
| SSL 강제 | 활성화 | 비암호화 연결 차단 |
| 방화벽 규칙 | VNet 내부만 허용 | AKS 서브넷만 접근 허용 |
| 인증 방법 | PostgreSQL Authentication | 기본 인증 + Azure AD 통합 |
| 암호화 | AES-256 | 저장 데이터 암호화 (TDE) |

### 3.3 Azure AD 통합

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| Azure AD 인증 | 활성화 | 관리형 ID 지원 |
| AD 관리자 | phonebill-admin | Azure AD 기반 관리자 |
| 서비스 주체 | bill-inquiry-service-identity | 애플리케이션용 관리형 ID |

## 4. 데이터베이스 및 사용자 구성

### 4.1 데이터베이스 생성

```sql
-- 메인 데이터베이스 생성
CREATE DATABASE bill_inquiry_db
    WITH ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- 타임존 설정
ALTER DATABASE bill_inquiry_db SET timezone TO 'Asia/Seoul';

-- 확장 모듈 설치
\c bill_inquiry_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
```

### 4.2 사용자 및 권한 설정

```sql
-- 애플리케이션 사용자 생성
CREATE USER bill_app_user WITH PASSWORD 'Complex#Password#2025!';

-- 읽기 전용 사용자 생성 (모니터링/분석용)
CREATE USER bill_readonly_user WITH PASSWORD 'ReadOnly#Password#2025!';

-- 백업 전용 사용자 생성
CREATE USER bill_backup_user WITH PASSWORD 'Backup#Password#2025!';

-- 애플리케이션 사용자 권한
GRANT CONNECT ON DATABASE bill_inquiry_db TO bill_app_user;
GRANT USAGE ON SCHEMA public TO bill_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO bill_app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO bill_app_user;

-- 읽기 전용 사용자 권한
GRANT CONNECT ON DATABASE bill_inquiry_db TO bill_readonly_user;
GRANT USAGE ON SCHEMA public TO bill_readonly_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO bill_readonly_user;

-- 기본 권한 설정 (신규 테이블에 자동 적용)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bill_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO bill_readonly_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO bill_app_user;
```

## 5. 성능 최적화 설정

### 5.1 PostgreSQL 파라미터 튜닝

```sql
-- 연결 풀링 설정
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';

-- 메모리 설정 (16GB RAM 기준)
ALTER SYSTEM SET shared_buffers = '4GB';
ALTER SYSTEM SET effective_cache_size = '12GB';
ALTER SYSTEM SET work_mem = '64MB';
ALTER SYSTEM SET maintenance_work_mem = '1GB';

-- 체크포인트 설정
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET max_wal_size = '4GB';
ALTER SYSTEM SET min_wal_size = '1GB';

-- 로깅 설정
ALTER SYSTEM SET log_min_duration_statement = 1000;
ALTER SYSTEM SET log_checkpoints = on;
ALTER SYSTEM SET log_connections = on;
ALTER SYSTEM SET log_disconnections = on;

-- 통계 수집 설정
ALTER SYSTEM SET track_activities = on;
ALTER SYSTEM SET track_counts = on;
ALTER SYSTEM SET track_io_timing = on;

-- 설정 적용
SELECT pg_reload_conf();
```

### 5.2 연결 풀링 구성

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 최대 연결 수 | 200 | 동시 연결 제한 |
| HikariCP Pool Size | 15 | 애플리케이션 연결 풀 크기 |
| 연결 타임아웃 | 30초 | 연결 획득 타임아웃 |
| 유휴 타임아웃 | 10분 | 유휴 연결 해제 시간 |
| 최대 라이프타임 | 30분 | 연결 최대 생존 시간 |

## 6. 스키마 및 데이터 초기화

### 6.1 스키마 적용

```bash
# 스키마 파일 적용
psql -h phonebill-bill-inquiry-prod-pg.postgres.database.azure.com \
     -U bill_app_user \
     -d bill_inquiry_db \
     -f design/backend/database/bill-inquiry-schema.psql
```

### 6.2 초기 데이터 확인

```sql
-- 테이블 생성 확인
SELECT table_name, table_type 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- 인덱스 생성 확인
SELECT schemaname, tablename, indexname 
FROM pg_indexes 
WHERE schemaname = 'public' 
ORDER BY tablename, indexname;

-- 시스템 설정 확인
SELECT config_key, config_value, description 
FROM system_config 
WHERE is_active = true 
ORDER BY config_key;
```

## 7. 읽기 전용 복제본 구성

### 7.1 읽기 복제본 생성

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 복제본 이름 | phonebill-bill-inquiry-prod-pg-replica | 읽기 전용 복제본 |
| 리전 | Korea South | 재해복구용 다른 리전 |
| 컴퓨팅 크기 | Standard_D2s_v3 | 2 vCPU, 8GB RAM (읽기용) |
| 스토리지 | 256GB Premium SSD | 마스터와 동일 |
| 용도 | 읽기 부하 분산 및 재해복구 | - |

### 7.2 읽기 복제본 활용

```yaml
application_config:
  # Spring Boot DataSource 설정 예시
  datasource:
    master:
      url: jdbc:postgresql://phonebill-bill-inquiry-prod-pg.postgres.database.azure.com:5432/bill_inquiry_db
      username: bill_app_user
      
    readonly:
      url: jdbc:postgresql://phonebill-bill-inquiry-prod-pg-replica.postgres.database.azure.com:5432/bill_inquiry_db
      username: bill_readonly_user
      
  # 읽기/쓰기 분리 라우팅
  routing:
    write_operations: master
    read_operations: readonly
    analytics_queries: readonly
```

## 8. 모니터링 및 알림 설정

### 8.1 Azure Monitor 통합

| 모니터링 항목 | 알림 임계값 | 대응 방안 |
|--------------|-------------|----------|
| CPU 사용률 | 85% 이상 | Auto-scaling 또는 수동 스케일업 |
| 메모리 사용률 | 90% 이상 | 연결 최적화 또는 스케일업 |
| 디스크 사용률 | 80% 이상 | 스토리지 자동 확장 |
| 연결 수 | 180개 이상 (90%) | 연결 풀 튜닝 |
| 응답 시간 | 500ms 이상 | 쿼리 최적화 검토 |
| 실패한 연결 | 10회/분 이상 | 네트워크 및 보안 설정 점검 |

### 8.2 로그 분석 설정

```sql
-- 슬로우 쿼리 모니터링
SELECT query, calls, total_time, rows, 100.0 * shared_blks_hit /
       nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
WHERE total_time > 60000  -- 1분 이상 쿼리
ORDER BY total_time DESC 
LIMIT 10;

-- 데이터베이스 통계
SELECT datname, numbackends, xact_commit, xact_rollback, 
       blks_read, blks_hit,
       100.0 * blks_hit / (blks_hit + blks_read) as cache_hit_ratio
FROM pg_stat_database 
WHERE datname = 'bill_inquiry_db';
```

## 9. 백업 및 재해복구 계획

### 9.1 백업 전략

| 백업 유형 | 주기 | 보존 기간 | 위치 |
|----------|------|-----------|------|
| 자동 백업 | 일 1회 (02:00 KST) | 35일 | Azure 백업 스토리지 |
| 지리적 백업 | 자동 복제 | 35일 | Korea South 리전 |
| Point-in-Time | 연속 | 35일 내 5분 단위 | WAL 로그 기반 |
| 논리적 백업 | 주 1회 (일요일) | 3개월 | Azure Blob Storage |

### 9.2 재해복구 절차

#### RTO/RPO 목표
- **RTO (복구 시간 목표)**: 30분 이내
- **RPO (복구 지점 목표)**: 5분 이내

#### 장애 시나리오별 대응

1. **주 서버 장애**
   - Azure 자동 장애조치 (60초 이내)
   - DNS 업데이트 (자동)
   - 애플리케이션 재연결 (자동)

2. **리전 전체 장애**
   - 읽기 복제본을 마스터로 승격
   - 애플리케이션 설정 변경
   - 트래픽 라우팅 변경

3. **데이터 손상**
   - Point-in-Time 복구 수행
   - 별도 서버에서 복구 후 전환
   - 데이터 무결성 검증

## 10. 보안 강화 방안

### 10.1 접근 제어

```sql
-- 특권 사용자 역할 생성
CREATE ROLE bill_admin;
GRANT ALL PRIVILEGES ON DATABASE bill_inquiry_db TO bill_admin;

-- 개발자 역할 생성 (제한적 권한)
CREATE ROLE bill_developer;
GRANT CONNECT ON DATABASE bill_inquiry_db TO bill_developer;
GRANT USAGE ON SCHEMA public TO bill_developer;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO bill_developer;

-- 감사 역할 생성
CREATE ROLE bill_auditor;
GRANT CONNECT ON DATABASE bill_inquiry_db TO bill_auditor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO bill_auditor;
```

### 10.2 데이터 암호화

| 암호화 유형 | 구현 방법 | 대상 데이터 |
|------------|-----------|-------------|
| 저장 데이터 암호화 | TDE (투명한 데이터 암호화) | 모든 테이블 데이터 |
| 전송 데이터 암호화 | TLS 1.2+ | 클라이언트-서버 간 통신 |
| 컬럼 수준 암호화 | AES-256 | 고객명, 요금정보 등 민감정보 |
| 백업 암호화 | AES-256 | 모든 백업 파일 |

### 10.3 감사 설정

```sql
-- 감사 로그 활성화
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_line_prefix = '%t [%p]: user=%u,db=%d,app=%a,client=%h ';
ALTER SYSTEM SET log_lock_waits = on;
ALTER SYSTEM SET log_temp_files = 10240;  -- 10MB 이상 임시 파일 로그

-- pg_audit 확장 설치 (필요시)
-- CREATE EXTENSION pg_audit;
-- ALTER SYSTEM SET pg_audit.log = 'write,ddl';
```

## 11. 비용 최적화

### 11.1 예상 비용 (월간, USD)

| 구성 요소 | 사양 | 예상 비용 | 최적화 방안 |
|----------|------|-----------|-------------|
| 메인 서버 | Standard_D4s_v3 | $450 | Reserved Instance (1년 약정 20% 절약) |
| 읽기 복제본 | Standard_D2s_v3 | $225 | 필요시에만 활성화 |
| 스토리지 (256GB) | Premium SSD | $50 | 사용량 기반 자동 확장 |
| 백업 스토리지 | 지리적 중복 | $20 | 보존 기간 최적화 |
| 네트워킹 | 데이터 전송 | $10 | VNet 내부 통신 활용 |
| **총 예상 비용** | | **$755** | **Reserved Instance 시 $605** |

### 11.2 비용 모니터링

```sql
-- 리소스 사용량 모니터링 쿼리
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 인덱스 사용률 확인
SELECT 
    t.tablename,
    i.indexname,
    i.idx_tup_read,
    i.idx_tup_fetch,
    pg_size_pretty(pg_relation_size(i.indexname::regclass)) as index_size
FROM pg_stat_user_indexes i
JOIN pg_stat_user_tables t ON i.relid = t.relid
WHERE i.idx_tup_read = 0
ORDER BY pg_relation_size(i.indexname::regclass) DESC;
```

## 12. 설치 실행 계획

### 12.1 설치 단계

| 단계 | 작업 내용 | 예상 시간 | 담당자 |
|------|-----------|----------|--------|
| 1 | Azure PostgreSQL Flexible Server 생성 | 30분 | 데옵스 |
| 2 | 네트워크 및 보안 설정 | 20분 | 데옵스 |
| 3 | 고가용성 및 백업 설정 | 15분 | 데옵스 |
| 4 | 데이터베이스 및 사용자 생성 | 10분 | 백엔더 |
| 5 | 스키마 적용 및 초기화 | 15분 | 백엔더 |
| 6 | 읽기 복제본 생성 | 20분 | 데옵스 |
| 7 | 모니터링 및 알림 설정 | 30분 | 데옵스 |
| 8 | 성능 테스트 및 튜닝 | 60분 | 백엔더/QA매니저 |
| **총 예상 시간** | | **3시간 20분** | |

### 12.2 사전 준비사항

```yaml
prerequisites:
  azure_resources:
    - Resource Group: phonebill-rg
    - Virtual Network: phonebill-vnet
    - Database Subnet: 10.0.2.0/24
    - Private DNS Zone: privatelink.postgres.database.azure.com
    
  azure_permissions:
    - Contributor role on Resource Group
    - Network Contributor role on VNet
    - PostgreSQL Flexible Server Contributor
    
  network_connectivity:
    - AKS cluster network access
    - Azure CLI access from deployment machine
    - psql client tools installed
```

### 12.3 설치 스크립트

```bash
#!/bin/bash
# Bill-Inquiry 서비스 PostgreSQL 설치 스크립트

# 변수 설정
RESOURCE_GROUP="phonebill-rg"
SERVER_NAME="phonebill-bill-inquiry-prod-pg"
LOCATION="koreacentral"
ADMIN_USER="postgres"
ADMIN_PASSWORD="Complex#PostgreSQL#2025!"
DATABASE_NAME="bill_inquiry_db"

# PostgreSQL Flexible Server 생성
az postgres flexible-server create \
    --resource-group $RESOURCE_GROUP \
    --name $SERVER_NAME \
    --location $LOCATION \
    --admin-user $ADMIN_USER \
    --admin-password "$ADMIN_PASSWORD" \
    --sku-name Standard_D4s_v3 \
    --tier GeneralPurpose \
    --storage-size 256 \
    --storage-auto-grow Enabled \
    --version 14 \
    --zone 1 \
    --high-availability ZoneRedundant \
    --standby-zone 2

# 데이터베이스 생성
az postgres flexible-server db create \
    --resource-group $RESOURCE_GROUP \
    --server-name $SERVER_NAME \
    --database-name $DATABASE_NAME

# VNet 통합 설정
az postgres flexible-server vnet-rule create \
    --resource-group $RESOURCE_GROUP \
    --name allow-aks-subnet \
    --server-name $SERVER_NAME \
    --vnet-name phonebill-vnet \
    --subnet database-subnet

# 백업 설정
az postgres flexible-server parameter set \
    --resource-group $RESOURCE_GROUP \
    --server-name $SERVER_NAME \
    --name backup_retention_days \
    --value 35

echo "PostgreSQL Flexible Server 설치 완료"
echo "Server: $SERVER_NAME.postgres.database.azure.com"
echo "Database: $DATABASE_NAME"
```

## 13. 테스트 계획

### 13.1 기능 테스트

```sql
-- 연결 테스트
\conninfo

-- 기본 성능 테스트
SELECT pg_size_pretty(pg_database_size('bill_inquiry_db')) as db_size;

-- 테이블 생성 및 CRUD 테스트
INSERT INTO system_config (config_key, config_value, description) 
VALUES ('test.config', 'test_value', 'Test configuration');

SELECT * FROM system_config WHERE config_key = 'test.config';

DELETE FROM system_config WHERE config_key = 'test.config';
```

### 13.2 성능 테스트

```bash
# pgbench를 이용한 성능 테스트
pgbench -i -s 10 bill_inquiry_db -h $SERVER_NAME.postgres.database.azure.com -U bill_app_user
pgbench -c 50 -j 2 -T 300 bill_inquiry_db -h $SERVER_NAME.postgres.database.azure.com -U bill_app_user
```

### 13.3 장애복구 테스트

1. **계획된 장애조치 테스트**
   - Azure Portal에서 수동 장애조치 수행
   - 애플리케이션 연결 상태 확인
   - 복구 시간 측정

2. **백업 복구 테스트**
   - Point-in-Time 복구 수행
   - 데이터 무결성 검증
   - 복구 시간 측정

## 14. 운영 가이드

### 14.1 일상 운영 점검

```yaml
daily_checklist:
  - [ ] 서버 상태 및 가용성 확인
  - [ ] CPU/메모리/디스크 사용률 점검
  - [ ] 백업 성공 여부 확인
  - [ ] 슬로우 쿼리 로그 검토
  - [ ] 오류 로그 검토
  - [ ] 연결 수 및 성능 지표 확인

weekly_checklist:
  - [ ] 장애조치 메커니즘 테스트
  - [ ] 백업 복구 테스트 수행
  - [ ] 성능 통계 분석 및 튜닝
  - [ ] 보안 패치 적용 검토
  - [ ] 용량 계획 검토

monthly_checklist:
  - [ ] 전체 시스템 성능 검토
  - [ ] 비용 최적화 기회 분석
  - [ ] 재해복구 계획 업데이트
  - [ ] 보안 감사 수행
```

### 14.2 긴급 대응 절차

```yaml
incident_response:
  severity_1: # 서비스 중단
    - immediate_action: 자동 장애조치 확인
    - notification: 운영팀 즉시 알림
    - escalation: 15분 내 관리자 호출
    - recovery_target: 30분 내 서비스 복구
    
  severity_2: # 성능 저하
    - analysis: 성능 지표 분석
    - optimization: 쿼리 튜닝 또는 리소스 증설
    - timeline: 2시간 내 해결
    
  severity_3: # 경미한 문제
    - monitoring: 지속적 모니터링
    - planning: 다음 정기 점검 시 해결
    - timeline: 24시간 내 계획 수립
```

## 15. 결론

본 설치 계획서는 Bill-Inquiry 서비스의 운영환경에서 요구되는 고가용성, 고성능, 엔터프라이즈급 보안을 만족하는 Azure Database for PostgreSQL Flexible Server 구성을 제시합니다.

### 15.1 주요 특징

- **고가용성**: Zone Redundant HA로 99.99% 가용성 보장
- **성능 최적화**: Premium SSD, 읽기 복제본, 연결 풀링
- **보안 강화**: VNet 통합, TLS 암호화, Azure AD 인증
- **재해복구**: 35일 백업 보존, 지리적 중복, Point-in-Time 복구
- **비용 효율성**: Reserved Instance 활용으로 20% 비용 절약

### 15.2 다음 단계

1. 본 계획서 검토 및 승인 ✅
2. Azure 리소스 생성 및 구성 수행
3. 스키마 적용 및 초기화 실행
4. 성능 테스트 및 튜닝 수행
5. 모니터링 시스템 구축
6. 운영 문서 작성 및 교육

---

**작성일**: 2025-09-08  
**작성자**: 데옵스 (최운영), 백엔더 (이개발)  
**검토자**: 아키텍트 (김기획), QA매니저 (정테스트)  
**승인자**: 기획자 (김기획)