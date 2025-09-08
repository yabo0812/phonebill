# Redis 캐시 설치 계획서 - 운영환경

## 1. 개요

### 1.1 설치 목적
- 통신요금 관리 서비스의 **운영환경**용 Redis 캐시 구축
- Azure Cache for Redis Premium을 활용한 고가용성 캐시 서비스 제공
- 모든 마이크로서비스 간 공유 구성으로 데이터 일관성 및 성능 최적화
- 99.9% 가용성과 엔터프라이즈급 보안 수준 달성

### 1.2 설계 원칙
- **고가용성**: Zone Redundancy를 통한 Multi-Zone 배포
- **보안 우선**: Private Endpoint와 VNet 통합을 통한 격리된 네트워크
- **성능 최적화**: Premium 계층으로 고성능 및 데이터 지속성 보장
- **확장성**: 클러스터링을 통한 수평 확장 지원
- **모니터링**: 포괄적인 메트릭 수집 및 알림 체계

### 1.3 참조 문서
- 운영환경 물리아키텍처: design/backend/physical/physical-architecture-prod.md
- 데이터 설계 종합: design/backend/database/data-design-summary.md
- 백킹서비스설치방법: claude/backing-service-method.md

## 2. 시스템 환경

### 2.1 운영환경 사양
- **환경**: Microsoft Azure (운영환경)
- **위치**: Korea Central (주 리전), Korea South (재해복구 리전)
- **네트워크**: Azure Virtual Network (VNet) 통합
- **서비스 계층**: Azure Cache for Redis Premium
- **가용성**: 99.99% (Zone Redundancy 적용)
- **동시 사용자**: Peak 1,000명 지원

### 2.2 네트워크 구성
- **VNet**: phonebill-prod-vnet (10.0.0.0/16)
- **Cache Subnet**: 10.0.3.0/24 (Redis 전용)
- **Private Endpoint**: VNet 내부 접근만 허용
- **DNS Zone**: privatelink.redis.cache.windows.net

## 3. Azure Cache for Redis Premium 구성

### 3.1 기본 설정

| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| 서비스 명 | phonebill-cache-prod | 운영환경 Redis 인스턴스 |
| 계층 | Premium P2 | 6GB 메모리, 고성능 |
| 위치 | Korea Central | 주 리전 |
| 클러스터링 | 활성화 | 확장성 및 가용성 |
| 복제 | 활성화 | 데이터 안전성 |

### 3.2 고가용성 구성

#### 3.2.1 Zone Redundancy 설정
```yaml
zone_redundancy_config:
  enabled: true
  primary_zone: Korea Central Zone 1
  secondary_zone: Korea Central Zone 2
  tertiary_zone: Korea Central Zone 3
  automatic_failover: true
  failover_time: "<30초"
```

#### 3.2.2 클러스터 구성
```yaml
cluster_configuration:
  shard_count: 3  # 데이터 분산
  replicas_per_shard: 1  # 샤드별 복제본
  total_nodes: 6  # 3개 샤드 × 2개 노드(마스터+복제본)
  
  shard_distribution:
    shard_0: 
      master: "phonebill-cache-prod-000001.cache.windows.net:6380"
      replica: "phonebill-cache-prod-000002.cache.windows.net:6380"
    shard_1:
      master: "phonebill-cache-prod-000003.cache.windows.net:6380"
      replica: "phonebill-cache-prod-000004.cache.windows.net:6380"
    shard_2:
      master: "phonebill-cache-prod-000005.cache.windows.net:6380"
      replica: "phonebill-cache-prod-000006.cache.windows.net:6380"
```

## 4. 네트워크 보안 설정

### 4.1 Virtual Network 통합
```yaml
vnet_integration:
  resource_group: "phonebill-prod-rg"
  vnet_name: "phonebill-prod-vnet"
  subnet_name: "cache-subnet"
  subnet_address_prefix: "10.0.3.0/24"
  
  private_endpoint:
    name: "phonebill-cache-pe"
    subnet_id: "/subscriptions/{subscription}/resourceGroups/phonebill-prod-rg/providers/Microsoft.Network/virtualNetworks/phonebill-prod-vnet/subnets/cache-subnet"
    connection_name: "phonebill-cache-connection"
```

### 4.2 방화벽 규칙
```yaml
firewall_rules:
  # AKS 노드에서만 접근 허용
  - name: "Allow-AKS-Nodes"
    start_ip: "10.0.1.0"
    end_ip: "10.0.1.255"
    description: "AKS Application Subnet 접근 허용"
    
  # 관리용 Bastion 호스트 접근
  - name: "Allow-Bastion"
    start_ip: "10.0.4.100"
    end_ip: "10.0.4.110"
    description: "운영 관리용 Bastion 호스트"
    
  # 외부 접근 차단 (기본값)
  public_network_access: "Disabled"
```

### 4.3 보안 인증 설정
```yaml
security_configuration:
  # Redis AUTH 활성화
  auth_enabled: true
  require_ssl: true
  minimum_tls_version: "1.2"
  
  # 액세스 키 관리
  access_keys:
    primary_key_regeneration: "매월 1일"
    secondary_key_regeneration: "매월 15일"
    key_vault_integration: true
    
  # Azure AD 통합 (Preview 기능)
  azure_ad_authentication:
    enabled: false  # 운영 안정성을 위해 비활성화
    fallback_to_access_key: true
```

## 5. 캐시 전략 및 키 관리

### 5.1 캐시 키 전략

#### 5.1.1 네이밍 규칙
```yaml
cache_key_patterns:
  # 서비스별 네임스페이스 분리
  auth_service:
    user_session: "auth:session:{userId}:{sessionId}"
    user_permissions: "auth:permissions:{userId}"
    login_attempts: "auth:attempts:{userId}"
    
  bill_inquiry_service:
    customer_info: "bill:customer:{lineNumber}"
    bill_cache: "bill:inquiry:{customerId}:{month}"
    kos_response: "bill:kos:{requestId}"
    
  product_change_service:
    product_info: "product:info:{productCode}"
    available_products: "product:available:{customerId}"
    change_history: "product:history:{customerId}:{requestId}"
    
  common:
    system_config: "system:config:{configKey}"
    circuit_breaker: "system:cb:{serviceName}"
```

### 5.2 TTL 정책

#### 5.2.1 서비스별 TTL 설정
```yaml
ttl_policies:
  # 고객정보 - 4시간 (자주 조회되지만 변경 가능성 있음)
  customer_info: 14400  # 4시간
  
  # 상품정보 - 2시간 (정기적 업데이트)
  product_info: 7200    # 2시간
  
  # 세션정보 - 24시간 (로그인 유지)
  session_info: 86400   # 24시간
  
  # 권한정보 - 8시간 (보안 중요도 높음)
  permissions: 28800    # 8시간
  
  # 가용 상품 목록 - 24시간 (일반적으로 일정)
  available_products: 86400  # 24시간
  
  # 회선 상태 - 30분 (실시간성 중요)
  line_status: 1800     # 30분
  
  # KOS 응답 캐시 - 1시간 (외부 API 부하 감소)
  kos_response: 3600    # 1시간
  
  # 시스템 설정 - 1일 (거의 변경되지 않음)
  system_config: 86400  # 24시간
  
  # Circuit Breaker 상태 - 5분 (빠른 복구 필요)
  circuit_breaker: 300  # 5분
```

### 5.3 메모리 관리 정책
```yaml
memory_management:
  # 메모리 정책 설정
  maxmemory_policy: "allkeys-lru"
  
  # 메모리 사용량 임계값
  memory_thresholds:
    warning: "80%"  # 경고 알림
    critical: "90%" # 긴급 알림
    
  # 메모리 샘플링 설정
  maxmemory_samples: 5
  
  # 키 만료 정책
  expire_policy:
    active_expire_frequency: 10  # 초당 10회 만료 키 검사
    lazy_expire_on_access: true  # 접근 시 만료 검사
```

## 6. 데이터 지속성 설정

### 6.1 RDB 백업 구성
```yaml
rdb_backup_configuration:
  # 스냅샷 백업 설정
  save_policy:
    - "900 1"    # 15분 이내 1개 이상 키 변경 시 저장
    - "300 10"   # 5분 이내 10개 이상 키 변경 시 저장
    - "60 10000" # 1분 이내 10000개 이상 키 변경 시 저장
    
  # 백업 파일 관리
  backup_retention:
    daily_backups: 7   # 7일간 보관
    weekly_backups: 4  # 4주간 보관
    monthly_backups: 12 # 12개월간 보관
    
  # 백업 스토리지
  backup_storage:
    account: "phonebillprodbackup"
    container: "redis-backups"
    encryption: true
```

### 6.2 AOF (Append Only File) 설정
```yaml
aof_configuration:
  # AOF 활성화
  appendonly: true
  
  # 동기화 정책
  appendfsync: "everysec"  # 매초마다 디스크에 동기화
  
  # AOF 리라이트 설정
  auto_aof_rewrite_percentage: 100
  auto_aof_rewrite_min_size: "64mb"
  
  # AOF 로딩 설정
  aof_load_truncated: true
```

## 7. 모니터링 및 알림 설정

### 7.1 Azure Monitor 통합
```yaml
monitoring_configuration:
  # Azure Monitor 연동
  diagnostic_settings:
    log_analytics_workspace: "law-phonebill-prod"
    metrics_retention_days: 90
    logs_retention_days: 30
    
  # 수집할 메트릭
  metrics:
    - "CacheMisses"
    - "CacheHits"
    - "GetCommands"
    - "SetCommands"
    - "ConnectedClients"
    - "UsedMemory"
    - "UsedMemoryPercentage"
    - "TotalCommandsProcessed"
    - "CacheLatency"
    - "Errors"
```

### 7.2 알림 규칙
```yaml
alert_rules:
  # 성능 관련 알림
  performance_alerts:
    - name: "High Cache Miss Rate"
      metric: "CacheMissPercentage"
      threshold: 30  # 30% 이상
      window: "5분"
      severity: "Warning"
      action: "Teams 알림"
      
    - name: "High Memory Usage"
      metric: "UsedMemoryPercentage"
      threshold: 85  # 85% 이상
      window: "5분"
      severity: "Critical"
      action: "Teams + SMS 알림"
      
    - name: "High Response Time"
      metric: "CacheLatency"
      threshold: 10  # 10ms 이상
      window: "5분"
      severity: "Warning"
      action: "Teams 알림"
      
  # 가용성 관련 알림
  availability_alerts:
    - name: "Cache Connection Failed"
      metric: "Errors"
      threshold: 5   # 5개 이상 에러
      window: "1분"
      severity: "Critical"
      action: "즉시 전화 + Teams 알림"
      
    - name: "Too Many Connected Clients"
      metric: "ConnectedClients"
      threshold: 500  # 500개 이상 연결
      window: "5분"
      severity: "Warning"
      action: "Teams 알림"
```

### 7.3 대시보드 구성
```yaml
dashboard_configuration:
  # Azure Portal 대시보드
  azure_dashboard:
    - "Cache Hit/Miss 비율 차트"
    - "메모리 사용량 추이"
    - "연결된 클라이언트 수"
    - "응답 시간 분포"
    - "에러 발생률"
    
  # Grafana 대시보드 (옵션)
  grafana_dashboard:
    datasource: "Azure Monitor"
    panels:
      - "실시간 메트릭 패널"
      - "성능 추이 그래프"
      - "알림 상태 표시"
```

## 8. 연결 설정 및 클라이언트 구성

### 8.1 연결 문자열
```yaml
connection_configuration:
  # 클러스터 연결 (운영환경)
  cluster_connection_string: |
    phonebill-cache-prod.redis.cache.windows.net:6380,
    password={access_key},ssl=True,abortConnect=False,
    connectTimeout=5000,syncTimeout=5000
    
  # 클라이언트 라이브러리별 설정
  client_configurations:
    spring_boot:
      redis_host: "phonebill-cache-prod.redis.cache.windows.net"
      redis_port: 6380
      redis_ssl: true
      redis_timeout: 5000
      redis_pool_size: 20
      redis_cluster_enabled: true
      
    connection_pool:
      max_total: 20
      max_idle: 10
      min_idle: 5
      test_on_borrow: true
      test_while_idle: true
```

### 8.2 Spring Boot 연동 설정
```yaml
spring_redis_configuration:
  # application-prod.yml 설정
  spring:
    redis:
      cluster:
        nodes:
          - "phonebill-cache-prod.redis.cache.windows.net:6380"
      ssl: true
      password: "${REDIS_PASSWORD}"
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 5000ms
        cluster:
          refresh:
            adaptive: true
            period: 30s
```

## 9. 재해복구 및 백업 전략

### 9.1 지역 간 복제 설정
```yaml
geo_replication:
  # 주 리전 (Korea Central)
  primary_region:
    cache_name: "phonebill-cache-prod"
    resource_group: "phonebill-prod-rg"
    
  # 재해복구 리전 (Korea South)
  secondary_region:
    cache_name: "phonebill-cache-prod-dr"
    resource_group: "phonebill-prod-dr-rg"
    
  # 복제 설정
  replication_configuration:
    link_name: "phonebill-cache-geo-link"
    replication_role: "Primary"
    linked_cache_name: "phonebill-cache-prod-dr"
```

### 9.2 백업 및 복구 절차
```yaml
backup_recovery:
  # 백업 전략
  backup_strategy:
    automated_backup: true
    backup_frequency: "매시간"
    backup_retention: "7일"
    
    manual_backup:
      before_maintenance: true
      before_major_release: true
      
  # 복구 목표
  recovery_objectives:
    rto: "15분"  # Recovery Time Objective
    rpo: "5분"   # Recovery Point Objective
    
  # 복구 절차
  recovery_procedures:
    automated_failover: true
    manual_failover_approval: false  # 운영환경에서는 자동 처리
    health_check_interval: "30초"
```

## 10. 보안 강화 설정

### 10.1 Azure Key Vault 통합
```yaml
key_vault_integration:
  vault_name: "phonebill-prod-kv"
  
  # 저장할 시크릿
  secrets:
    - name: "redis-primary-key"
      description: "Redis 기본 액세스 키"
      rotation_period: "30일"
      
    - name: "redis-secondary-key"
      description: "Redis 보조 액세스 키"
      rotation_period: "30일"
      
    - name: "redis-connection-string"
      description: "Redis 연결 문자열"
      auto_update: true
```

### 10.2 네트워크 보안 정책
```yaml
network_security:
  # Private Endpoint 보안
  private_endpoint_security:
    network_access_policy: "Private endpoints only"
    public_network_access: "Disabled"
    
  # Network Security Group 규칙
  nsg_rules:
    - name: "Allow-Redis-From-AKS"
      priority: 100
      direction: "Inbound"
      access: "Allow"
      protocol: "TCP"
      source_port_ranges: "*"
      destination_port_ranges: "6379-6380"
      source_address_prefix: "10.0.1.0/24"
      
    - name: "Deny-All-Other"
      priority: 1000
      direction: "Inbound"
      access: "Deny"
      protocol: "*"
      source_port_ranges: "*"
      destination_port_ranges: "*"
      source_address_prefix: "*"
```

## 11. 성능 최적화 설정

### 11.1 클러스터 최적화
```yaml
cluster_optimization:
  # 클러스터 설정 최적화
  cluster_configuration:
    cluster_enabled: true
    cluster_config_file: "nodes.conf"
    cluster_node_timeout: 15000
    cluster_slave_validity_factor: 10
    
  # 메모리 최적화
  memory_optimization:
    maxmemory: "5gb"  # P2 계층 6GB 중 5GB 사용
    maxmemory_policy: "allkeys-lru"
    maxmemory_samples: 5
    
  # 네트워크 최적화
  network_optimization:
    tcp_keepalive: 300
    timeout: 0
    tcp_backlog: 511
```

### 11.2 클라이언트 최적화
```yaml
client_optimization:
  # 연결 풀 최적화
  connection_pool:
    max_total: 20
    max_idle: 10
    min_idle: 5
    max_wait_millis: 5000
    test_on_borrow: true
    test_on_return: false
    test_while_idle: true
    
  # 파이프라이닝 설정
  pipelining:
    enabled: true
    batch_size: 100
    timeout: 1000
```

## 12. 설치 실행 계획

### 12.1 설치 단계
```yaml
installation_phases:
  phase_1_preparation:
    duration: "1일"
    tasks:
      - "Azure 리소스 그룹 준비"
      - "Virtual Network 구성 확인"
      - "서브넷 및 NSG 설정"
      - "Key Vault 시크릿 준비"
      
  phase_2_deployment:
    duration: "2일"
    tasks:
      - "Azure Cache for Redis 생성"
      - "클러스터링 구성"
      - "Private Endpoint 설정"
      - "방화벽 규칙 적용"
      
  phase_3_configuration:
    duration: "1일"
    tasks:
      - "백업 설정 구성"
      - "모니터링 설정"
      - "알림 규칙 생성"
      - "대시보드 구성"
      
  phase_4_testing:
    duration: "2일"
    tasks:
      - "연결 테스트"
      - "성능 테스트"
      - "장애조치 테스트"
      - "보안 검증"
```

### 12.2 사전 준비사항
```yaml
prerequisites:
  azure_resources:
    - "Azure 구독 및 권한 확인"
    - "Resource Group 생성: phonebill-prod-rg"
    - "Virtual Network: phonebill-prod-vnet"
    - "Key Vault: phonebill-prod-kv"
    
  network_configuration:
    - "Cache Subnet (10.0.3.0/24) 생성"
    - "Network Security Group 규칙 준비"
    - "Private DNS Zone 설정"
    
  security_preparation:
    - "Service Principal 생성 및 권한 부여"
    - "SSL 인증서 준비"
    - "액세스 키 생성 정책 수립"
```

### 12.3 검증 체크리스트
```yaml
validation_checklist:
  connectivity_tests:
    - [ ] "AKS 클러스터에서 Redis 연결 테스트"
    - [ ] "각 서비스에서 캐시 읽기/쓰기 테스트"
    - [ ] "클러스터 모드 연결 확인"
    - [ ] "SSL/TLS 암호화 통신 확인"
    
  performance_tests:
    - [ ] "응답 시간 < 5ms 확인"
    - [ ] "초당 10,000 요청 처리 확인"
    - [ ] "메모리 사용량 최적화 확인"
    - [ ] "캐시 히트율 > 90% 달성"
    
  security_tests:
    - [ ] "외부 접근 차단 확인"
    - [ ] "인증 및 권한 확인"
    - [ ] "데이터 암호화 확인"
    - [ ] "감사 로그 기록 확인"
    
  availability_tests:
    - [ ] "Zone 장애 시뮬레이션"
    - [ ] "노드 장애 복구 테스트"
    - [ ] "자동 장애조치 확인"
    - [ ] "백업 및 복원 테스트"
```

## 13. 운영 및 유지보수

### 13.1 일상 운영 절차
```yaml
daily_operations:
  monitoring_checks:
    - [ ] "Redis 클러스터 상태 확인"
    - [ ] "메모리 사용률 점검"
    - [ ] "캐시 히트율 확인"
    - [ ] "에러 로그 검토"
    
  weekly_operations:
    - [ ] "성능 메트릭 리포트 생성"
    - [ ] "백업 상태 확인"
    - [ ] "보안 패치 적용 검토"
    - [ ] "용량 계획 검토"
```

### 13.2 성능 튜닝 가이드
```yaml
performance_tuning:
  memory_optimization:
    - "키 만료 정책 최적화"
    - "메모리 사용 패턴 분석"
    - "불필요한 키 정리"
    
  network_optimization:
    - "연결 풀 크기 조정"
    - "타임아웃 값 튜닝"
    - "파이프라이닝 활용"
    
  application_optimization:
    - "캐시 키 설계 개선"
    - "TTL 값 최적화"
    - "배치 처리 활용"
```

## 14. 비용 최적화

### 14.1 예상 비용 (월간, USD)
```yaml
monthly_cost_estimation:
  azure_cache_redis:
    tier: "Premium P2"
    capacity: "6GB"
    estimated_cost: "$350"
    
  network_costs:
    private_endpoint: "$15"
    data_transfer: "$20"
    
  backup_storage:
    storage_account: "$10"
    
  total_monthly_cost: "$395"
```

### 14.2 비용 최적화 전략
```yaml
cost_optimization:
  rightsizing:
    - "실제 메모리 사용량 기반 계층 조정"
    - "사용량 패턴 분석 후 스케일링"
    
  efficiency_improvements:
    - "TTL 최적화로 불필요한 데이터 정리"
    - "압축 알고리즘 활용"
    - "캐시 히트율 향상"
    
  reserved_capacity:
    - "1년 예약 인스턴스 (20% 할인)"
    - "3년 예약 인스턴스 (40% 할인)"
```

## 15. 설치 완료 확인

### 15.1 설치 성공 기준
- ✅ Azure Cache for Redis Premium 정상 생성
- ✅ Zone Redundancy 및 클러스터링 활성화
- ✅ Private Endpoint를 통한 VNet 통합 완료
- ✅ 모든 서비스에서 캐시 연결 성공
- ✅ 모니터링 및 알림 체계 구축
- ✅ 백업 및 재해복구 설정 완료

### 15.2 성과 목표 달성 확인
- 🎯 **가용성**: 99.99% 이상 (Zone Redundancy)
- 🎯 **성능**: 응답시간 < 5ms, 초당 10,000+ 요청 처리
- 🎯 **보안**: Private Endpoint, 암호화 통신 적용
- 🎯 **확장성**: 클러스터링을 통한 수평 확장 준비
- 🎯 **모니터링**: 실시간 메트릭 수집 및 알림 체계

---

**계획서 작성일**: `2025-09-08`  
**작성자**: 데옵스 (최운영)  
**검토자**: 백엔더 (이개발), 아키텍트 (김기획)  
**승인자**: 기획자 (김기획)

이 Redis 캐시 설치 계획서는 **통신요금 관리 서비스의 운영환경**에 최적화되어 있으며, **Azure Cache for Redis Premium**을 활용한 고가용성 및 고성능 캐시 서비스를 제공합니다.