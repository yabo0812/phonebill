# Auth 서비스 개발환경 데이터베이스 설치 계획서

## 1. 개요

### 1.1 설치 목적
- Auth 서비스(`phonebill_auth`)의 개발환경용 PostgreSQL 데이터베이스 구축
- Kubernetes StatefulSet을 활용한 컨테이너 기반 배포
- 개발팀의 빠른 개발과 검증을 위한 최적화 설정

### 1.2 설치 환경
- **클러스터**: Azure Kubernetes Service (AKS)  
- **네임스페이스**: `phonebill-dev`
- **데이터베이스**: `phonebill_auth`
- **DBMS**: PostgreSQL 16 (Bitnami 이미지)
- **배포 방식**: Helm Chart + StatefulSet

### 1.3 참조 문서
- 개발환경 물리아키텍처: `design/backend/physical/physical-architecture-dev.md`
- Auth 서비스 데이터 설계서: `design/backend/database/auth.md`  
- Auth 스키마 스크립트: `design/backend/database/auth-schema.psql`
- 백킹서비스 설치 가이드: `claude/backing-service-method.md`

## 2. 시스템 요구사항

### 2.1 하드웨어 사양
| 항목 | 요구사양 | 설명 |
|------|----------|------|
| CPU | 500m (요청) / 1000m (제한) | 개발환경 적정 사양 |
| Memory | 1Gi (요청) / 2Gi (제한) | Auth 서비스 전용 DB |
| Storage | 20Gi (Azure Disk Standard) | 개발 데이터 + 로그 저장 |
| Node | Standard_B2s (2vCPU, 4GB) | AKS 개발환경 노드 |

### 2.2 네트워크 구성
| 설정 항목 | 값 | 설명 |
|-----------|-------|-------|
| 네트워크 | Azure CNI | AKS 기본 네트워크 플러그인 |
| 서비스 타입 | ClusterIP | 클러스터 내부 통신 |
| 외부 접근 | LoadBalancer (개발용) | 개발팀 접근을 위한 외부 서비스 |
| 포트 | 5432 | PostgreSQL 기본 포트 |

### 2.3 스토리지 구성  
| 설정 항목 | 값 | 설명 |
|-----------|-------|-------|
| Storage Class | `managed-standard` | Azure Disk Standard |
| 볼륨 크기 | 20Gi | 개발환경 충분한 용량 |
| 접근 모드 | ReadWriteOnce | 단일 노드 접근 |
| 백업 정책 | Azure Disk Snapshot | 일일 자동 백업 |

## 3. 데이터베이스 설계 정보

### 3.1 데이터베이스 정보
- **데이터베이스명**: `phonebill_auth`
- **문자셋**: UTF-8
- **시간대**: Asia/Seoul
- **확장**: `uuid-ossp`, `pgcrypto`

### 3.2 테이블 구성 (7개)
| 테이블명 | 목적 | 주요 기능 |
|----------|------|----------|
| `auth_users` | 사용자 계정 | 로그인 ID, 비밀번호, 계정 상태 |
| `auth_user_sessions` | 세션 관리 | JWT 토큰, 세션 상태 추적 |
| `auth_services` | 서비스 정의 | 시스템 내 서비스 목록 |
| `auth_permissions` | 권한 정의 | 서비스별 권한 코드 |
| `auth_user_permissions` | 사용자 권한 | 사용자별 권한 할당 |
| `auth_login_history` | 로그인 이력 | 성공/실패 로그 추적 |
| `auth_permission_access_log` | 권한 접근 로그 | 권한 기반 접근 감사 |

### 3.3 보안 설정
- **비밀번호 암호화**: BCrypt + 개별 솔트
- **계정 잠금**: 5회 실패 시 30분 잠금
- **세션 관리**: JWT 토큰 + 리프레시 토큰  
- **접근 제어**: 서비스 계정별 최소 권한

## 4. 설치 절차

### 4.1 사전 준비

#### 4.1.1 AKS 클러스터 확인
```bash
# AKS 클러스터 상태 확인
kubectl cluster-info

# 네임스페이스 생성
kubectl create namespace phonebill-dev
kubectl config set-context --current --namespace=phonebill-dev
```

#### 4.1.2 Helm Repository 설정
```bash
# Bitnami Helm Repository 추가
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Repository 확인
helm repo list
```

#### 4.1.3 작업 디렉토리 준비
```bash
# 설치 디렉토리 생성
mkdir -p ~/install/auth-db-dev
cd ~/install/auth-db-dev
```

### 4.2 PostgreSQL 설치

#### 4.2.1 Values.yaml 설정 파일 작성
```yaml
# values.yaml - Auth DB 개발환경 설정
# PostgreSQL 기본 설정
global:
  postgresql:
    auth:
      postgresPassword: "Auth2025Dev!"
      database: "phonebill_auth"
      username: "auth_user"
      password: "AuthUser2025!"
  storageClass: "managed-standard"

# Primary 설정 (개발환경 단독 구성)
architecture: standalone

primary:
  # 리소스 설정 (개발환경 최적화)
  resources:
    limits:
      memory: "2Gi"
      cpu: "1000m"
    requests:
      memory: "1Gi"
      cpu: "500m"

  # 스토리지 설정
  persistence:
    enabled: true
    storageClass: "managed-standard"
    size: 20Gi

  # PostgreSQL 성능 설정 (개발환경 최적화)
  extraEnvVars:
    - name: POSTGRESQL_SHARED_BUFFERS
      value: "256MB"
    - name: POSTGRESQL_EFFECTIVE_CACHE_SIZE
      value: "1GB"
    - name: POSTGRESQL_MAX_CONNECTIONS
      value: "100"
    - name: POSTGRESQL_WORK_MEM
      value: "4MB"
    - name: POSTGRESQL_MAINTENANCE_WORK_MEM
      value: "64MB"

  # 초기화 스크립트 설정
  initdb:
    scripts:
      00-extensions.sql: |
        -- PostgreSQL 확장 설치
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
        CREATE EXTENSION IF NOT EXISTS "pgcrypto";
      01-database.sql: |
        -- Auth 데이터베이스 생성 확인
        SELECT 'phonebill_auth database ready' as status;

# 서비스 설정
service:
  type: ClusterIP
  ports:
    postgresql: 5432

# 네트워크 정책 (개발환경 허용적 설정)
networkPolicy:
  enabled: false

# 보안 설정 (개발환경 기본 설정)
securityContext:
  enabled: true
  fsGroup: 1001
  runAsUser: 1001

# 메트릭 설정 (개발환경 모니터링)
metrics:
  enabled: true
  service:
    type: ClusterIP

# 백업 설정 (개발환경 기본)
backup:
  enabled: false  # 개발환경에서는 수동 백업
```

#### 4.2.2 PostgreSQL 설치 실행
```bash
# Helm을 통한 PostgreSQL 설치
helm install auth-postgres-dev \
  -f values.yaml \
  bitnami/postgresql \
  --version 12.12.10 \
  --namespace phonebill-dev

# 설치 진행 상황 모니터링
watch kubectl get pods -n phonebill-dev
```

#### 4.2.3 설치 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -l app.kubernetes.io/name=postgresql -n phonebill-dev

# StatefulSet 상태 확인  
kubectl get statefulset -n phonebill-dev

# 서비스 확인
kubectl get svc -l app.kubernetes.io/name=postgresql -n phonebill-dev

# PVC 확인
kubectl get pvc -n phonebill-dev
```

### 4.3 외부 접근 설정 (개발용)

#### 4.3.1 외부 접근 서비스 생성
```yaml
# auth-postgres-external.yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-postgres-external
  namespace: phonebill-dev
  labels:
    app: auth-postgres-dev
    purpose: external-access
spec:
  type: LoadBalancer
  ports:
  - name: postgresql
    port: 5432
    targetPort: 5432
    protocol: TCP
  selector:
    app.kubernetes.io/name: postgresql
    app.kubernetes.io/instance: auth-postgres-dev
    app.kubernetes.io/component: primary
```

#### 4.3.2 외부 서비스 배포
```bash
# 외부 접근 서비스 생성
kubectl apply -f auth-postgres-external.yaml

# LoadBalancer IP 확인 (할당까지 대기)
kubectl get svc auth-postgres-external -n phonebill-dev -w
```

### 4.4 스키마 적용

#### 4.4.1 데이터베이스 연결 확인
```bash
# PostgreSQL Pod 이름 확인
POSTGRES_POD=$(kubectl get pods -l app.kubernetes.io/name=postgresql,app.kubernetes.io/component=primary -n phonebill-dev -o jsonpath="{.items[0].metadata.name}")

# 데이터베이스 접속 테스트
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -d phonebill_auth -c "SELECT version();"
```

#### 4.4.2 스키마 스크립트 적용
```bash
# 로컬 스키마 파일을 Pod로 복사
kubectl cp design/backend/database/auth-schema.psql $POSTGRES_POD:/tmp/auth-schema.psql -n phonebill-dev

# 스키마 적용 실행
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -d phonebill_auth -f /tmp/auth-schema.psql

# 스키마 적용 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -d phonebill_auth -c "\\dt"
```

#### 4.4.3 초기 데이터 확인
```bash
# 서비스 테이블 데이터 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -d phonebill_auth -c "SELECT * FROM auth_services;"

# 권한 테이블 데이터 확인  
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -d phonebill_auth -c "SELECT * FROM auth_permissions;"

# 샘플 사용자 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -d phonebill_auth -c "SELECT user_id, customer_id, account_status FROM auth_users;"
```

## 5. 연결 정보

### 5.1 클러스터 내부 접속
```yaml
# Auth Service에서 사용할 연결 정보
apiVersion: v1
kind: Secret
metadata:
  name: auth-db-secret
  namespace: phonebill-dev
type: Opaque
data:
  # Base64 인코딩된 값
  database-url: "postgresql://auth_user:AuthUser2025!@auth-postgres-dev-postgresql:5432/phonebill_auth"
  postgres-password: "QXV0aDIwMjVEZXYh"  # Auth2025Dev!
  auth-user-password: "QXV0aFVzZXIyMDI1IQ=="  # AuthUser2025!
```

### 5.2 개발팀 외부 접속
```bash
# LoadBalancer IP 확인 (설치 완료 후)
EXTERNAL_IP=$(kubectl get svc auth-postgres-external -n phonebill-dev -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "External Access: $EXTERNAL_IP:5432"

# DBeaver 연결 설정
Host: $EXTERNAL_IP
Port: 5432
Database: phonebill_auth
Username: postgres
Password: Auth2025Dev!
```

## 6. 백업 및 복구 설정

### 6.1 수동 백업 방법
```bash
# 데이터베이스 백업
kubectl exec $POSTGRES_POD -n phonebill-dev -- pg_dump -U postgres phonebill_auth > auth-db-backup-$(date +%Y%m%d).sql

# 압축 백업
kubectl exec $POSTGRES_POD -n phonebill-dev -- pg_dump -U postgres phonebill_auth | gzip > auth-db-backup-$(date +%Y%m%d).sql.gz
```

### 6.2 Azure Disk 스냅샷 백업
```bash
# PV 정보 확인
kubectl get pv -o wide

# Azure Disk 스냅샷 생성 (Azure CLI)
az snapshot create \
  --resource-group phonebill-dev-rg \
  --name auth-postgres-snapshot-$(date +%Y%m%d) \
  --source /subscriptions/{subscription-id}/resourceGroups/{resource-group}/providers/Microsoft.Compute/disks/{disk-name}
```

### 6.3 데이터 복구 절차
```bash
# SQL 파일로부터 복원
kubectl exec -i $POSTGRES_POD -n phonebill-dev -- psql -U postgres phonebill_auth < auth-db-backup.sql

# 압축 파일로부터 복원  
gunzip -c auth-db-backup.sql.gz | kubectl exec -i $POSTGRES_POD -n phonebill-dev -- psql -U postgres phonebill_auth
```

## 7. 모니터링 및 관리

### 7.1 상태 모니터링
```bash
# Pod 리소스 사용량 확인
kubectl top pod -l app.kubernetes.io/name=postgresql -n phonebill-dev

# 연결 상태 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active';"

# 데이터베이스 크기 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SELECT pg_size_pretty(pg_database_size('phonebill_auth'));"
```

### 7.2 로그 확인
```bash
# PostgreSQL 로그 확인
kubectl logs -f $POSTGRES_POD -n phonebill-dev

# 최근 로그 확인 (100줄)
kubectl logs --tail=100 $POSTGRES_POD -n phonebill-dev
```

### 7.3 성능 튜닝 확인
```bash
# PostgreSQL 설정 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SHOW shared_buffers;"
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SHOW effective_cache_size;"
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SHOW max_connections;"
```

## 8. 트러블슈팅

### 8.1 일반적인 문제 해결

#### Pod 시작 실패
```bash
# Pod 상태 상세 확인
kubectl describe pod $POSTGRES_POD -n phonebill-dev

# 이벤트 확인
kubectl get events -n phonebill-dev --sort-by='.lastTimestamp'

# PVC 상태 확인
kubectl describe pvc data-auth-postgres-dev-postgresql-0 -n phonebill-dev
```

#### 연결 실패
```bash
# 서비스 엔드포인트 확인
kubectl get endpoints -n phonebill-dev

# 네트워크 정책 확인 
kubectl get networkpolicies -n phonebill-dev

# DNS 해석 확인
kubectl run debug --image=busybox -it --rm -- nslookup auth-postgres-dev-postgresql.phonebill-dev.svc.cluster.local
```

#### 성능 문제
```bash
# 느린 쿼리 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SELECT query, calls, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# 연결 수 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

# 락 대기 확인
kubectl exec -it $POSTGRES_POD -n phonebill-dev -- psql -U postgres -c "SELECT * FROM pg_locks WHERE NOT granted;"
```

### 8.2 복구 절차
```bash
# StatefulSet 재시작
kubectl rollout restart statefulset auth-postgres-dev-postgresql -n phonebill-dev

# Pod 강제 삭제 및 재생성
kubectl delete pod $POSTGRES_POD -n phonebill-dev --grace-period=0 --force

# 전체 재설치 (데이터 손실 주의)
helm uninstall auth-postgres-dev -n phonebill-dev
# PVC도 함께 삭제하려면
kubectl delete pvc data-auth-postgres-dev-postgresql-0 -n phonebill-dev
```

## 9. 보안 고려사항

### 9.1 개발환경 보안 설정
- **네트워크 접근**: 개발팀 IP만 허용 (NSG 규칙)
- **인증**: 강력한 패스워드 정책 적용
- **권한**: 최소 필요 권한만 부여
- **감사**: 모든 접근 로그 기록

### 9.2 프로덕션 전환 시 고려사항
- **데이터 암호화**: TDE (Transparent Data Encryption) 적용
- **네트워크 격리**: Private Endpoint 사용
- **백업 암호화**: 백업 데이터 암호화 저장
- **접근 제어**: Azure AD 통합 인증

## 10. 비용 최적화

### 10.1 개발환경 비용 절약 방안
- **Storage**: Standard SSD 사용 (Premium 대비 60% 절약)
- **Node**: Spot Instance 활용 (70% 비용 절약)
- **Auto-Scaling**: 개발 시간외 Pod 스케일다운
- **리소스 Right-sizing**: 실사용량 기반 리소스 조정

### 10.2 예상 월간 비용
| 항목 | 사양 | 월간 비용 (USD) |
|------|------|-----------------|
| AKS 관리 비용 | Managed Service | $73 |
| 컴퓨팅 (노드) | Standard_B2s | $60 |
| 스토리지 | Standard 20GB | $2 |
| 네트워크 | LoadBalancer Basic | $18 |
| **총합** | | **$153** |

## 11. 마이그레이션 계획

### 11.1 운영환경 전환 계획
1. **데이터 익스포트**: 개발 데이터 백업 및 정리
2. **스키마 검증**: 운영환경 스키마 호환성 확인  
3. **성능 테스트**: 운영 워크로드 시뮬레이션
4. **보안 강화**: 프로덕션 보안 정책 적용
5. **모니터링**: 운영 모니터링 시스템 구축

### 11.2 데이터 마이그레이션
```bash
# 스키마만 익스포트 (데이터 제외)
kubectl exec $POSTGRES_POD -n phonebill-dev -- pg_dump -U postgres --schema-only phonebill_auth > auth-schema-only.sql

# 특정 테이블 데이터 익스포트
kubectl exec $POSTGRES_POD -n phonebill-dev -- pg_dump -U postgres -t auth_services -t auth_permissions phonebill_auth > auth-reference-data.sql
```

## 12. 완료 체크리스트

### 12.1 설치 완료 확인
- [ ] PostgreSQL Pod 정상 실행 상태
- [ ] 스키마 및 테이블 생성 완료 (7개 테이블)
- [ ] 초기 데이터 적용 완료 (서비스, 권한, 샘플 사용자)
- [ ] 클러스터 내부 연결 테스트 성공
- [ ] 외부 접근 서비스 구성 완료
- [ ] 백업 절차 테스트 완료

### 12.2 개발팀 인수인계
- [ ] 연결 정보 전달 (내부/외부 접속)
- [ ] DBeaver 연결 설정 가이드 제공
- [ ] 백업/복구 절차 문서 전달
- [ ] 트러블슈팅 가이드 공유
- [ ] 모니터링 대시보드 접근 권한 부여

---

**작성자**: 이개발 (백엔더)  
**작성일**: 2025-09-08  
**검토자**: 최운영 (데옵스), 정테스트 (QA매니저)  
**승인자**: 김기획 (Product Owner)

**다음 단계**: Auth 서비스 애플리케이션 개발 및 데이터베이스 연동 테스트