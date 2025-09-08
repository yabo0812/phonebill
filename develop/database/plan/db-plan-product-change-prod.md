# Product-Change 서비스 운영환경 데이터베이스 설치 계획서

## 1. 개요

### 1.1 프로젝트 정보
- **프로젝트명**: 통신요금 관리 서비스
- **서비스명**: Product-Change Service (상품변경)
- **환경**: 운영환경 (Production)
- **데이터베이스명**: product_change_db
- **작성일**: 2025-09-08
- **작성자**: 데옵스 (최운영)

### 1.2 설치 목적
- Product-Change 서비스의 운영환경 전용 데이터베이스 구축
- Azure Database for PostgreSQL Flexible Server를 활용한 고가용성 구성
- 99.9% 가용성을 목표로 한 엔터프라이즈급 데이터베이스 환경 제공
- 1,000명 동시 사용자 지원 및 성능 최적화

### 1.3 설계 원칙
- **고가용성 우선**: Zone Redundant HA로 99.9% 가용성 보장
- **보안 강화**: Private Endpoint, TLS 1.3, 감사 로깅 적용
- **성능 최적화**: Premium SSD, Read Replica, 자동 인덱스 관리
- **재해복구**: 자동 백업, Point-in-Time Recovery, 지리적 복제
- **모니터링**: 포괄적 메트릭 수집 및 알림 설정

## 2. 아키텍처 설계

### 2.1 전체 아키텍처
```
┌─────────────────────────────────────────────────────────────┐
│                    운영환경 데이터 아키텍처                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │ Product-Change  │    │ AKS Cluster     │                │
│  │ Application     │◄──►│ (Multi-Zone)    │                │
│  │ Pods            │    │ Korea Central   │                │
│  └─────────────────┘    └─────────────────┘                │
│           │                       │                        │
│           │ Private Endpoint      │                        │
│           ▼                       │                        │
│  ┌─────────────────┐              │                        │
│  │ Azure Database  │              │                        │
│  │ for PostgreSQL  │              │                        │
│  │ Flexible Server │              │                        │
│  │ (Zone Redundant)│              │                        │
│  └─────────────────┘              │                        │
│           │                       │                        │
│           │ Read Traffic          │                        │
│           ▼                       │                        │
│  ┌─────────────────┐              │                        │
│  │ Read Replica    │              │                        │
│  │ (Korea South)   │              │                        │
│  └─────────────────┘              │                        │
│                                   │                        │
│  ┌─────────────────┐              │                        │
│  │ Azure Cache     │◄─────────────┘                        │
│  │ for Redis       │                                       │
│  │ (Premium P2)    │                                       │
│  └─────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 데이터베이스 구성 요소

#### 2.2.1 주 데이터베이스
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **서비스 유형** | Azure Database for PostgreSQL Flexible Server | 관리형 PostgreSQL 서비스 |
| **위치** | Korea Central | 주 리전 |
| **PostgreSQL 버전** | 14 | 안정화된 최신 버전 |
| **서비스 티어** | GeneralPurpose | 범용 프로덕션 환경 |
| **컴퓨팅 사이즈** | Standard_D4s_v3 | 4 vCore, 16GB RAM |
| **스토리지** | 256GB Premium SSD | 고성능 스토리지 |
| **IOPS** | 1,280 (자동 확장) | 고성능 I/O |
| **데이터베이스명** | product_change_db | Product-Change 전용 DB |

#### 2.2.2 고가용성 구성
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **HA 모드** | Zone Redundant HA | 영역 간 중복화 |
| **Primary Zone** | Zone 1 | 주 데이터베이스 영역 |
| **Standby Zone** | Zone 2 | 대기 데이터베이스 영역 |
| **자동 장애조치** | 활성화 | 60초 이내 자동 전환 |
| **복제 모드** | 동기식 복제 | 데이터 일관성 보장 |
| **가용성 SLA** | 99.95% | Zone Redundant SLA |

#### 2.2.3 읽기 전용 복제본
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **위치** | Korea South | 재해복구 리전 |
| **복제본 수** | 1개 | 읽기 부하 분산용 |
| **컴퓨팅 사이즈** | Standard_D2s_v3 | 2 vCore, 8GB RAM |
| **복제 지연** | < 1분 | 실시간에 가까운 복제 |
| **사용 목적** | 읽기 부하 분산, 재해복구 | 성능 및 가용성 향상 |

### 2.3 네트워크 구성

#### 2.3.1 네트워크 보안
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **네트워크 액세스** | Private Access (VNet) | VNet 내부 접근만 허용 |
| **Private Endpoint** | 활성화 | 10.0.2.0/24 서브넷 |
| **Private DNS Zone** | privatelink.postgres.database.azure.com | 내부 DNS 해석 |
| **방화벽 규칙** | VNet 규칙만 | AKS 서브넷에서만 접근 허용 |
| **SSL 암호화** | 필수 (TLS 1.3) | 전송 구간 암호화 |

#### 2.3.2 연결 설정
```yaml
database_connection:
  # 주 데이터베이스 연결
  primary:
    host: "phonebill-postgresql-prod.postgres.database.azure.com"
    port: 5432
    database: "product_change_db"
    ssl_mode: "require"
    connect_timeout: 30
    
  # 읽기 전용 복제본 연결
  read_replica:
    host: "phonebill-postgresql-replica.postgres.database.azure.com"
    port: 5432
    database: "product_change_db"
    ssl_mode: "require"
    connect_timeout: 30
```

## 3. 스토리지 및 성능 최적화

### 3.1 스토리지 구성

#### 3.1.1 스토리지 설정
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **스토리지 유형** | Premium SSD | 고성능 스토리지 |
| **초기 용량** | 256GB | 서비스 시작 용량 |
| **최대 용량** | 16TB | 자동 확장 상한 |
| **자동 확장** | 활성화 | 80% 사용 시 자동 확장 |
| **증분 단위** | 64GB | 확장 단위 |
| **IOPS** | 1,280 (기본) | 자동 확장 가능 |

#### 3.1.2 성능 튜닝 매개변수
```sql
-- PostgreSQL 운영환경 최적화 매개변수
# 메모리 설정
shared_buffers = '4GB'                    # 전체 메모리의 25%
effective_cache_size = '12GB'             # 사용 가능한 메모리의 75%
work_mem = '32MB'                         # 정렬/해시 작업용 메모리
maintenance_work_mem = '512MB'            # 유지보수 작업용 메모리

# 연결 및 인증
max_connections = 200                     # 최대 동시 연결 수
idle_in_transaction_session_timeout = '30min'  # 유휴 트랜잭션 타임아웃

# 체크포인트 및 WAL
checkpoint_completion_target = 0.9        # 체크포인트 완료 목표
wal_buffers = '16MB'                     # WAL 버퍼 크기
max_wal_size = '4GB'                     # 최대 WAL 크기

# 로깅 설정
log_statement = 'all'                    # 모든 SQL 로깅 (운영환경)
log_duration = on                        # 쿼리 실행 시간 로깅
log_slow_queries = on                    # 느린 쿼리 로깅
log_min_duration_statement = 1000        # 1초 이상 쿼리 로깅

# 통계 및 모니터링
track_activities = on                    # 활동 추적
track_counts = on                       # 통계 수집
track_functions = all                   # 함수 통계
shared_preload_libraries = 'pg_stat_statements'  # 쿼리 통계
```

### 3.2 인덱스 전략

#### 3.2.1 핵심 인덱스 설계
```sql
-- 상품변경 이력 테이블 인덱스 (성능 최적화)
-- 1. 회선번호 + 처리상태 + 요청일시 (복합 인덱스)
CREATE INDEX idx_pc_history_line_status_date 
ON pc_product_change_history(line_number, process_status, requested_at DESC);

-- 2. 고객ID + 요청일시 (고객별 이력 조회)
CREATE INDEX idx_pc_history_customer_date 
ON pc_product_change_history(customer_id, requested_at DESC);

-- 3. 처리상태 + 요청일시 (상태별 모니터링)
CREATE INDEX idx_pc_history_status_date 
ON pc_product_change_history(process_status, requested_at DESC);

-- 4. JSONB 데이터 검색용 GIN 인덱스
CREATE INDEX idx_pc_history_kos_request_gin 
ON pc_product_change_history USING GIN(kos_request_data);

CREATE INDEX idx_pc_history_kos_response_gin 
ON pc_product_change_history USING GIN(kos_response_data);

-- KOS 연동 로그 테이블 인덱스
-- 1. 요청ID + 연동유형 + 생성일시
CREATE INDEX idx_kos_log_request_type_date 
ON pc_kos_integration_log(request_id, integration_type, created_at DESC);

-- 2. 연동유형 + 성공여부 + 생성일시 (성공률 모니터링)
CREATE INDEX idx_kos_log_type_success_date 
ON pc_kos_integration_log(integration_type, is_success, created_at DESC);

-- 3. 응답시간 성능 분석용 인덱스
CREATE INDEX idx_kos_log_response_time 
ON pc_kos_integration_log(integration_type, response_time_ms DESC, created_at DESC) 
WHERE response_time_ms IS NOT NULL;
```

## 4. 보안 설계

### 4.1 인증 및 권한 관리

#### 4.1.1 데이터베이스 사용자 계정
```sql
-- 1. 애플리케이션 사용자 (운영)
CREATE USER product_change_app WITH 
    PASSWORD 'PCApp2025Prod@#'
    CONNECTION LIMIT 150
    VALID UNTIL 'infinity';

-- 2. 읽기 전용 사용자 (모니터링/분석)
CREATE USER product_change_readonly WITH 
    PASSWORD 'PCRead2025Prod@#'
    CONNECTION LIMIT 20
    VALID UNTIL 'infinity';

-- 3. 관리자 사용자 (DBA)
CREATE USER product_change_admin WITH 
    PASSWORD 'PCAdmin2025Prod@#'
    CONNECTION LIMIT 10
    VALID UNTIL 'infinity'
    CREATEDB CREATEROLE;
```

#### 4.1.2 권한 설정
```sql
-- 애플리케이션 사용자 권한 (최소 권한 원칙)
GRANT CONNECT ON DATABASE product_change_db TO product_change_app;
GRANT USAGE ON SCHEMA product_change TO product_change_app;
GRANT SELECT, INSERT, UPDATE ON TABLE product_change.pc_product_change_history TO product_change_app;
GRANT SELECT, INSERT ON TABLE product_change.pc_kos_integration_log TO product_change_app;
GRANT SELECT, UPDATE ON TABLE product_change.pc_circuit_breaker_state TO product_change_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA product_change TO product_change_app;

-- 읽기 전용 사용자 권한
GRANT CONNECT ON DATABASE product_change_db TO product_change_readonly;
GRANT USAGE ON SCHEMA product_change TO product_change_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA product_change TO product_change_readonly;

-- 관리자 사용자 권한 (전체 권한)
GRANT ALL PRIVILEGES ON DATABASE product_change_db TO product_change_admin;
GRANT ALL PRIVILEGES ON SCHEMA product_change TO product_change_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA product_change TO product_change_admin;
```

### 4.2 데이터 암호화

#### 4.2.1 저장 데이터 암호화
| 암호화 유형 | 설정 값 | 설명 |
|------------|---------|------|
| **TDE (Transparent Data Encryption)** | 활성화 | 데이터파일, 로그파일 암호화 |
| **암호화 알고리즘** | AES-256 | 업계 표준 암호화 |
| **키 관리** | Azure Key Vault 통합 | 중앙 집중식 키 관리 |
| **키 회전** | 매년 자동 | 보안 정책 준수 |

#### 4.2.2 전송 데이터 암호화
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **SSL/TLS** | TLS 1.3 (최신) | 전송 구간 암호화 |
| **SSL 모드** | require | SSL 연결 강제 |
| **인증서 검증** | 활성화 | 서버 인증서 검증 |
| **클라이언트 인증서** | 고려 사항 | 양방향 SSL (필요시) |

### 4.3 감사 및 모니터링

#### 4.3.1 감사 로깅
```sql
-- 감사 로깅 설정
ALTER SYSTEM SET log_statement = 'all';                    -- 모든 SQL 로깅
ALTER SYSTEM SET log_connections = on;                     -- 연결 로깅
ALTER SYSTEM SET log_disconnections = on;                  -- 연결 해제 로깅
ALTER SYSTEM SET log_duration = on;                        -- 실행 시간 로깅
ALTER SYSTEM SET log_hostname = on;                        -- 호스트명 로깅
ALTER SYSTEM SET log_line_prefix = '%t [%p]: user=%u,db=%d,app=%a,client=%h ';  -- 로그 형식

-- 로그 보관 설정
ALTER SYSTEM SET log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log';  -- 로그 파일명 형식
ALTER SYSTEM SET log_file_mode = 0640;                     -- 로그 파일 권한
ALTER SYSTEM SET log_rotation_age = '1d';                  -- 1일 단위 로그 회전
ALTER SYSTEM SET log_rotation_size = '100MB';              -- 100MB 단위 로그 회전
ALTER SYSTEM SET log_truncate_on_rotation = off;           -- 로그 파일 유지
```

#### 4.3.2 보안 정책
```yaml
security_policies:
  password_policy:
    min_length: 12
    complexity: "uppercase, lowercase, number, special char"
    expiry: 90 days
    history: 5 passwords
    
  connection_security:
    max_failed_attempts: 5
    lockout_duration: 30 minutes
    session_timeout: 8 hours
    idle_timeout: 30 minutes
    
  network_security:
    allowed_subnets:
      - "10.0.1.0/24"  # AKS Application Subnet
      - "10.0.4.0/24"  # Management Subnet
    blocked_countries: []  # 필요시 지역 차단
    rate_limiting: 100 connections/minute
```

## 5. 백업 및 재해복구

### 5.1 백업 전략

#### 5.1.1 자동 백업 설정
| 백업 유형 | 설정 값 | 설명 |
|----------|---------|------|
| **자동 백업** | 활성화 | Azure 관리형 자동 백업 |
| **백업 보존 기간** | 35일 | 최대 보존 기간 |
| **백업 시간** | 02:00-04:00 KST | 비즈니스 영향 최소화 |
| **백업 압축** | 활성화 | 스토리지 비용 절약 |
| **백업 암호화** | 활성화 | AES-256 암호화 |

#### 5.1.2 백업 유형별 설정
```yaml
backup_configuration:
  # 전체 백업
  full_backup:
    frequency: "매일"
    time: "02:00 KST"
    retention: "35일"
    compression: true
    encryption: "AES-256"
    
  # 트랜잭션 로그 백업
  log_backup:
    frequency: "5분"
    retention: "7일"
    compression: true
    
  # Point-in-Time Recovery
  pitr:
    enabled: true
    granularity: "5분"
    retention: "35일"
    
  # 지리적 복제 백업
  geo_backup:
    enabled: true
    target_region: "Korea South"
    retention: "35일"
```

### 5.2 재해복구 계획

#### 5.2.1 복구 목표
| 복구 지표 | 목표 값 | 설명 |
|----------|---------|------|
| **RTO (Recovery Time Objective)** | 30분 | 서비스 복구 목표 시간 |
| **RPO (Recovery Point Objective)** | 1시간 | 데이터 손실 허용 범위 |
| **복구 우선순위** | 높음 | 비즈니스 크리티컬 서비스 |
| **장애조치 방식** | 자동 + 수동 | HA는 자동, 지역 간은 수동 |

#### 5.2.2 재해복구 시나리오
```yaml
disaster_recovery_scenarios:
  # 시나리오 1: 단일 가용성 영역 장애
  zone_failure:
    detection: "자동 (Azure Monitor)"
    response: "자동 장애조치 (60초)"
    rto: "2분"
    rpo: "0분"
    action: "Zone Redundant HA 활성화"
    
  # 시나리오 2: 전체 리전 장애
  region_failure:
    detection: "수동 확인 필요"
    response: "수동 장애조치"
    rto: "30분"
    rpo: "1시간"
    action: "읽기 복제본을 마스터로 승격"
    
  # 시나리오 3: 데이터 손상
  data_corruption:
    detection: "모니터링 알림 또는 사용자 신고"
    response: "Point-in-Time Recovery"
    rto: "4시간"
    rpo: "손상 발생 시점까지"
    action: "특정 시점으로 데이터베이스 복원"
```

#### 5.2.3 복구 절차
```yaml
recovery_procedures:
  # 자동 장애조치 (Zone Redundant HA)
  automatic_failover:
    - step: "1. 장애 감지 (헬스 체크 실패)"
      duration: "30초"
    - step: "2. Standby 승격 결정"
      duration: "15초"  
    - step: "3. DNS 업데이트 및 트래픽 전환"
      duration: "15초"
    - step: "4. 애플리케이션 연결 재시도"
      duration: "자동"
      
  # 수동 지역 간 장애조치
  manual_failover:
    - step: "1. 주 리전 장애 확인"
      responsible: "DBA/운영팀"
    - step: "2. 읽기 복제본 상태 확인"
      responsible: "DBA"
    - step: "3. 복제본을 마스터로 승격"
      responsible: "DBA"
      command: "az postgres flexible-server replica promote"
    - step: "4. 애플리케이션 연결 문자열 업데이트"
      responsible: "개발팀"
    - step: "5. DNS 레코드 업데이트"
      responsible: "네트워크팀"
    - step: "6. 서비스 상태 확인"
      responsible: "운영팀"
```

## 6. 모니터링 및 알림

### 6.1 모니터링 지표

#### 6.1.1 시스템 메트릭
| 메트릭 분류 | 지표명 | 임계값 | 알림 레벨 |
|------------|-------|--------|----------|
| **CPU 사용률** | cpu_percent | > 80% | Warning |
| **메모리 사용률** | memory_percent | > 85% | Warning |
| **스토리지 사용률** | storage_percent | > 75% | Warning |
| **IOPS 사용률** | iops_percent | > 80% | Warning |
| **연결 수** | active_connections | > 150 | Critical |
| **복제 지연** | replica_lag_seconds | > 300 | Critical |

#### 6.1.2 성능 메트릭
| 메트릭 분류 | 지표명 | 목표값 | 임계값 |
|------------|-------|--------|--------|
| **평균 응답시간** | avg_query_time | < 100ms | > 500ms |
| **트랜잭션 처리량** | transactions_per_second | > 100 TPS | < 50 TPS |
| **캐시 적중률** | buffer_cache_hit_ratio | > 95% | < 90% |
| **데드락 발생률** | deadlock_rate | 0 | > 5/hour |
| **슬로우 쿼리 비율** | slow_query_percentage | < 1% | > 5% |

#### 6.1.3 비즈니스 메트릭
```yaml
business_metrics:
  # 상품변경 성공률
  product_change_success_rate:
    target: "> 95%"
    warning: "< 90%"
    critical: "< 80%"
    measurement: "성공한 요청 / 전체 요청 * 100"
    
  # KOS 연동 성공률
  kos_integration_success_rate:
    target: "> 98%"
    warning: "< 95%"
    critical: "< 90%"
    measurement: "성공한 연동 / 전체 연동 * 100"
    
  # 평균 처리시간
  avg_processing_time:
    target: "< 5초"
    warning: "> 10초"
    critical: "> 30초"
    measurement: "처리완료시간 - 요청시간"
```

### 6.2 알림 설정

#### 6.2.1 알림 채널
| 채널 유형 | 용도 | 대상 |
|----------|------|------|
| **Microsoft Teams** | 실시간 알림 | 운영팀, 개발팀 |
| **Email** | 중요 알림 | DBA, 관리자 |
| **SMS** | 긴급 알림 | 담당자 |
| **Azure Monitor** | 자동 스케일링 | 시스템 |

#### 6.2.2 알림 규칙
```yaml
alert_rules:
  # Critical 알림 (즉시 대응 필요)
  critical_alerts:
    - name: "데이터베이스 연결 실패"
      condition: "connection_failed > 0"
      duration: "1분"
      channels: ["teams", "sms"]
      
    - name: "복제 지연 임계 초과"
      condition: "replica_lag > 300초"
      duration: "2분"
      channels: ["teams", "email"]
      
    - name: "자동 장애조치 발생"
      condition: "failover_event = true"
      duration: "즉시"
      channels: ["teams", "sms", "email"]
      
  # Warning 알림 (주의 감시)
  warning_alerts:
    - name: "CPU 사용률 높음"
      condition: "cpu_percent > 80%"
      duration: "5분"
      channels: ["teams"]
      
    - name: "스토리지 사용률 높음"
      condition: "storage_percent > 75%"
      duration: "10분"
      channels: ["teams", "email"]
      
    - name: "느린 쿼리 증가"
      condition: "slow_query_count > 10/분"
      duration: "5분"
      channels: ["teams"]
```

### 6.3 대시보드 구성

#### 6.3.1 운영 대시보드
```yaml
operational_dashboard:
  # 실시간 상태
  real_time_status:
    - "데이터베이스 상태 (Primary/Standby)"
    - "현재 연결 수"
    - "진행 중인 트랜잭션 수"
    - "복제 지연 시간"
    
  # 성능 지표
  performance_metrics:
    - "CPU/메모리 사용률 (시계열)"
    - "IOPS 및 처리량 (시계열)"
    - "쿼리 응답시간 분포"
    - "슬로우 쿼리 TOP 10"
    
  # 비즈니스 지표
  business_metrics:
    - "상품변경 성공률 (일/주/월)"
    - "KOS 연동 성공률 (일/주/월)"
    - "사용자별 활동 통계"
    - "오류 발생 추이"
```

## 7. 설치 및 구성

### 7.1 Azure 리소스 생성

#### 7.1.1 리소스 그룹 및 네트워킹
```bash
# 1. 리소스 그룹 생성
az group create \
  --name rg-phonebill-prod \
  --location koreacentral

# 2. Virtual Network 생성 (이미 존재하는 경우 스킵)
az network vnet create \
  --resource-group rg-phonebill-prod \
  --name vnet-phonebill-prod \
  --address-prefix 10.0.0.0/16

# 3. 데이터베이스 서브넷 생성
az network vnet subnet create \
  --resource-group rg-phonebill-prod \
  --vnet-name vnet-phonebill-prod \
  --name subnet-database \
  --address-prefix 10.0.2.0/24 \
  --delegations Microsoft.DBforPostgreSQL/flexibleServers
```

#### 7.1.2 PostgreSQL Flexible Server 생성
```bash
# 1. 주 데이터베이스 서버 생성
az postgres flexible-server create \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-prod \
  --location koreacentral \
  --admin-user dbadmin \
  --admin-password 'ProductChange2025Prod@#$' \
  --sku-name Standard_D4s_v3 \
  --tier GeneralPurpose \
  --storage-size 256 \
  --storage-auto-grow Enabled \
  --version 14 \
  --high-availability ZoneRedundant \
  --standby-zone 2 \
  --backup-retention 35 \
  --geo-redundant-backup Enabled \
  --vnet vnet-phonebill-prod \
  --subnet subnet-database \
  --private-dns-zone phonebill-prod.private.postgres.database.azure.com

# 2. 데이터베이스 생성
az postgres flexible-server db create \
  --resource-group rg-phonebill-prod \
  --server-name phonebill-postgresql-prod \
  --database-name product_change_db
```

#### 7.1.3 읽기 전용 복제본 생성
```bash
# 읽기 전용 복제본 생성 (Korea South)
az postgres flexible-server replica create \
  --resource-group rg-phonebill-prod \
  --replica-name phonebill-postgresql-replica \
  --source-server phonebill-postgresql-prod \
  --location koreasouth \
  --sku-name Standard_D2s_v3
```

### 7.2 데이터베이스 초기 설정

#### 7.2.1 스키마 및 초기 데이터 생성
```bash
# 1. 스키마 파일 적용
psql -h phonebill-postgresql-prod.postgres.database.azure.com \
     -U dbadmin \
     -d product_change_db \
     -f design/backend/database/product-change-schema.psql

# 2. 초기 설정 확인
psql -h phonebill-postgresql-prod.postgres.database.azure.com \
     -U dbadmin \
     -d product_change_db \
     -c "\dt product_change.*"
```

#### 7.2.2 성능 튜닝 매개변수 적용
```bash
# PostgreSQL 서버 매개변수 설정
az postgres flexible-server parameter set \
  --resource-group rg-phonebill-prod \
  --server-name phonebill-postgresql-prod \
  --name shared_buffers --value 4194304  # 4GB

az postgres flexible-server parameter set \
  --resource-group rg-phonebill-prod \
  --server-name phonebill-postgresql-prod \
  --name effective_cache_size --value 12582912  # 12GB

az postgres flexible-server parameter set \
  --resource-group rg-phonebill-prod \
  --server-name phonebill-postgresql-prod \
  --name work_mem --value 32768  # 32MB

az postgres flexible-server parameter set \
  --resource-group rg-phonebill-prod \
  --server-name phonebill-postgresql-prod \
  --name max_connections --value 200
```

### 7.3 보안 구성

#### 7.3.1 방화벽 및 네트워크 규칙
```bash
# 1. AKS 서브넷에서의 접근 허용
az postgres flexible-server firewall-rule create \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-prod \
  --rule-name allow-aks-subnet \
  --start-ip-address 10.0.1.0 \
  --end-ip-address 10.0.1.255

# 2. SSL 강제 설정
az postgres flexible-server parameter set \
  --resource-group rg-phonebill-prod \
  --server-name phonebill-postgresql-prod \
  --name require_secure_transport --value on
```

#### 7.3.2 사용자 계정 및 권한 설정
```sql
-- 애플리케이션별 사용자 생성 스크립트 실행
psql -h phonebill-postgresql-prod.postgres.database.azure.com \
     -U dbadmin \
     -d product_change_db \
     -c "
-- 애플리케이션 사용자 생성 및 권한 부여
CREATE USER product_change_app WITH PASSWORD 'PCApp2025Prod@#';
GRANT CONNECT ON DATABASE product_change_db TO product_change_app;
GRANT USAGE ON SCHEMA product_change TO product_change_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA product_change TO product_change_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA product_change TO product_change_app;

-- 읽기 전용 사용자 생성
CREATE USER product_change_readonly WITH PASSWORD 'PCRead2025Prod@#';
GRANT CONNECT ON DATABASE product_change_db TO product_change_readonly;
GRANT USAGE ON SCHEMA product_change TO product_change_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA product_change TO product_change_readonly;
"
```

### 7.4 모니터링 설정

#### 7.4.1 Azure Monitor 통합
```bash
# 1. Log Analytics 워크스페이스 생성
az monitor log-analytics workspace create \
  --resource-group rg-phonebill-prod \
  --workspace-name law-phonebill-prod \
  --location koreacentral

# 2. 진단 설정 활성화
az monitor diagnostic-settings create \
  --resource-group rg-phonebill-prod \
  --name diagnostic-postgresql \
  --resource phonebill-postgresql-prod \
  --resource-type Microsoft.DBforPostgreSQL/flexibleServers \
  --workspace law-phonebill-prod \
  --logs '[{"category":"PostgreSQLLogs","enabled":true}]' \
  --metrics '[{"category":"AllMetrics","enabled":true}]'
```

#### 7.4.2 알림 규칙 생성
```bash
# CPU 사용률 높음 알림
az monitor metrics alert create \
  --resource-group rg-phonebill-prod \
  --name alert-high-cpu \
  --description "데이터베이스 CPU 사용률이 80%를 초과했습니다" \
  --severity 2 \
  --condition "avg cpu_percent > 80" \
  --window-size 5m \
  --evaluation-frequency 1m \
  --target-resource-id "/subscriptions/{subscription-id}/resourceGroups/rg-phonebill-prod/providers/Microsoft.DBforPostgreSQL/flexibleServers/phonebill-postgresql-prod"

# 연결 수 임계 초과 알림
az monitor metrics alert create \
  --resource-group rg-phonebill-prod \
  --name alert-high-connections \
  --description "데이터베이스 연결 수가 150을 초과했습니다" \
  --severity 1 \
  --condition "avg active_connections > 150" \
  --window-size 1m \
  --evaluation-frequency 1m \
  --target-resource-id "/subscriptions/{subscription-id}/resourceGroups/rg-phonebill-prod/providers/Microsoft.DBforPostgreSQL/flexibleServers/phonebill-postgresql-prod"
```

## 8. 운영 절차

### 8.1 일상 운영 체크리스트

#### 8.1.1 일일 점검 항목
```yaml
daily_checklist:
  system_health:
    - [ ] 데이터베이스 서비스 상태 확인
    - [ ] Primary/Standby 상태 정상 여부
    - [ ] 복제 지연 시간 확인 (< 1분)
    - [ ] CPU/메모리/스토리지 사용률 확인
    
  performance_monitoring:
    - [ ] 슬로우 쿼리 로그 검토
    - [ ] 대기 이벤트 분석
    - [ ] 연결 풀 상태 확인
    - [ ] 캐시 적중률 확인
    
  security_audit:
    - [ ] 로그인 실패 시도 검토
    - [ ] 권한 변경 이력 확인
    - [ ] 비정상 접근 패턴 검토
    
  backup_verification:
    - [ ] 자동 백업 성공 여부 확인
    - [ ] 백업 파일 무결성 검사
    - [ ] 복구 테스트 (주 1회)
```

#### 8.1.2 주간 점검 항목
```yaml
weekly_checklist:
  capacity_planning:
    - [ ] 스토리지 증가 추세 분석
    - [ ] 트랜잭션 볼륨 추세 분석
    - [ ] 동시 사용자 수 추세 분석
    
  performance_optimization:
    - [ ] 인덱스 사용률 분석
    - [ ] 쿼리 계획 변경 검토
    - [ ] 통계 정보 업데이트 상태 확인
    
  security_maintenance:
    - [ ] 패치 적용 가능 여부 확인
    - [ ] 사용자 계정 정기 검토
    - [ ] 인증서 만료일 확인
```

### 8.2 장애 대응 절차

#### 8.2.1 장애 심각도 분류
| 심각도 | 설명 | 대응시간 | 에스컬레이션 |
|--------|------|-----------|-------------|
| **P1 (Critical)** | 서비스 완전 중단 | 15분 | 즉시 관리팀 호출 |
| **P2 (High)** | 성능 심각 저하 | 1시간 | 업무시간 내 대응 |
| **P3 (Medium)** | 부분적 기능 장애 | 4시간 | 정규 업무시간 대응 |
| **P4 (Low)** | 경미한 성능 저하 | 24시간 | 다음 정기 점검 시 |

#### 8.2.2 P1 장애 대응 절차
```yaml
p1_incident_response:
  immediate_actions:
    - step: "1. 장애 상황 파악 및 확인"
      duration: "5분"
      responsible: "운영팀"
      
    - step: "2. 관리팀 및 개발팀 즉시 호출"
      duration: "즉시"
      responsible: "운영팀"
      
    - step: "3. 장애 원인 초기 분석"
      duration: "10분"
      responsible: "DBA"
      
  recovery_actions:
    - step: "4. 자동 장애조치 상태 확인"
      duration: "2분"
      action: "Zone Redundant HA 동작 확인"
      
    - step: "5. 수동 장애조치 결정"
      duration: "5분"
      condition: "자동 장애조치 실패 시"
      
    - step: "6. 읽기 복제본으로 긴급 전환"
      duration: "10분"
      condition: "주 리전 전체 장애 시"
      
  communication:
    - step: "7. 고객 공지 발송"
      duration: "장애 확인 후 30분 내"
      responsible: "CS팀"
      
    - step: "8. 복구 상황 업데이트"
      frequency: "30분마다"
      responsible: "운영팀"
```

### 8.3 정기 유지보수

#### 8.3.1 월간 유지보수 작업
```yaml
monthly_maintenance:
  performance_optimization:
    - "통계 정보 수동 업데이트"
    - "미사용 인덱스 정리"
    - "슬로우 쿼리 패턴 분석 및 최적화"
    - "파티션 정리 (12개월 이전 로그)"
    
  security_hardening:
    - "사용자 계정 정기 검토"
    - "권한 최소화 원칙 적용"
    - "패치 적용 계획 수립"
    - "취약점 스캔 실시"
    
  capacity_management:
    - "향후 6개월 용량 예측"
    - "스토리지 확장 계획 수립"
    - "성능 개선 방안 도출"
    - "비용 최적화 검토"
```

## 9. 비용 관리

### 9.1 예상 비용 분석

#### 9.1.1 월간 운영 비용 (USD)
| 구성 요소 | 사양 | 월간 비용 | 연간 비용 |
|----------|------|-----------|-----------|
| **Primary DB** | Standard_D4s_v3, 256GB, HA | $820 | $9,840 |
| **Read Replica** | Standard_D2s_v3, 256GB | $290 | $3,480 |
| **백업 스토리지** | 35일 보존, 지리적 복제 | $150 | $1,800 |
| **네트워크 비용** | Private Endpoint, 데이터 전송 | $80 | $960 |
| **모니터링** | Log Analytics, 메트릭 수집 | $60 | $720 |
| **총 예상 비용** | | **$1,400** | **$16,800** |

#### 9.1.2 비용 최적화 방안
```yaml
cost_optimization:
  # Reserved Instance 활용
  reserved_instances:
    savings: "~30%"
    commitment: "1년 또는 3년"
    estimated_savings: "$4,200/년"
    
  # 스토리지 최적화
  storage_optimization:
    - "로그 데이터 정기 정리"
    - "백업 압축률 향상"
    - "불필요한 인덱스 제거"
    estimated_savings: "$600/년"
    
  # 네트워크 비용 절감
  network_optimization:
    - "데이터 전송량 최적화"
    - "캐시 활용률 향상"
    - "압축 전송 적용"
    estimated_savings: "$200/년"
```

### 9.2 비용 모니터링

#### 9.2.1 비용 추적 메트릭
```yaml
cost_tracking:
  daily_monitoring:
    - "데이터베이스 컴퓨팅 비용"
    - "스토리지 사용량 및 비용"
    - "백업 스토리지 비용"
    - "네트워크 데이터 전송 비용"
    
  monthly_analysis:
    - "비용 추세 분석"
    - "예산 대비 실제 비용"
    - "비용 효율성 지표"
    - "최적화 기회 식별"
    
  cost_alerts:
    - threshold: "$1,600/월"
      action: "예산 초과 알림"
    - threshold: "20% 증가"
      action: "비정상 증가 알림"
```

## 10. 검증 및 테스트

### 10.1 설치 검증

#### 10.1.1 기능 검증 테스트
```sql
-- 1. 연결 테스트
\conninfo

-- 2. 스키마 존재 확인
SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'product_change';

-- 3. 테이블 생성 확인
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'product_change' 
ORDER BY table_name;

-- 4. 인덱스 생성 확인
SELECT indexname, tablename FROM pg_indexes 
WHERE schemaname = 'product_change' 
ORDER BY tablename, indexname;

-- 5. 기본 데이터 확인
SELECT service_name, state FROM product_change.pc_circuit_breaker_state;
```

#### 10.1.2 성능 검증 테스트
```sql
-- 1. 샘플 데이터 삽입
INSERT INTO product_change.pc_product_change_history 
(line_number, customer_id, current_product_code, target_product_code, process_status)
VALUES 
('010-1234-5678', 'CUST001', 'PLAN_A', 'PLAN_B', 'REQUESTED'),
('010-2345-6789', 'CUST002', 'PLAN_B', 'PLAN_C', 'COMPLETED'),
('010-3456-7890', 'CUST003', 'PLAN_C', 'PLAN_A', 'FAILED');

-- 2. 인덱스 사용 확인
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM product_change.pc_product_change_history 
WHERE line_number = '010-1234-5678' 
  AND process_status = 'REQUESTED';

-- 3. 성능 측정
SELECT 
    query,
    calls,
    mean_exec_time,
    total_exec_time
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 5;
```

### 10.2 고가용성 테스트

#### 10.2.1 장애조치 테스트
```bash
# 1. 현재 Primary 서버 상태 확인
az postgres flexible-server show \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-prod \
  --query "{name:name,state:state,haState:highAvailability.state}"

# 2. 강제 장애조치 테스트 (계획된 유지보수 시)
az postgres flexible-server restart \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-prod \
  --restart-ha-server

# 3. 장애조치 후 상태 확인
az postgres flexible-server show \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-prod \
  --query "{name:name,state:state,haState:highAvailability.state}"
```

#### 10.2.2 복구 테스트
```bash
# 1. Point-in-Time Recovery 테스트
az postgres flexible-server restore \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-test \
  --source-server phonebill-postgresql-prod \
  --restore-time "2025-09-08T10:00:00Z"

# 2. 복구된 서버 검증
psql -h phonebill-postgresql-test.postgres.database.azure.com \
     -U dbadmin \
     -d product_change_db \
     -c "SELECT COUNT(*) FROM product_change.pc_product_change_history;"

# 3. 테스트 서버 정리
az postgres flexible-server delete \
  --resource-group rg-phonebill-prod \
  --name phonebill-postgresql-test \
  --yes
```

## 11. 프로젝트 일정

### 11.1 설치 일정

| 단계 | 작업 내용 | 소요 시간 | 담당자 | 의존성 |
|------|----------|-----------|---------|--------|
| **Phase 1** | Azure 리소스 생성 | 2시간 | 데옵스 | - |
| **Phase 2** | 네트워크 구성 | 1시간 | 데옵스 | Phase 1 |
| **Phase 3** | PostgreSQL 서버 생성 | 2시간 | 데옵스 | Phase 2 |
| **Phase 4** | 스키마 및 초기 데이터 생성 | 1시간 | 백엔더 | Phase 3 |
| **Phase 5** | 보안 구성 | 2시간 | 데옵스 | Phase 4 |
| **Phase 6** | 모니터링 설정 | 2시간 | 데옵스 | Phase 5 |
| **Phase 7** | 테스트 및 검증 | 4시간 | 백엔더, QA매니저 | Phase 6 |
| **Phase 8** | 문서화 및 인수인계 | 2시간 | 데옵스 | Phase 7 |
| **총 소요 시간** | | **16시간** | | |

### 11.2 주요 이정표

```yaml
milestones:
  M1_Infrastructure_Ready:
    date: "설치 시작일"
    deliverable: "Azure 리소스 및 네트워크 구성 완료"
    
  M2_Database_Deployed:
    date: "설치 시작일 + 1일"
    deliverable: "PostgreSQL 서버 및 스키마 배포 완료"
    
  M3_Security_Configured:
    date: "설치 시작일 + 2일"
    deliverable: "보안 설정 및 모니터링 구성 완료"
    
  M4_Testing_Complete:
    date: "설치 시작일 + 3일"
    deliverable: "기능/성능/가용성 테스트 완료"
    
  M5_Production_Ready:
    date: "설치 시작일 + 4일"
    deliverable: "운영환경 준비 완료"
```

## 12. 위험 관리

### 12.1 위험 요소 분석

| 위험 요소 | 발생 가능성 | 영향도 | 위험 수준 | 대응 방안 |
|----------|------------|--------|----------|-----------|
| **Azure 서비스 장애** | 낮음 | 높음 | 중간 | 다중 리전 구성, SLA 모니터링 |
| **네트워크 연결 오류** | 중간 | 중간 | 중간 | Private Endpoint, 네트워크 이중화 |
| **데이터 손실** | 낮음 | 높음 | 중간 | 자동 백업, 지리적 복제 |
| **성능 저하** | 중간 | 중간 | 중간 | 모니터링 강화, 자동 스케일링 |
| **보안 침해** | 낮음 | 높음 | 중간 | 다층 보안, 정기 감사 |

### 12.2 비상 계획

#### 12.2.1 데이터 센터 장애
```yaml
datacenter_failure:
  scenario: "Korea Central 리전 전체 장애"
  impact: "서비스 중단 30분"
  response_plan:
    - "Korea South 읽기 복제본을 마스터로 승격"
    - "애플리케이션 연결 문자열 업데이트"
    - "DNS 레코드 변경"
    - "서비스 상태 모니터링"
  recovery_time: "30분"
```

#### 12.2.2 데이터 손상
```yaml
data_corruption:
  scenario: "애플리케이션 버그로 인한 데이터 손상"
  impact: "일부 데이터 불일치"
  response_plan:
    - "영향받은 데이터 범위 확인"
    - "Point-in-Time Recovery 실행"
    - "데이터 무결성 검증"
    - "애플리케이션 버그 수정"
  recovery_time: "4시간"
```

## 13. 승인 및 검토

### 13.1 검토 사항

- [ ] **아키텍처 검토**: 고가용성 및 성능 요구사항 충족
- [ ] **보안 검토**: 엔터프라이즈급 보안 정책 적용
- [ ] **비용 검토**: 예산 범위 내 운영비용 산정
- [ ] **운영 절차**: 일상 운영 및 장애 대응 절차 완비
- [ ] **재해복구**: RTO/RPO 목표 달성 가능 여부

### 13.2 승인자

| 역할 | 이름 | 승인 사항 | 서명 | 일자 |
|------|------|-----------|------|------|
| **프로젝트 매니저** | 김기획 | 전체 계획 승인 |  |  |
| **기술 아키텍트** | 이개발 | 기술 사양 승인 |  |  |
| **보안 관리자** | 정테스트 | 보안 정책 승인 |  |  |
| **인프라 관리자** | 최운영 | 인프라 구성 승인 |  |  |

---

## 부록

### A. 참조 문서
- [물리아키텍처 설계서 (운영환경)](../../../design/backend/physical/physical-architecture-prod.md)
- [Product-Change 서비스 데이터 설계서](../../../design/backend/database/product-change.md)
- [Product-Change 서비스 스키마](../../../design/backend/database/product-change-schema.psql)
- [백킹서비스 설치방법 가이드](../../../claude/backing-service-method.md)

### B. 연락처
- **운영팀**: ops-team@phonebill.com
- **DBA팀**: dba-team@phonebill.com  
- **개발팀**: dev-team@phonebill.com
- **보안팀**: security-team@phonebill.com

### C. 응급상황 연락처
- **24시간 운영센터**: +82-2-1234-5678
- **DBA 긴급전화**: +82-10-1234-5678
- **인프라 관리자**: +82-10-2345-6789

---

**최운영/데옵스**: Product-Change 서비스용 운영환경 데이터베이스 설치 계획서를 완성했습니다. Azure Database for PostgreSQL Flexible Server의 Zone Redundant HA를 활용한 고가용성 구성과 엔터프라이즈급 보안, 그리고 체계적인 모니터링 및 재해복구 방안을 포함하여 99.9% 가용성 목표 달성이 가능하도록 설계했습니다.