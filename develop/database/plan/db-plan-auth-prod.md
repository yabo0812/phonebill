# Auth 서비스 데이터베이스 설치 계획서 - 운영환경

## 1. 계획 개요

### 1.1 설치 목적
- **서비스**: Auth 서비스 (사용자 인증/인가)
- **데이터베이스**: `phonebill_auth`
- **환경**: 운영환경 (Production)
- **플랫폼**: Azure Database for PostgreSQL Flexible Server

### 1.2 설치 범위
- Azure Database for PostgreSQL Flexible Server 인스턴스 생성
- Auth 서비스 전용 데이터베이스 및 스키마 구성
- 고가용성 및 보안 설정 구성
- 백업 및 모니터링 설정

### 1.3 참조 문서
- **물리아키텍처**: `design/backend/physical/physical-architecture-prod.md`
- **데이터설계서**: `design/backend/database/auth.md`
- **스키마파일**: `design/backend/database/auth-schema.psql`
- **백킹서비스가이드**: `claude/backing-service-method.md`

## 2. 인프라 요구사항

### 2.1 Azure Database for PostgreSQL Flexible Server 구성

#### 2.1.1 기본 설정
| 구성 항목 | 설정 값 | 비고 |
|----------|---------|------|
| **리소스 그룹** | rg-phonebill-prod | 운영환경 전용 |
| **서버 이름** | phonebill-auth-postgresql-prod | DNS: `{서버이름}.postgres.database.azure.com` |
| **지역** | Korea Central | 주 데이터센터 |
| **PostgreSQL 버전** | 15 | 최신 안정 버전 |
| **컴퓨팅 + 스토리지** | GeneralPurpose | 범용 워크로드 |

#### 2.1.2 컴퓨팅 리소스
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **SKU** | Standard_D4s_v3 | 4 vCPU, 16GB RAM |
| **스토리지 크기** | 256GB | Premium SSD |
| **스토리지 자동 증가** | 활성화 | 최대 2TB까지 자동 확장 |
| **IOPS** | 3000 | Provisioned IOPS |
| **처리량** | 125 MBps | 스토리지 처리량 |

### 2.2 네트워크 구성

#### 2.2.1 네트워크 설정
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **연결 방법** | Private access (VNet Integration) | VNet 통합 |
| **가상 네트워크** | phonebill-vnet-prod | 운영환경 VNet |
| **서브넷** | database-subnet (10.0.2.0/24) | 데이터베이스 전용 서브넷 |
| **Private DNS Zone** | privatelink.postgres.database.azure.com | 내부 DNS 해석 |

#### 2.2.2 방화벽 및 보안
```yaml
방화벽_규칙:
  - 규칙명: "AllowAKSSubnet"
    시작IP: "10.0.1.0"
    종료IP: "10.0.1.255"
    설명: "AKS Application Subnet 접근 허용"
    
  - 규칙명: "DenyAllOthers" 
    기본정책: "DENY"
    설명: "기본적으로 모든 외부 접근 차단"

Private_Endpoint:
  활성화: true
  서브넷: database-subnet
  보안: "VNet 내부 접근만 허용"
```

## 3. 고가용성 구성

### 3.1 Zone Redundant 고가용성

#### 3.1.1 고가용성 설정
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **고가용성 모드** | Zone Redundant | 가용영역 간 중복화 |
| **Primary Zone** | Zone 1 | 기본 가용영역 |
| **Standby Zone** | Zone 2 | 대기 가용영역 |
| **자동 장애조치** | 활성화 | 60초 이내 자동 전환 |
| **Standby 서버** | 동일 사양 | Primary와 동일한 리소스 |

#### 3.1.2 고가용성 아키텍처
```
┌─────────────────────┐    ┌─────────────────────┐
│    Korea Central    │    │    Korea Central    │
│       Zone 1        │    │       Zone 2        │
├─────────────────────┤    ├─────────────────────┤
│  Primary Server     │◄──►│  Standby Server     │
│  - Active/Read      │    │  - Standby/Write    │
│  - Write Traffic    │    │  - Auto Failover    │
│  - Read Traffic     │    │  - Sync Replication │
└─────────────────────┘    └─────────────────────┘
           │                           │
           └─────────┬─────────────────┘
                     │
       ┌─────────────▼─────────────┐
       │    Application Layer     │
       │  - Automatic Failover    │
       │  - Connection Retry      │
       │  - Circuit Breaker       │
       └───────────────────────────┘
```

### 3.2 읽기 복제본

#### 3.2.1 읽기 전용 복제본 구성
```yaml
읽기_복제본_1:
  위치: "Korea South"  # 지역적 분산
  목적: "재해복구 + 읽기 부하 분산"
  사양: "Standard_D2s_v3"  # Primary보다 낮은 사양
  스토리지: "128GB"
  
읽기_복제본_2:
  위치: "Korea Central"  # 동일 리전
  목적: "읽기 부하 분산"
  사양: "Standard_D2s_v3"
  스토리지: "128GB"

복제_설정:
  복제_지연: "< 5초"
  복제_방식: "비동기 복제"
  사용_용도:
    - 조회_쿼리_부하_분산
    - 리포팅_및_분석
    - 백업_작업_오프로드
```

## 4. 보안 설계

### 4.1 인증 및 권한 관리

#### 4.1.1 관리자 계정
| 계정 유형 | 계정명 | 권한 | 용도 |
|----------|--------|------|------|
| **서버 관리자** | `phonebill_admin` | SUPERUSER | 서버 관리, 스키마 생성 |
| **애플리케이션 계정** | `phonebill_auth_user` | DB/TABLE 권한 | Auth 서비스 연결 |
| **모니터링 계정** | `phonebill_monitor` | 읽기 전용 | 모니터링, 백업 |

#### 4.1.2 보안 구성
```yaml
보안_설정:
  암호_정책:
    최소_길이: 16자
    복잡성: "대소문자+숫자+특수문자"
    주기적_변경: "90일"
    
  연결_보안:
    SSL_필수: true
    TLS_버전: "1.2 이상"
    암호화_방식: "AES-256"
    
  접근_제어:
    Private_Endpoint: "필수"
    방화벽_규칙: "최소 권한 원칙"
    연결_제한: "최대 100개 동시 연결"

Azure_AD_통합:
  활성화: true
  관리자_계정: "phonebill-db-admins@company.com"
  MFA_필수: true
  조건부_접근: "회사 네트워크만"
```

### 4.2 데이터 보호

#### 4.2.1 암호화
```yaml
미사용_데이터_암호화:
  방식: "Microsoft 관리 키"
  알고리즘: "AES-256"
  범위: "전체 데이터베이스"
  
전송_중_암호화:
  SSL/TLS: "필수"
  인증서: "Azure 제공"
  프로토콜: "TLS 1.2+"
  
애플리케이션_레벨_암호화:
  비밀번호: "BCrypt + Salt"
  민감정보: "필요시 컬럼 레벨 암호화"
  토큰: "JWT with RSA-256"
```

## 5. 백업 및 복구

### 5.1 자동 백업 설정

#### 5.1.1 백업 구성
| 구성 항목 | 설정 값 | 설명 |
|----------|---------|------|
| **백업 보존 기간** | 35일 | 법규 준수 + 운영 요구사항 |
| **백업 주기** | 매일 자동 | 시스템 자동 실행 |
| **백업 시간** | 02:00 KST | 트래픽 최소 시간대 |
| **백업 압축** | 활성화 | 스토리지 비용 절약 |
| **지리적 중복** | 활성화 | Korea South 지역 복제 |

#### 5.1.2 Point-in-Time Recovery (PITR)
```yaml
PITR_설정:
  활성화: true
  복구_범위: "35일 이내 5분 단위"
  로그_백업: "5분 간격"
  복구_시간: "일반적으로 15-30분"
  
백업_전략:
  전체_백업: "주간 (일요일)"
  차등_백업: "일간"
  로그_백업: "5분 간격"
  
복구_목표:
  RTO: "30분" # Recovery Time Objective
  RPO: "5분"  # Recovery Point Objective
```

### 5.2 재해복구 전략

#### 5.2.1 재해복구 시나리오
```yaml
장애_시나리오:
  Primary_Zone_장애:
    복구_방법: "자동 Standby Zone 전환"
    예상_시간: "60초 이내"
    데이터_손실: "없음 (동기 복제)"
    
  전체_리전_장애:
    복구_방법: "Korea South 읽기 복제본 승격"
    예상_시간: "15-30분"
    데이터_손실: "최대 5초 (비동기 복제)"
    
  데이터_손상:
    복구_방법: "PITR을 통한 특정 시점 복구"
    예상_시간: "15-60분"
    데이터_손실: "최대 5분"

복구_절차:
  1단계: "장애 감지 및 알림"
  2단계: "자동/수동 장애조치 실행"  
  3단계: "애플리케이션 연결 재설정"
  4단계: "서비스 정상화 확인"
  5단계: "사후 분석 및 개선"
```

## 6. 성능 최적화

### 6.1 Connection Pool 설정

#### 6.1.1 연결 관리
```yaml
연결_설정:
  최대_연결수: 100
  예약_연결수: 10  # 관리용
  애플리케이션_연결: 90
  
HikariCP_설정:
  maximum_pool_size: 20
  minimum_idle: 5
  connection_timeout: 30000  # 30초
  idle_timeout: 600000       # 10분
  max_lifetime: 1800000      # 30분
  validation_query: "SELECT 1"
```

### 6.2 성능 모니터링

#### 6.2.1 주요 메트릭
```yaml
모니터링_지표:
  성능_메트릭:
    - CPU_사용률: "< 80%"
    - 메모리_사용률: "< 85%"  
    - 디스크_IOPS: "< 2500"
    - 연결_수: "< 80개"
    
  쿼리_성능:
    - 평균_응답시간: "< 100ms"
    - 슬로우_쿼리: "< 5개/시간"
    - 데드락: "0건"
    - 대기_시간: "< 50ms"
    
  가용성_지표:
    - 서버_가동률: "> 99.9%"
    - 장애조치_시간: "< 60초"
    - 백업_성공률: "100%"
```

## 7. 데이터베이스 구성

### 7.1 데이터베이스 및 사용자 생성

#### 7.1.1 데이터베이스 생성
```sql
-- 관리자 계정으로 실행
CREATE DATABASE phonebill_auth
    WITH ENCODING 'UTF8'
    LC_COLLATE = 'ko_KR.UTF-8'
    LC_CTYPE = 'ko_KR.UTF-8'
    TIMEZONE = 'Asia/Seoul';
```

#### 7.1.2 애플리케이션 사용자 생성
```sql
-- 애플리케이션 전용 사용자 생성
CREATE USER phonebill_auth_user WITH 
    PASSWORD 'Auth$ervice2025!Prod'
    CONNECTION LIMIT 50;

-- 데이터베이스 접근 권한 부여
GRANT CONNECT ON DATABASE phonebill_auth TO phonebill_auth_user;
GRANT USAGE ON SCHEMA public TO phonebill_auth_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO phonebill_auth_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO phonebill_auth_user;

-- 향후 생성될 테이블에 대한 권한 자동 부여
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO phonebill_auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
    GRANT USAGE, SELECT ON SEQUENCES TO phonebill_auth_user;
```

### 7.2 스키마 적용 계획

#### 7.2.1 스키마 파일 실행 순서
```bash
# 1. 데이터베이스 연결 및 확장 설치
psql -h phonebill-auth-postgresql-prod.postgres.database.azure.com \
     -U phonebill_admin \
     -d phonebill_auth \
     -f design/backend/database/auth-schema.psql

# 2. 스키마 생성 확인
psql -h phonebill-auth-postgresql-prod.postgres.database.azure.com \
     -U phonebill_admin \
     -d phonebill_auth \
     -c "\dt"

# 3. 초기 데이터 확인
psql -h phonebill-auth-postgresql-prod.postgres.database.azure.com \
     -U phonebill_admin \
     -d phonebill_auth \
     -c "SELECT COUNT(*) as service_count FROM auth_services;"
```

## 8. 모니터링 및 알림

### 8.1 Azure Monitor 통합

#### 8.1.1 메트릭 수집
```yaml
Azure_Monitor_설정:
  메트릭_수집:
    - 서버_성능_메트릭
    - 데이터베이스_성능_메트릭  
    - 연결_메트릭
    - 스토리지_메트릭
    
  로그_수집:
    - PostgreSQL_로그
    - 슬로우_쿼리_로그
    - 감사_로그
    - 오류_로그

진단_설정:
  로그_분석_작업영역: "law-phonebill-prod"
  메트릭_보존기간: "90일"
  로그_보존기간: "30일"
```

### 8.2 알림 설정

#### 8.2.2 Critical 알림
```yaml
Critical_알림:
  서버_다운:
    메트릭: "서버 가용성"
    임계값: "< 100%"
    지속시간: "1분"
    알림채널: "Teams + Email + SMS"
    
  CPU_과부하:
    메트릭: "CPU 사용률"
    임계값: "> 90%"
    지속시간: "5분"
    알림채널: "Teams + Email"
    
  메모리_부족:
    메트릭: "메모리 사용률"
    임계값: "> 95%"
    지속시간: "3분"  
    알림채널: "Teams + Email"
    
  연결_한계:
    메트릭: "활성 연결 수"
    임계값: "> 85개"
    지속시간: "2분"
    알림채널: "Teams"

Warning_알림:
  성능_저하:
    메트릭: "평균 응답시간"
    임계값: "> 200ms"
    지속시간: "10분"
    알림채널: "Teams"
    
  스토리지_사용량:
    메트릭: "스토리지 사용률"
    임계값: "> 80%"
    지속시간: "30분"
    알림채널: "Teams"
```

## 9. 설치 작업 계획

### 9.1 설치 단계

#### 9.1.1 사전 준비 작업
```yaml
사전_준비:
  - [ ] Azure 구독 및 리소스 그룹 확인
  - [ ] VNet 및 서브넷 구성 확인
  - [ ] 네트워크 보안 그룹(NSG) 규칙 확인
  - [ ] Private DNS Zone 설정 확인
  - [ ] 관리자 계정 권한 확인

필요_권한:
  - Contributor (PostgreSQL 인스턴스 생성)
  - Network Contributor (VNet 통합)
  - DNS Zone Contributor (Private DNS 설정)
```

#### 9.1.2 설치 작업 단계
```yaml
1단계_인프라_구성:
  - [ ] Azure Database for PostgreSQL Flexible Server 생성
  - [ ] Zone Redundant 고가용성 설정
  - [ ] VNet 통합 및 Private Endpoint 구성
  - [ ] 방화벽 규칙 설정
  - [ ] 예상소요시간: 30분

2단계_보안_설정:
  - [ ] 관리자 및 애플리케이션 계정 생성
  - [ ] Azure AD 통합 설정
  - [ ] SSL/TLS 인증서 구성
  - [ ] 접근 권한 설정
  - [ ] 예상소요시간: 20분

3단계_고가용성_구성:
  - [ ] 읽기 전용 복제본 생성 (Korea South)
  - [ ] 읽기 전용 복제본 생성 (Korea Central)
  - [ ] 장애조치 테스트 실행
  - [ ] 예상소요시간: 45분

4단계_데이터베이스_설정:
  - [ ] phonebill_auth 데이터베이스 생성
  - [ ] 스키마 파일 (auth-schema.psql) 실행
  - [ ] 초기 데이터 생성 확인
  - [ ] 애플리케이션 계정 권한 테스트
  - [ ] 예상소요시간: 15분

5단계_모니터링_설정:
  - [ ] Azure Monitor 진단 설정
  - [ ] 메트릭 및 로그 수집 활성화
  - [ ] 알림 규칙 생성
  - [ ] 대시보드 구성
  - [ ] 예상소요시간: 30분

6단계_검증_및_테스트:
  - [ ] 애플리케이션 연결 테스트
  - [ ] 성능 벤치마크 실행
  - [ ] 장애조치 시나리오 테스트  
  - [ ] 백업/복구 테스트
  - [ ] 예상소요시간: 60분

총_예상소요시간: "3시간 20분"
```

### 9.2 롤백 계획

#### 9.2.1 롤백 시나리오
```yaml
롤백_트리거:
  - 인스턴스_생성_실패
  - 네트워크_연결_불가
  - 성능_기준_미달성
  - 보안_검증_실패

롤백_절차:
  1단계: "진행중인 작업 중단"
  2단계: "생성된 Azure 리소스 삭제"
  3단계: "VNet/DNS 설정 원복"
  4단계: "사용자/권한 정리"
  5단계: "문제점_분석_및_재설치_계획_수립"

데이터_보호:
  - 기존_데이터_백업_확인
  - 스키마_파일_보관
  - 설정_정보_문서화
```

## 10. 운영 이관

### 10.1 인수인계 체크리스트

#### 10.1.1 기술 문서 이관
```yaml
문서_이관:
  - [ ] 데이터베이스 접속 정보 (암호화하여 전달)
  - [ ] 스키마 구조 및 ERD 다이어그램
  - [ ] 백업/복구 절차서
  - [ ] 성능 튜닝 가이드
  - [ ] 장애 대응 매뉴얼
  - [ ] 모니터링 대시보드 접근 권한

운영_정보:
  - [ ] 정기 점검 일정
  - [ ] 패치 적용 정책
  - [ ] 용량 관리 계획
  - [ ] 비용 모니터링 정보
```

### 10.2 운영 관리 방안

#### 10.2.1 일상 운영 작업
```yaml
일일_점검:
  - 서버 상태 확인
  - 성능 메트릭 모니터링
  - 백업 상태 확인
  - 보안 알림 검토

주간_점검:
  - 성능 분석 리포트 검토
  - 용량 사용량 분석
  - 슬로우 쿼리 분석
  - 보안 패치 확인

월간_점검:
  - 용량 계획 검토
  - 비용 분석
  - 성능 최적화 검토
  - 재해복구 테스트
```

## 11. 비용 분석

### 11.1 운영 비용 추정

#### 11.1.1 월간 비용 분석 (USD)
| 구성요소 | 사양 | 예상 비용 | 비고 |
|----------|------|-----------|------|
| **Primary Server** | Standard_D4s_v3 | $280 | 4 vCPU, 16GB RAM |
| **Standby Server** | Standard_D4s_v3 | $280 | Zone Redundant |
| **스토리지** | 256GB Premium SSD | $40 | IOPS 포함 |
| **읽기 복제본 (Korea South)** | Standard_D2s_v3 | $140 | 2 vCPU, 8GB RAM |
| **읽기 복제본 (Korea Central)** | Standard_D2s_v3 | $140 | 2 vCPU, 8GB RAM |
| **백업 스토리지** | 35일 보존 | $20 | 압축 적용 |
| **네트워크** | VNet 통합 | $15 | Private Link |
| **모니터링** | Azure Monitor | $10 | 로그 및 메트릭 |
| **총합** | | **$925** | |

#### 11.1.2 비용 최적화 방안
```yaml
단기_최적화:
  - Reserved_Instance: "1년 약정시 30% 절약"
  - 읽기_복제본_스케일링: "사용량 기반 조정" 
  - 백업_정책_조정: "보존기간 최적화"

중장기_최적화:
  - 성능_기반_사이징: "실제 사용량 분석 후 조정"
  - 읽기_복제본_지역_최적화: "트래픽 패턴 분석"
  - 아카이빙_정책: "오래된 데이터 별도 보관"
```

## 12. 위험 관리

### 12.1 위험 요소 및 대응 방안

#### 12.1.1 기술적 위험
| 위험 요소 | 발생 확률 | 영향도 | 대응 방안 |
|----------|----------|-------|-----------|
| **네트워크 연결 실패** | 중간 | 높음 | Private Link 다중화, 연결 재시도 로직 |
| **성능 저하** | 낮음 | 중간 | 읽기 복제본 활용, 쿼리 최적화 |
| **데이터 손실** | 낮음 | 매우 높음 | Zone Redundant HA, PITR 백업 |
| **보안 침해** | 낮음 | 높음 | Private Endpoint, Azure AD 통합 |

#### 12.1.2 운영적 위험
```yaml
운영_위험:
  설치_지연:
    원인: "네트워크 설정 복잡성"
    대응: "사전 테스트 환경에서 검증"
    
  비용_초과:
    원인: "리소스 오버 프로비저닝"
    대응: "단계적 확장, 비용 모니터링"
    
  성능_미달:
    원인: "부하 패턴 예측 오차"
    대응: "성능 테스트, 단계적 최적화"
```

## 13. 성공 기준

### 13.1 설치 완료 기준

#### 13.1.1 기술적 기준
```yaml
완료_기준:
  가용성:
    - [ ] Zone Redundant 고가용성 정상 작동
    - [ ] 자동 장애조치 60초 이내 완료
    - [ ] 읽기 복제본 정상 동기화
    
  성능:
    - [ ] 평균 응답시간 < 100ms
    - [ ] 동시 연결 수 100개 지원
    - [ ] TPS 500 이상 처리
    
  보안:
    - [ ] Private Endpoint 연결만 허용
    - [ ] SSL/TLS 암호화 적용
    - [ ] 애플리케이션 계정 최소 권한 적용
    
  백업:
    - [ ] 자동 백업 정상 실행
    - [ ] PITR 복구 테스트 성공
    - [ ] 지리적 복제 정상 작동
```

## 14. 설치 일정

### 14.1 작업 일정표

| 일정 | 작업 내용 | 담당자 | 소요 시간 |
|------|-----------|--------|-----------|
| **D-Day** | 사전 준비 및 인프라 구성 | 데옵스 (최운영) | 1시간 |
| **D-Day** | 보안 설정 및 고가용성 구성 | 백엔더 (이개발) | 1시간 |
| **D-Day** | 데이터베이스 및 스키마 설정 | 백엔더 (이개발) | 30분 |
| **D-Day** | 모니터링 및 알림 설정 | 데옵스 (최운영) | 30분 |
| **D+1** | 애플리케이션 연결 테스트 | 백엔더 (이개발) | 1시간 |
| **D+1** | 성능 및 장애조치 테스트 | QA매니저 (정테스트) | 2시간 |
| **D+2** | 최종 검증 및 운영 이관 | 전체 팀 | 1시간 |

---

**계획서 작성일**: 2025-01-08  
**작성자**: 데옵스 (최운영)  
**검토자**: 백엔더 (이개발), QA매니저 (정테스트)  
**승인자**: 아키텍트 (김기획)

---

> **참고**: 이 계획서는 설치 전 최종 검토가 필요하며, 실제 환경에 따라 일부 설정값이 조정될 수 있습니다.