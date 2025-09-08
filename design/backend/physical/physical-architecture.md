# 물리 아키텍처 설계서 - 마스터 인덱스

## 1. 개요

### 1.1 설계 목적
- 통신요금 관리 서비스의 Azure Cloud 기반 통합 물리 아키텍처 설계 및 관리
- 개발환경과 운영환경의 체계적인 아키텍처 분리 및 단계적 진화 전략
- 환경별 특화 구성과 비용 효율적인 확장 로드맵 제시
- 전체 시스템의 거버넌스 체계 및 운영 가이드라인 정의

### 1.2 아키텍처 분리 원칙
- **환경별 특화**: 개발환경(MVP/비용 우선)과 운영환경(가용성/확장성 우선)의 목적에 맞는 최적화
- **단계적 발전**: 개발→운영 단계적 아키텍처 진화 및 기술적 성숙도 향상
- **비용 효율성**: 환경별 리소스 최적화를 통한 전체 TCO 최소화
- **운영 일관성**: 환경별 차이를 최소화한 일관된 배포 및 운영 절차

### 1.3 문서 구조
```
physical-architecture.md (마스터 인덱스)
├── physical-architecture-dev.md (개발환경)
└── physical-architecture-prod.md (운영환경)
```

### 1.4 참조 아키텍처
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 논리아키텍처: design/backend/logical/logical-architecture.md
- 아키텍처패턴: design/pattern/architecture-pattern.md
- API설계서: design/backend/api/*.yaml

## 2. 환경별 아키텍처 개요

### 2.1 환경별 특성 비교

| 구분 | 개발환경 | 운영환경 |
|------|----------|----------|
| **목적** | MVP 개발/검증 | 실제 서비스 운영 |
| **가용성** | 95% (월 36시간 다운타임) | 99.9% (월 43분 다운타임) |
| **사용자** | 개발팀 (5명) | 실사용자 (Peak 1,000명) |
| **확장성** | 고정 리소스 | 자동 스케일링 (10배 확장) |
| **보안** | 기본 보안 | 엔터프라이즈급 다층 보안 |
| **비용** | 최소화 ($171/월) | 최적화 ($2,450/월) |
| **복잡도** | 단순 (운영 편의성) | 고도화 (안정성/성능) |

### 2.2 환경별 세부 문서

#### 2.2.1 개발환경 아키텍처
📄 **[물리 아키텍처 설계서 - 개발환경](./physical-architecture-dev.md)**

**주요 특징:**
- **비용 최적화**: Spot Instance, Pod 기반 백킹서비스 활용
- **개발 편의성**: 복잡한 설정 최소화, 빠른 배포
- **단순한 보안**: 기본 Network Policy, JWT 검증
- **Pod 기반 구성**: PostgreSQL/Redis Pod 배포

**핵심 구성:**
📄 **[개발환경 물리 아키텍처 다이어그램](./physical-architecture-dev.mmd)**
- NGINX Ingress → AKS Basic → Pod Services 구조
- Application Pods, PostgreSQL Pod, Redis Pod 배치

#### 2.2.2 운영환경 아키텍처  
📄 **[물리 아키텍처 설계서 - 운영환경](./physical-architecture-prod.md)**

**주요 특징:**
- **고가용성**: Multi-Zone 배포, 자동 장애조치
- **확장성**: HPA 기반 자동 스케일링 (10배 확장)
- **엔터프라이즈 보안**: 다층 보안, Private Endpoint
- **관리형 서비스**: Azure Database, Cache for Redis

**핵심 구성:**
📄 **[운영환경 물리 아키텍처 다이어그램](./physical-architecture-prod.mmd)**
- Azure Front Door → App Gateway + WAF → AKS Premium 구조
- Multi-Zone Apps, Azure PostgreSQL, Azure Redis Premium 배치

### 2.3 핵심 아키텍처 결정사항

#### 2.3.1 공통 아키텍처 원칙
- **서비스 메시 제거**: Istio 대신 Kubernetes Network Policies 사용 (복잡도 최소화)
- **선택적 비동기**: 이력 처리만 비동기, 핵심 비즈니스 로직은 동기 통신
- **Managed Identity**: 키 없는 인증으로 보안 강화 및 운영 단순화
- **다층 보안**: L1(Network) → L2(Gateway) → L3(Identity) → L4(Data)

#### 2.3.2 환경별 차별화 전략

**개발환경 최적화:**
- 개발 속도와 비용 효율성 우선
- 단순한 구성으로 운영 부담 최소화
- Pod 기반 백킹서비스로 외부 의존성 제거

**운영환경 최적화:**
- 가용성과 확장성 우선
- Azure 관리형 서비스로 운영 안정성 확보
- 엔터프라이즈급 보안 및 종합적 모니터링

## 3. 네트워크 아키텍처 비교

### 3.1 환경별 네트워크 전략

#### 3.1.1 환경별 네트워크 전략 비교

| 구성 요소 | 개발환경 | 운영환경 | 비교 |
|-----------|----------|----------|------|
| **인그레스** | NGINX Ingress Controller | Azure Application Gateway + WAF | 운영환경에서 WAF 보안 강화 |
| **네트워크** | 단일 VNet 구성 | 다중 서브넷 (App/DB/Cache) | 운영환경에서 계층적 네트워크 분리 |
| **보안** | 기본 Network Policy | Private Endpoint, NSG 강화 | 운영환경에서 엔터프라이즈급 보안 |
| **접근** | 인터넷 직접 접근 허용 | Private Link 기반 보안 접근 | 운영환경에서 보안 접근 제한 |

### 3.2 네트워크 보안 전략

#### 3.2.1 공통 보안 원칙
- **Network Policies**: Pod 간 통신 제어 및 마이크로 세그먼테이션
- **Managed Identity**: 키 없는 인증으로 Azure 서비스 안전 접근
- **Private Endpoints**: Azure 서비스 보안 연결
- **TLS 암호화**: 모든 외부 통신 암호화

#### 3.2.2 환경별 보안 수준

| 보안 요소 | 개발환경 | 운영환경 | 보안 수준 |
|-----------|----------|----------|----------|
| **Network Policy** | 기본 (개발 편의성 고려) | 엄격한 적용 | 운영환경에서 강화 |
| **시크릿 관리** | Kubernetes Secrets | Azure Key Vault | 운영환경에서 HSM 보안 |
| **암호화** | HTTPS 인그레스 레벨 | End-to-End TLS 1.3 | 운영환경에서 완전 암호화 |
| **웹 보안** | - | WAF + DDoS 보호 | 운영환경 전용 |

## 4. 데이터 아키텍처 비교

### 4.1 환경별 데이터 전략

#### 4.1.1 환경별 데이터 구성 비교

| 데이터 서비스 | 개발환경 | 운영환경 | 가용성 | 비용 |
|-------------|----------|----------|---------|------|
| **PostgreSQL** | Kubernetes Pod + Azure Disk | Azure Database Flexible Server | 95% vs 99.9% | $0 vs $450/월 |
| **Redis** | Memory Only Pod | Azure Cache Premium (Cluster) | 단일 vs 클러스터 | $0 vs $250/월 |
| **백업** | 수동 (주 1회) | 자동 (35일 보존) | 로컬 vs 지역간 복제 | - |
| **데이터 지속성** | 재시작 시 손실 가능 | Zone Redundant | - | - |

### 4.2 캐시 전략 비교

#### 4.2.1 다층 캐시 아키텍처
| 캐시 계층 | 캐시 타입 | TTL | 개발환경 설정 | 운영환경 설정 | 용도 |
|----------|----------|-----|-------------|-------------|------|
| **L1_Application** | Caffeine Cache | 5분 | max_entries: 1000 | max_entries: 2000 | 애플리케이션 레벨 로컬 캐시 |
| **L2_Distributed** | Redis | 30분 | cluster_mode: false | cluster_mode: true | 분산 캐시, eviction_policy: allkeys-lru |

#### 4.2.2 환경별 캐시 특성 비교

| 캐시 특성 | 개발환경 | 운영환경 | 비고 |
|-----------|----------|----------|------|
| **Redis 구성** | 단일 Pod | Premium 클러스터 | 운영환경에서 고가용성 |
| **데이터 지속성** | 메모리 전용 | 지속성 백업 | 운영환경에서 데이터 보장 |
| **성능** | 기본 성능 | 최적화된 성능 | 운영환경에서 향상된 처리 능력 |

## 5. 보안 아키텍처 비교

### 5.1 다층 보안 아키텍처

#### 5.1.1 공통 보안 계층
| 보안 계층 | 보안 기술 | 적용 범위 | 보안 목적 |
|----------|----------|----------|----------|
| **L1_Network** | Kubernetes Network Policies | Pod-to-Pod 통신 제어 | 내부 네트워크 마이크로 세그먼테이션 |
| **L2_Gateway** | API Gateway JWT 검증 | 외부 요청 인증/인가 | API 레벨 인증 및 인가 제어 |
| **L3_Identity** | Azure Managed Identity | Azure 서비스 접근 | 클라우드 리소스 안전한 접근 |
| **L4_Data** | Private Link + Key Vault | 데이터 암호화 및 비밀 관리 | 엔드투엔드 데이터 보호 |

### 5.2 환경별 보안 수준

#### 5.2.1 환경별 보안 수준 비교

| 보안 영역 | 개발환경 | 운영환경 | 보안 강화 |
|-----------|----------|----------|----------|
| **인증** | JWT (고정 시크릿) | Azure AD + Managed Identity | 운영환경에서 엔터프라이즈 인증 |
| **네트워크** | 기본 Network Policy | 엄격한 Network Policy + Private Endpoint | 운영환경에서 네트워크 격리 강화 |
| **시크릿** | Kubernetes Secrets | Azure Key Vault (HSM) | 운영환경에서 하드웨어 보안 모듈 |
| **암호화** | HTTPS (인그레스 레벨) | End-to-End TLS 1.3 | 운영환경에서 전 구간 암호화 |

## 6. 모니터링 및 운영

### 6.1 환경별 모니터링 전략

#### 6.1.1 환경별 모니터링 도구 비교

| 모니터링 요소 | 개발환경 | 운영환경 | 기능 차이 |
|-------------|----------|----------|----------|
| **도구** | Kubernetes Dashboard, kubectl logs | Azure Monitor, Application Insights | 운영환경에서 전문 APM 도구 |
| **메트릭** | 기본 Pod/Node 메트릭 | 포괄적 APM, 비즈니스 메트릭 | 운영환경에서 비즈니스 인사이트 |
| **알림** | 기본 알림 (Pod 재시작) | 다단계 알림 (Teams 연동) | 운영환경에서 전문 알림 체계 |
| **로그** | 로컬 파일시스템 (7일) | Log Analytics (90일) | 운영환경에서 장기 보존 |

### 6.2 CI/CD 및 배포 전략

#### 6.2.1 환경별 배포 방식 비교

| 배포 요소 | 개발환경 | 운영환경 | 안정성 차이 |
|-----------|----------|----------|----------|
| **배포 방식** | Rolling Update | Blue-Green Deployment | 운영환경에서 무중단 배포 |
| **자동화** | develop 브랜치 자동 | tag 생성 + 수동 승인 | 운영환경에서 더 신중한 배포 |
| **테스트** | 기본 헬스체크 | 종합 품질 게이트 (80% 커버리지) | 운영환경에서 더 엄격한 테스트 |
| **다운타임** | 허용 (1-2분) | Zero Downtime | 운영환경에서 서비스 연속성 보장 |

## 7. 비용 분석

### 7.1 환경별 비용 구조

#### 7.1.1 월간 비용 비교 (USD)

```yaml
cost_comparison:
  development:
    total_cost: "$171"
    components:
      aks_nodes: "$73 (Spot Instance)"
      azure_disk: "$5 (Standard 20GB)"
      load_balancer: "$18 (Basic)"
      service_bus: "$10 (Basic)"
      container_registry: "$5 (Basic)"
      networking: "$10 (Single Region)"
      others: "$50"
    optimization_strategies:
      - spot_instances: "70% 절약"
      - pod_based_services: "100% 절약"
      - minimal_configuration: "비용 최소화"
      
  production:
    total_cost: "$2,450"
    components:
      aks_nodes: "$1,200 (Reserved Instance)"
      postgresql: "$450 (Managed Service)"
      redis: "$250 (Premium Cluster)"
      application_gateway: "$150 (Standard_v2)"
      service_bus: "$100 (Premium)"
      load_balancer: "$50 (Standard)"
      storage: "$100 (Premium SSD)"
      networking: "$150 (Data Transfer)"
      monitoring: "$100 (Log Analytics)"
    optimization_strategies:
      - reserved_instances: "30% 절약"
      - auto_scaling: "동적 최적화"
      - performance_tuning: "효율성 개선"
```

#### 7.1.2 환경별 비용 최적화 전략 비교

| 최적화 영역 | 개발환경 | 운영환경 | 절약 효과 |
|-------------|----------|----------|----------|
| **컴퓨팅 비용** | Spot Instances 사용 | Reserved Instances | 70% vs 30% 절약 |
| **백킹서비스** | Pod 기반 (무료) | 관리형 서비스 | 100% 절약 vs 안정성 |
| **리소스 관리** | 비업무시간 자동 종료 | 자동 스케일링 | 시간 절약 vs 효율성 |
| **사이징 전략** | 고정 리소스 | 성능 기반 적정 sizing | 단순 vs 최적화 |

## 8. 전환 및 확장 계획

### 8.1 개발환경 → 운영환경 전환 체크리스트

```yaml
migration_checklist:
  data_migration:
    - task: "개발 데이터 백업"
      status: "☐"
      priority: "높음"
      method: "pg_dump 사용"
      
    - task: "스키마 마이그레이션 스크립트"
      status: "☐"
      priority: "높음"
      method: "Flyway/Liquibase 고려"
      
    - task: "Azure Database 프로비저닝"
      status: "☐"
      priority: "높음"
      method: "Flexible Server 구성"
      
  configuration_changes:
    - task: "환경 변수 분리"
      status: "☐"
      priority: "높음"
      method: "ConfigMap/Secret 분리"
      
    - task: "Azure Key Vault 설정"
      status: "☐"
      priority: "높음"
      method: "HSM 보안 모듈"
      
    - task: "Managed Identity 구성"
      status: "☐"
      priority: "높음"
      method: "키 없는 인증"
      
  monitoring_setup:
    - task: "Azure Monitor 설정"
      status: "☐"
      priority: "중간"
      method: "Log Analytics 연동"
      
    - task: "알림 정책 수립"
      status: "☐"
      priority: "중간"
      method: "Teams 연동"
      
    - task: "대시보드 구축"
      status: "☐"
      priority: "낮음"
      method: "Application Insights"
```

### 8.2 단계별 확장 로드맵

```yaml
expansion_roadmap:
  phase_1:
    duration: "현재-6개월"
    focus: "안정화"
    core_objectives:
      - "개발환경 → 운영환경 전환"
      - "기본 모니터링 및 알림 구축"
      - "99.9% 가용성 달성"
    key_deliverables:
      - "운영환경 배포 완료"
      - "CI/CD 파이프라인 구축"
      - "기본 보안 정책 적용"
    user_support: "1만 사용자"
    availability: "99.9%"
    
  phase_2:
    duration: "6-12개월"
    focus: "최적화"
    core_objectives:
      - "성능 최적화 및 비용 효율화"
      - "고급 모니터링 (APM) 도입"
      - "자동 스케일링 고도화"
    key_deliverables:
      - "캐시 전략 고도화"
      - "성능 튜닝 완료"
      - "비용 최적화 달성"
    user_support: "10만 동시 사용자"
    availability: "99.9%"
    
  phase_3:
    duration: "12-18개월"
    focus: "글로벌 확장"
    core_objectives:
      - "다중 리전 배포"
      - "글로벌 CDN 및 로드 밸런싱"
      - "지역별 데이터 센터 구축"
    key_deliverables:
      - "Multi-Region 아키텍처"
      - "글로벌 재해복구 체계"
      - "지역별 성능 최적화"
    user_support: "100만 사용자"
    availability: "99.95%"
```

## 9. 핵심 SLA 지표

### 9.1 환경별 서비스 수준 목표

```yaml
sla_comparison:
  metrics:
    availability:
      development: "95%"
      production: "99.9%"
      global_phase3: "99.95%"
      
    response_time:
      development: "< 10초"
      production: "< 3초"
      global_phase3: "< 2초"
      
    deployment_time:
      development: "30분"
      production: "10분"
      global_phase3: "5분"
      
    recovery_time:
      development: "수동 복구"
      production: "< 30분"
      global_phase3: "< 15분"
      
    concurrent_users:
      development: "개발팀 (5명)"
      production: "1,000명"
      global_phase3: "100,000명"
      
    monthly_cost:
      development: "$171"
      production: "$2,450"
      global_phase3: "$15,000+"
      
    security_incidents:
      development: "모니터링 없음"
      production: "0건 목표"
      global_phase3: "0건 목표"
```

이 마스터 물리 아키텍처 설계서는 **통신요금 관리 서비스**의 전체 아키텍처를 통합 관리하며, 개발환경에서 글로벌 서비스까지의 체계적인 진화 경로를 제시합니다. Azure 클라우드 기반으로 구축되어 비용 효율성과 확장성을 동시에 달성할 수 있도록 설계되었습니다.