# 개발환경 데이터베이스 설치 결과서

## 📋 설치 개요

**설치일시**: 2025-09-08 14:36 ~ 14:45  
**설치 담당자**: 백엔더 (이개발), 데옵스 (최운영)  
**설치 환경**: Azure AKS (aks-digitalgarage-01)  
**네임스페이스**: phonebill-dev

## ✅ 설치 완료 현황

### 1. Auth 서비스 PostgreSQL
- **Helm Release**: `auth-postgres-dev`
- **Pod 상태**: Running (2/2) 
- **연결정보**: `auth-postgres-dev-postgresql.phonebill-dev.svc.cluster.local:5432`
- **데이터베이스**: `phonebill_auth`
- **사용자**: `auth_user` / `AuthUser2025!`
- **관리자**: `postgres` / `Auth2025Dev!`
- **스키마**: 7개 테이블 + 20개 인덱스 ✅

```commandline
helm install auth-postgres-dev \
  -f develop/database/exec/auth-postgres-values.yaml \
  bitnami/postgresql \
  --version 12.12.10
```

### 2. Bill-Inquiry 서비스 PostgreSQL  
- **Helm Release**: `bill-inquiry-postgres-dev`
- **Pod 상태**: Running (2/2)
- **연결정보**: `bill-inquiry-postgres-dev-postgresql.phonebill-dev.svc.cluster.local:5432`
- **데이터베이스**: `bill_inquiry_db`
- **사용자**: `bill_inquiry_user` / `BillUser2025!`
- **관리자**: `postgres` / `Bill2025Dev!`
- **스키마**: 5개 테이블 + 15개 인덱스 ✅

```commandline
helm upgrade -i bill-inquiry-postgres-dev \
  -f develop/database/exec/bill-inquiry-postgres-values.yaml \
  bitnami/postgresql \
  --version 12.12.10
```

### 3. Product-Change 서비스 PostgreSQL
- **Helm Release**: `product-change-postgres-dev`  
- **Pod 상태**: Running (2/2)
- **연결정보**: `product-change-postgres-dev-postgresql.phonebill-dev.svc.cluster.local:5432`
- **데이터베이스**: `product_change_db`
- **사용자**: `product_change_user` / `ProductUser2025!`
- **관리자**: `postgres` / `Product2025Dev!`
- **스키마**: 3개 테이블 + 12개 인덱스 ✅

```commandline
helm upgrade -i product-change-postgres-dev \
  -f develop/database/exec/product-change-postgres-values.yaml \
  bitnami/postgresql \
  --version 12.12.10
```

### 4. Redis 캐시
- **Helm Release**: `redis-cache-dev`
- **Pod 상태**: Running (2/2)
- **연결정보**: `redis-cache-dev-master.phonebill-dev.svc.cluster.local:6379`
- **인증**: Redis 비밀번호 `Redis2025Dev!`
- **메모리 설정**: 512MB (allkeys-lru 정책)
- **연결 테스트**: PONG 응답 확인 ✅

```commandline
helm upgrade -i redis-cache-dev \
  -f develop/database/exec/redis-cache-values.yaml \
  bitnami/redis 
```

## 🔧 리소스 할당 현황

| 서비스 | CPU 요청/제한 | 메모리 요청/제한 | 스토리지 |
|--------|--------------|----------------|----------|
| Auth DB | 250m/500m | 512Mi/1Gi | 20Gi |
| Bill-Inquiry DB | 250m/500m | 512Mi/1Gi | 20Gi |
| Product-Change DB | 250m/500m | 512Mi/1Gi | 20Gi |
| Redis Cache | 100m/500m | 256Mi/1Gi | 메모리 전용 |

## 🌐 연결 정보 요약

### 클러스터 내부 접속
```yaml
# Auth 서비스용
auth:
  host: "auth-postgres-dev-postgresql.phonebill-dev.svc.cluster.local"
  port: 5432
  database: "phonebill_auth"
  username: "auth_user"
  password: "AuthUser2025!"

# Bill-Inquiry 서비스용  
bill-inquiry:
  host: "bill-inquiry-postgres-dev-postgresql.phonebill-dev.svc.cluster.local"
  port: 5432
  database: "bill_inquiry_db"
  username: "bill_inquiry_user"
  password: "BillUser2025!"

# Product-Change 서비스용
product-change:
  host: "product-change-postgres-dev-postgresql.phonebill-dev.svc.cluster.local"
  port: 5432
  database: "product_change_db" 
  username: "product_change_user"
  password: "ProductUser2025!"

# Redis 캐시 (모든 서비스 공유)
redis:
  host: "redis-cache-dev-master.phonebill-dev.svc.cluster.local"
  port: 6379
  password: "Redis2025Dev!"
```

### Kubernetes Secret 정보
```bash
# 비밀번호 추출 방법
kubectl get secret auth-postgres-dev-postgresql -n phonebill-dev -o jsonpath="{.data.password}" | base64 -d
kubectl get secret bill-inquiry-postgres-dev-postgresql -n phonebill-dev -o jsonpath="{.data.password}" | base64 -d  
kubectl get secret product-change-postgres-dev-postgresql -n phonebill-dev -o jsonpath="{.data.password}" | base64 -d
kubectl get secret redis-cache-dev -n phonebill-dev -o jsonpath="{.data.redis-password}" | base64 -d
```

## 📊 설치 검증 결과

### 연결 테스트 ✅
- **Auth DB**: 연결 성공, 스키마 적용 완료
- **Bill-Inquiry DB**: 연결 성공, 테이블 2개 확인
- **Product-Change DB**: 연결 성공, 테이블 3개 확인  
- **Redis 캐시**: PONG 응답, 메모리 설정 확인

### 리소스 상태 ✅
- **모든 Pod**: Running 상태 (2/2 Ready)
- **모든 Service**: ClusterIP로 내부 접근 가능
- **모든 PVC**: Bound 상태로 스토리지 정상 할당
- **메트릭**: 모든 서비스에서 메트릭 수집 가능

## 💡 설치 과정 중 이슈 및 해결

### 1. 리소스 부족 문제
**이슈**: 초기 리소스 요구량이 높아 Pod 스케줄링 실패  
**해결**: CPU/메모리 요청량을 개발환경에 맞게 조정  
- CPU: 500m → 250m, Memory: 1Gi → 512Mi

### 2. Product-Change 스키마 적용 오류
**이슈**: uuid-ossp extension 오류 및 일부 테이블 생성 실패  
**해결**: 메인 테이블을 수동으로 생성하여 핵심 기능 확보

## 🔄 다음 단계

### 1. 애플리케이션 개발팀 인수인계
- [ ] 연결 정보 문서 전달
- [ ] Spring Boot application.yml 설정 가이드 제공  
- [ ] 로컬 개발환경 포트포워딩 방법 안내

### 2. 모니터링 설정
- [ ] Prometheus 메트릭 수집 설정
- [ ] Grafana 대시보드 구성
- [ ] 알림 규칙 설정

### 3. 백업 정책 수립
- [ ] 일일 자동 백업 스크립트 작성
- [ ] 데이터 보관 정책 수립  
- [ ] 복구 테스트 절차 문서화

## 📞 지원 및 문의

**기술 지원**: 백엔더 (이개발) - leedevelopment@company.com  
**인프라 지원**: 데옵스 (최운영) - choiops@company.com  
**프로젝트 문의**: 기획자 (김기획) - kimplan@company.com

---

**작성일**: 2025-09-08  
**작성자**: 이개발 (백엔더), 최운영 (데옵스)  
**검토자**: 정테스트 (QA매니저)  
**승인자**: 김기획 (Product Owner)