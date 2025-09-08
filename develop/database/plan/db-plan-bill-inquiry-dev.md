# Bill-Inquiry 서비스 개발환경 데이터베이스 설치 계획서

## 1. 개요

### 1.1 설치 목적
- Bill-Inquiry 서비스 전용 PostgreSQL 14 데이터베이스 개발환경 구축
- 요금조회 기능을 위한 독립적인 데이터베이스 환경 제공
- Kubernetes StatefulSet을 통한 안정적인 데이터 지속성 보장

### 1.2 설치 환경
- **플랫폼**: Azure Kubernetes Service (AKS)
- **환경**: 개발환경 (Development)
- **네임스페이스**: phonebill-dev
- **클러스터**: phonebill-dev-aks (2 노드, Standard_B2s)

### 1.3 참조 문서
- 물리아키텍처 설계서: design/backend/physical/physical-architecture-dev.md
- 데이터 설계 종합: design/backend/database/data-design-summary.md
- Bill-Inquiry 데이터 설계서: design/backend/database/bill-inquiry.md
- 스키마 파일: design/backend/database/bill-inquiry-schema.psql

## 2. 데이터베이스 구성 정보

### 2.1 기본 정보
| 항목 | 값 | 설명 |
|------|----|----|
| 데이터베이스명 | bill_inquiry_db | Bill-Inquiry 서비스 전용 DB |
| DBMS | PostgreSQL 14 | 안정화된 PostgreSQL 14 버전 |
| 컨테이너 이미지 | bitnami/postgresql:14 | Bitnami 공식 이미지 |
| 문자셋 | UTF8 | 한글 지원을 위한 UTF8 |
| 타임존 | Asia/Seoul | 한국 표준시 |
| 초기 사용자 | postgres | 관리자 계정 |
| 초기 비밀번호 | Hi5Jessica! | 개발환경용 고정 비밀번호 |

### 2.2 스키마 구성
| 스키마 | 용도 | 테이블 수 |
|--------|------|---------|
| public | 비즈니스 테이블 | 5개 |
| cache | 캐시 데이터 (Redis 보조용) | 포함됨 |
| audit | 감사 및 이력 | 포함됨 |

### 2.3 주요 테이블
| 테이블명 | 용도 | 예상 데이터량 |
|----------|------|-------------|
| customer_info | 고객정보 임시 캐시 | 소규모 |
| bill_inquiry_history | 요금조회 요청 이력 | 중간규모 |
| kos_inquiry_history | KOS 연동 이력 | 중간규모 |
| bill_info_cache | 요금정보 캐시 | 소규모 |
| system_config | 시스템 설정 | 소규모 |

## 3. 리소스 할당 계획

### 3.1 컴퓨팅 리소스
| 리소스 유형 | 요청량 | 제한량 | 설명 |
|------------|--------|--------|------|
| CPU | 500m | 1000m | 개발환경 최적화 |
| Memory | 1Gi | 2Gi | 기본 워크로드 대응 |
| Storage | 20Gi | - | 개발 데이터 충분 용량 |

### 3.2 스토리지 구성
| 설정 항목 | 값 | 설명 |
|-----------|----|----|
| 스토리지 클래스 | managed-standard | Azure Disk Standard HDD |
| 볼륨 타입 | PersistentVolumeClaim | 데이터 지속성 보장 |
| 마운트 경로 | /bitnami/postgresql | 표준 데이터 디렉토리 |
| 백업 방식 | Azure Disk Snapshot | 일일 자동 백업 |

### 3.3 네트워크 구성
| 설정 항목 | 값 | 설명 |
|-----------|----|----|
| Service 타입 | ClusterIP | 클러스터 내부 접근 |
| 내부 포트 | 5432 | PostgreSQL 표준 포트 |
| Service 이름 | postgresql-bill-inquiry | 서비스 디스커버리용 |
| DNS 주소 | postgresql-bill-inquiry.phonebill-dev.svc.cluster.local | 내부 접근 주소 |

## 4. PostgreSQL 설정

### 4.1 성능 최적화 설정
| 설정 항목 | 값 | 설명 |
|-----------|----|----|
| max_connections | 100 | 개발환경 충분한 연결 수 |
| shared_buffers | 256MB | 메모리의 25% 할당 |
| effective_cache_size | 1GB | 총 메모리의 75% |
| work_mem | 4MB | 작업 메모리 |
| maintenance_work_mem | 64MB | 유지보수 작업 메모리 |

### 4.2 로그 설정
| 설정 항목 | 값 | 설명 |
|-----------|----|----|
| log_destination | stderr | 표준 에러로 로그 출력 |
| log_min_duration_statement | 1000ms | 1초 이상 쿼리 로그 |
| log_statement | none | 개발환경용 최소 로깅 |
| log_connections | on | 연결 로그 활성화 |

### 4.3 보안 설정
| 설정 항목 | 값 | 설명 |
|-----------|----|----|
| 비밀번호 암호화 | BCrypt | 안전한 비밀번호 저장 |
| SSL 모드 | require | TLS 암호화 통신 |
| 접근 제어 | md5 | 비밀번호 기반 인증 |
| 외부 접근 | 제한 | 클러스터 내부만 허용 |

## 5. Kubernetes 매니페스트

### 5.1 ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-bill-inquiry-config
  namespace: phonebill-dev
data:
  POSTGRES_DB: "bill_inquiry_db"
  POSTGRES_USER: "postgres"
  POSTGRESQL_MAX_CONNECTIONS: "100"
  POSTGRESQL_SHARED_BUFFERS: "256MB"
  POSTGRESQL_EFFECTIVE_CACHE_SIZE: "1GB"
  POSTGRESQL_WORK_MEM: "4MB"
  POSTGRESQL_MAINTENANCE_WORK_MEM: "64MB"
  POSTGRESQL_LOG_MIN_DURATION_STATEMENT: "1000"
```

### 5.2 Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-bill-inquiry-secret
  namespace: phonebill-dev
type: Opaque
data:
  postgres-password: SGk1SmVzc2ljYSE= # Hi5Jessica!
```

### 5.3 PersistentVolumeClaim
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgresql-bill-inquiry-pvc
  namespace: phonebill-dev
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: managed-standard
  resources:
    requests:
      storage: 20Gi
```

### 5.4 StatefulSet
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql-bill-inquiry
  namespace: phonebill-dev
  labels:
    app: postgresql-bill-inquiry
    tier: database
spec:
  serviceName: postgresql-bill-inquiry
  replicas: 1
  selector:
    matchLabels:
      app: postgresql-bill-inquiry
  template:
    metadata:
      labels:
        app: postgresql-bill-inquiry
        tier: database
    spec:
      containers:
      - name: postgresql
        image: bitnami/postgresql:14
        imagePullPolicy: IfNotPresent
        env:
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRES_DB
        - name: POSTGRES_USER
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-bill-inquiry-secret
              key: postgres-password
        - name: POSTGRESQL_MAX_CONNECTIONS
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRESQL_MAX_CONNECTIONS
        - name: POSTGRESQL_SHARED_BUFFERS
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRESQL_SHARED_BUFFERS
        - name: POSTGRESQL_EFFECTIVE_CACHE_SIZE
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRESQL_EFFECTIVE_CACHE_SIZE
        - name: POSTGRESQL_WORK_MEM
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRESQL_WORK_MEM
        - name: POSTGRESQL_MAINTENANCE_WORK_MEM
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRESQL_MAINTENANCE_WORK_MEM
        - name: POSTGRESQL_LOG_MIN_DURATION_STATEMENT
          valueFrom:
            configMapKeyRef:
              name: postgresql-bill-inquiry-config
              key: POSTGRESQL_LOG_MIN_DURATION_STATEMENT
        ports:
        - name: postgresql
          containerPort: 5432
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 1000m
            memory: 2Gi
        volumeMounts:
        - name: postgresql-data
          mountPath: /bitnami/postgresql
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U postgres -h 127.0.0.1 -p 5432
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 6
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U postgres -h 127.0.0.1 -p 5432
          initialDelaySeconds: 5
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 6
      volumes:
      - name: postgresql-data
        persistentVolumeClaim:
          claimName: postgresql-bill-inquiry-pvc
```

### 5.5 Service
```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgresql-bill-inquiry
  namespace: phonebill-dev
  labels:
    app: postgresql-bill-inquiry
    tier: database
spec:
  type: ClusterIP
  ports:
  - name: postgresql
    port: 5432
    targetPort: 5432
    protocol: TCP
  selector:
    app: postgresql-bill-inquiry
```

## 6. 스키마 초기화

### 6.1 초기화 Job
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: bill-inquiry-db-init
  namespace: phonebill-dev
spec:
  template:
    spec:
      containers:
      - name: db-init
        image: bitnami/postgresql:14
        env:
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-bill-inquiry-secret
              key: postgres-password
        command: ["/bin/bash"]
        args:
        - -c
        - |
          echo "스키마 초기화 시작..."
          
          # 연결 대기
          until pg_isready -h postgresql-bill-inquiry -p 5432 -U postgres; do
            echo "PostgreSQL 서버 대기 중..."
            sleep 2
          done
          
          echo "스키마 생성 중..."
          psql -h postgresql-bill-inquiry -U postgres -d bill_inquiry_db << 'EOF'
          
          -- 타임존 설정
          SET timezone = 'Asia/Seoul';
          
          -- 확장 모듈 활성화
          CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
          CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
          
          -- 테이블 생성 (bill-inquiry-schema.psql 내용)
          -- 고객정보 테이블 생성
          CREATE TABLE IF NOT EXISTS customer_info (
              customer_id VARCHAR(50) NOT NULL,
              line_number VARCHAR(20) NOT NULL,
              customer_name VARCHAR(100),
              status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
              operator_code VARCHAR(10) NOT NULL,
              cached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              expires_at TIMESTAMP NOT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              CONSTRAINT pk_customer_info PRIMARY KEY (customer_id),
              CONSTRAINT uk_customer_info_line UNIQUE (line_number),
              CONSTRAINT ck_customer_info_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
          );
          
          -- 기타 테이블들은 전체 스키마 파일에서 가져와 적용
          
          -- 기본 시스템 설정 데이터 삽입
          INSERT INTO system_config (config_key, config_value, description, config_type) VALUES
          ('bill.cache.ttl.hours', '4', '요금정보 캐시 TTL (시간)', 'INTEGER'),
          ('kos.connection.timeout.ms', '30000', 'KOS 연결 타임아웃 (밀리초)', 'INTEGER'),
          ('kos.retry.max.attempts', '3', 'KOS 최대 재시도 횟수', 'INTEGER')
          ON CONFLICT (config_key) DO NOTHING;
          
          SELECT 'Bill-Inquiry Database 초기화 완료' AS result;
          
          EOF
          
          echo "스키마 초기화 완료"
        volumeMounts:
        - name: schema-script
          mountPath: /scripts
      volumes:
      - name: schema-script
        configMap:
          name: bill-inquiry-schema-script
      restartPolicy: OnFailure
```

### 6.2 스키마 스크립트 ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: bill-inquiry-schema-script
  namespace: phonebill-dev
data:
  init-schema.sql: |
    -- bill-inquiry-schema.psql 파일의 전체 내용을 여기에 포함
```

## 7. 설치 절차

### 7.1 사전 준비
1. **AKS 클러스터 확인**
   ```bash
   kubectl config current-context
   kubectl get nodes
   ```

2. **네임스페이스 생성**
   ```bash
   kubectl create namespace phonebill-dev
   kubectl config set-context --current --namespace=phonebill-dev
   ```

3. **스토리지 클래스 확인**
   ```bash
   kubectl get storageclass
   ```

### 7.2 설치 순서
1. **ConfigMap 및 Secret 생성**
   ```bash
   kubectl apply -f postgresql-bill-inquiry-config.yaml
   kubectl apply -f postgresql-bill-inquiry-secret.yaml
   ```

2. **PersistentVolumeClaim 생성**
   ```bash
   kubectl apply -f postgresql-bill-inquiry-pvc.yaml
   kubectl get pvc
   ```

3. **StatefulSet 배포**
   ```bash
   kubectl apply -f postgresql-bill-inquiry-statefulset.yaml
   kubectl get statefulset
   kubectl get pods -w
   ```

4. **Service 생성**
   ```bash
   kubectl apply -f postgresql-bill-inquiry-service.yaml
   kubectl get service
   ```

5. **스키마 초기화**
   ```bash
   kubectl apply -f bill-inquiry-schema-configmap.yaml
   kubectl apply -f bill-inquiry-db-init-job.yaml
   kubectl logs -f job/bill-inquiry-db-init
   ```

### 7.3 설치 검증
1. **Pod 상태 확인**
   ```bash
   kubectl get pods -l app=postgresql-bill-inquiry
   kubectl describe pod postgresql-bill-inquiry-0
   ```

2. **데이터베이스 접속 테스트**
   ```bash
   kubectl exec -it postgresql-bill-inquiry-0 -- psql -U postgres -d bill_inquiry_db
   ```

3. **테이블 생성 확인**
   ```sql
   \dt
   SELECT COUNT(*) FROM system_config;
   SELECT config_key, config_value FROM system_config LIMIT 5;
   ```

4. **서비스 연결 테스트**
   ```bash
   kubectl run test-client --rm -it --image=postgres:14 --restart=Never -- psql -h postgresql-bill-inquiry.phonebill-dev.svc.cluster.local -U postgres -d bill_inquiry_db
   ```

## 8. 모니터링 및 관리

### 8.1 모니터링 메트릭
| 메트릭 | 임계값 | 설명 |
|--------|---------|------|
| CPU 사용률 | < 80% | 정상 동작 범위 |
| Memory 사용률 | < 85% | 메모리 부족 방지 |
| Disk 사용률 | < 80% | 스토리지 여유공간 |
| Connection 수 | < 80 | 최대 연결 수 100의 80% |
| 평균 응답시간 | < 100ms | 쿼리 성능 모니터링 |

### 8.2 로그 관리
```bash
# PostgreSQL 로그 확인
kubectl logs postgresql-bill-inquiry-0

# 실시간 로그 모니터링
kubectl logs -f postgresql-bill-inquiry-0

# 로그 검색
kubectl logs postgresql-bill-inquiry-0 | grep ERROR
```

### 8.3 백업 및 복구
1. **수동 백업**
   ```bash
   kubectl exec postgresql-bill-inquiry-0 -- pg_dump -U postgres bill_inquiry_db > bill_inquiry_backup_$(date +%Y%m%d).sql
   ```

2. **Azure Disk Snapshot**
   ```bash
   # PVC에 바인딩된 Disk 확인
   kubectl get pv
   
   # Azure CLI로 스냅샷 생성
   az snapshot create \
     --resource-group phonebill-dev-rg \
     --name bill-inquiry-db-snapshot-$(date +%Y%m%d) \
     --source {DISK_ID}
   ```

## 9. 트러블슈팅

### 9.1 일반적인 문제
| 문제 | 원인 | 해결방안 |
|------|------|----------|
| Pod Pending | 리소스 부족 | 노드 리소스 확인, requests 조정 |
| Connection Failed | Service 설정 오류 | Service 및 Endpoint 확인 |
| Init 실패 | 스키마 오류 | 스키마 파일 문법 검사 |
| 성능 저하 | 설정 부적절 | PostgreSQL 튜닝 적용 |

### 9.2 문제 해결 절차
```bash
# 1. Pod 상태 확인
kubectl get pods -l app=postgresql-bill-inquiry
kubectl describe pod postgresql-bill-inquiry-0

# 2. 로그 확인
kubectl logs postgresql-bill-inquiry-0 --tail=100

# 3. 서비스 확인
kubectl get service postgresql-bill-inquiry
kubectl get endpoints postgresql-bill-inquiry

# 4. PVC 상태 확인
kubectl get pvc postgresql-bill-inquiry-pvc
kubectl describe pvc postgresql-bill-inquiry-pvc

# 5. ConfigMap/Secret 확인
kubectl get configmap postgresql-bill-inquiry-config -o yaml
kubectl get secret postgresql-bill-inquiry-secret -o yaml
```

## 10. 보안 고려사항

### 10.1 접근 제어
- **Network Policy**: 클러스터 내부 접근만 허용
- **RBAC**: 최소 권한 원칙 적용
- **Secret 관리**: 비밀번호 암호화 저장

### 10.2 데이터 보호
- **암호화**: 전송 구간 TLS 적용
- **백업 암호화**: 백업 데이터 암호화
- **접근 로그**: 모든 접근 기록 유지

## 11. 운영 가이드

### 11.1 정기 작업
- **주간**: 백업 상태 확인 및 복구 테스트
- **월간**: 성능 메트릭 분석 및 튜닝
- **분기**: 보안 패치 및 업그레이드 검토

### 11.2 비상 대응
1. **서비스 중단 시**
   - Pod 재시작: `kubectl rollout restart statefulset/postgresql-bill-inquiry`
   - 백업으로부터 복구
   - 새로운 PVC 생성 후 데이터 이전

2. **성능 문제 시**
   - 리소스 확장: CPU/Memory limits 증가
   - 설정 튜닝: PostgreSQL 파라미터 최적화
   - 인덱스 재구성: 슬로우 쿼리 최적화

## 12. 비용 최적화

### 12.1 리소스 최적화
- **Storage**: Standard HDD 사용으로 비용 절약
- **CPU/Memory**: 개발환경 최적화된 사이징
- **백업**: Azure Disk Snapshot 활용으로 저비용

### 12.2 예상 비용 (월간)
| 항목 | 비용 (USD) | 설명 |
|------|-----------|------|
| Storage (20GB Standard) | $2 | Azure Disk Standard HDD |
| 컴퓨팅 리소스 | $0 | AKS 노드 내 리소스 활용 |
| 백업 스토리지 | $1 | Snapshot 저장 비용 |
| **총 비용** | **$3** | **월간 예상 비용** |

---

**작성일**: 2025-09-08  
**작성자**: 백엔더 (이개발)  
**검토자**: 아키텍트 (김기획), 데옵스 (최운영)  
**승인자**: 기획자 (김기획)