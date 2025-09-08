# Product-Change 서비스 개발환경 데이터베이스 설치 계획서

## 1. 개요

### 1.1 설치 목적
- Product-Change 서비스의 개발환경 데이터베이스 구축
- Kubernetes StatefulSet 기반 PostgreSQL 14 배포
- 개발팀 생산성 향상을 위한 최적화된 구성

### 1.2 설계 원칙
- **개발 친화적**: 빠른 개발과 검증을 위한 구성
- **비용 효율적**: 개발환경에 최적화된 리소스 할당
- **단순성**: 복잡한 설정 최소화, 운영 부담 경감
- **가용성**: 95% 가용성 목표 (개발환경 허용 수준)

### 1.3 참조 문서
- 물리아키텍처: `design/backend/physical/physical-architecture-dev.md`
- 데이터 설계서: `design/backend/database/product-change.md`
- 스키마 스크립트: `design/backend/database/product-change-schema.psql`
- 데이터 설계 종합: `design/backend/database/data-design-summary.md`

## 2. 환경 구성 정보

### 2.1 인프라 환경
| 구성 요소 | 값 | 설명 |
|----------|----|----|
| 클라우드 | Microsoft Azure | Azure Kubernetes Service |
| 클러스터 | phonebill-dev-aks | 개발환경 AKS 클러스터 |
| 네임스페이스 | phonebill-dev | 개발환경 전용 네임스페이스 |
| 리소스 그룹 | phonebill-dev-rg | 개발환경 리소스 그룹 |

### 2.2 데이터베이스 정보
| 설정 항목 | 값 | 설명 |
|-----------|----|-----|
| 데이터베이스 이름 | product_change_db | Product-Change 서비스 전용 DB |
| 스키마 | product_change | 서비스별 독립 스키마 |
| PostgreSQL 버전 | 14 | 안정화된 최신 버전 |
| 캐릭터셋 | UTF-8 | 다국어 지원 |
| 타임존 | UTC | 글로벌 표준 시간 |

## 3. 리소스 할당 계획

### 3.1 컴퓨팅 리소스
| 리소스 유형 | 요청량 (Requests) | 제한량 (Limits) | 설명 |
|-------------|------------------|----------------|------|
| CPU | 500m | 1000m | 0.5코어 요청, 1코어 최대 |
| Memory | 1Gi | 2Gi | 1GB 요청, 2GB 최대 |
| Replicas | 1 | 1 | 개발환경 단일 인스턴스 |

### 3.2 스토리지 구성
| 스토리지 유형 | 크기 | 클래스 | 용도 |
|-------------|-----|-------|------|
| 데이터 볼륨 | 20Gi | managed-standard | PostgreSQL 데이터 저장 |
| 백업 볼륨 | 10Gi | managed-standard | 백업 파일 저장 |
| 성능 | Standard HDD | Azure Disk | 개발환경 적합 성능 |

### 3.3 네트워크 구성
| 네트워크 설정 | 값 | 설명 |
|--------------|----|----|
| 서비스 타입 | ClusterIP | 클러스터 내부 접근 |
| 포트 | 5432 | PostgreSQL 기본 포트 |
| DNS 이름 | postgresql-product-change.phonebill-dev.svc.cluster.local | 서비스 디스커버리 |

## 4. PostgreSQL 설정

### 4.1 데이터베이스 설정
| 설정 항목 | 값 | 설명 |
|-----------|----|-----|
| max_connections | 100 | 최대 동시 연결 수 |
| shared_buffers | 256MB | 공유 버퍼 메모리 |
| effective_cache_size | 1GB | 효과적 캐시 크기 |
| work_mem | 4MB | 작업 메모리 |
| maintenance_work_mem | 64MB | 유지보수 작업 메모리 |
| checkpoint_completion_target | 0.7 | 체크포인트 완료 목표 |
| wal_buffers | 16MB | WAL 버퍼 크기 |
| default_statistics_target | 100 | 통계 정보 수집 대상 |

### 4.2 로그 설정
| 로그 설정 | 값 | 설명 |
|-----------|----|----|
| log_destination | 'stderr' | 표준 에러 출력 |
| logging_collector | on | 로그 수집 활성화 |
| log_directory | 'log' | 로그 디렉터리 |
| log_filename | 'postgresql-%Y-%m-%d_%H%M%S.log' | 로그 파일명 패턴 |
| log_min_duration_statement | 1000 | 1초 이상 쿼리 로깅 |
| log_checkpoints | on | 체크포인트 로깅 |
| log_connections | on | 연결 로깅 |
| log_disconnections | on | 연결 해제 로깅 |

## 5. Kubernetes 매니페스트

### 5.1 StatefulSet 구성
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql-product-change
  namespace: phonebill-dev
  labels:
    app: postgresql-product-change
    service: product-change
    tier: database
spec:
  serviceName: postgresql-product-change
  replicas: 1
  selector:
    matchLabels:
      app: postgresql-product-change
  template:
    metadata:
      labels:
        app: postgresql-product-change
        service: product-change
        tier: database
    spec:
      containers:
      - name: postgresql
        image: bitnami/postgresql:14
        ports:
        - containerPort: 5432
          name: postgresql
        env:
        - name: POSTGRES_DB
          value: "product_change_db"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgresql-product-change-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-product-change-secret
              key: password
        - name: PGDATA
          value: "/bitnami/postgresql/data"
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
        - name: postgresql-config
          mountPath: /opt/bitnami/postgresql/conf/conf.d
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" -h 127.0.0.1 -p 5432
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - -e
            - |
              exec pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" -h 127.0.0.1 -p 5432
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
      volumes:
      - name: postgresql-config
        configMap:
          name: postgresql-product-change-config
  volumeClaimTemplates:
  - metadata:
      name: postgresql-data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: managed-standard
      resources:
        requests:
          storage: 20Gi
```

### 5.2 Service 구성
```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgresql-product-change
  namespace: phonebill-dev
  labels:
    app: postgresql-product-change
    service: product-change
    tier: database
spec:
  type: ClusterIP
  ports:
  - port: 5432
    targetPort: 5432
    protocol: TCP
    name: postgresql
  selector:
    app: postgresql-product-change
```

### 5.3 ConfigMap 구성
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-product-change-config
  namespace: phonebill-dev
  labels:
    app: postgresql-product-change
    service: product-change
data:
  postgresql.conf: |
    # Custom PostgreSQL configuration for Product-Change service
    max_connections = 100
    shared_buffers = 256MB
    effective_cache_size = 1GB
    work_mem = 4MB
    maintenance_work_mem = 64MB
    checkpoint_completion_target = 0.7
    wal_buffers = 16MB
    default_statistics_target = 100
    
    # Logging configuration
    log_destination = 'stderr'
    logging_collector = on
    log_directory = 'log'
    log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
    log_min_duration_statement = 1000
    log_checkpoints = on
    log_connections = on
    log_disconnections = on
    
    # Development environment optimizations
    fsync = off
    synchronous_commit = off
    full_page_writes = off
    
    # Timezone setting
    timezone = 'UTC'
    log_timezone = 'UTC'
```

### 5.4 Secret 구성
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-product-change-secret
  namespace: phonebill-dev
  labels:
    app: postgresql-product-change
    service: product-change
type: Opaque
data:
  username: cHJvZHVjdF9jaGFuZ2VfYXBw  # product_change_app (base64)
  password: ZGV2X3Bhc3N3b3JkXzIwMjU=   # dev_password_2025 (base64)
```

## 6. 스키마 적용 계획

### 6.1 스키마 초기화 Job
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: postgresql-product-change-schema-init
  namespace: phonebill-dev
  labels:
    app: postgresql-product-change
    job-type: schema-init
spec:
  template:
    metadata:
      labels:
        app: postgresql-product-change
        job-type: schema-init
    spec:
      restartPolicy: OnFailure
      containers:
      - name: schema-init
        image: bitnami/postgresql:14
        env:
        - name: PGHOST
          value: "postgresql-product-change"
        - name: PGPORT
          value: "5432"
        - name: PGDATABASE
          value: "product_change_db"
        - name: PGUSER
          valueFrom:
            secretKeyRef:
              name: postgresql-product-change-secret
              key: username
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-product-change-secret
              key: password
        command:
        - /bin/bash
        - -c
        - |
          echo "Waiting for PostgreSQL to be ready..."
          until pg_isready -h $PGHOST -p $PGPORT -U $PGUSER; do
            echo "PostgreSQL is not ready - sleeping"
            sleep 2
          done
          
          echo "PostgreSQL is ready - applying schema..."
          psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -f /sql/product-change-schema.sql
          
          echo "Schema initialization completed successfully"
        volumeMounts:
        - name: schema-sql
          mountPath: /sql
      volumes:
      - name: schema-sql
        configMap:
          name: postgresql-product-change-schema
```

### 6.2 스키마 SQL ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-product-change-schema
  namespace: phonebill-dev
  labels:
    app: postgresql-product-change
    config-type: schema
data:
  product-change-schema.sql: |
    # (product-change-schema.psql 파일 내용 포함)
```

## 7. 백업 및 복구 설정

### 7.1 백업 전략
| 백업 유형 | 주기 | 보존 기간 | 방법 |
|-----------|------|----------|------|
| 전체 백업 | 일일 (02:00) | 7일 | pg_dump + Azure Blob Storage |
| WAL 백업 | 실시간 | 7일 | 연속 아카이빙 |
| 스냅샷 백업 | 수동 | 필요시 | Azure Disk Snapshot |

### 7.2 백업 CronJob
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgresql-product-change-backup
  namespace: phonebill-dev
spec:
  schedule: "0 2 * * *"  # 매일 새벽 2시
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
          - name: backup
            image: bitnami/postgresql:14
            env:
            - name: PGHOST
              value: "postgresql-product-change"
            - name: PGUSER
              valueFrom:
                secretKeyRef:
                  name: postgresql-product-change-secret
                  key: username
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgresql-product-change-secret
                  key: password
            command:
            - /bin/bash
            - -c
            - |
              BACKUP_FILE="/backup/product_change_db_$(date +%Y%m%d_%H%M%S).sql"
              pg_dump -h $PGHOST -U $PGUSER product_change_db > $BACKUP_FILE
              echo "Backup completed: $BACKUP_FILE"
              
              # 7일 이전 백업 파일 삭제
              find /backup -name "*.sql" -mtime +7 -delete
            volumeMounts:
            - name: backup-volume
              mountPath: /backup
          volumes:
          - name: backup-volume
            persistentVolumeClaim:
              claimName: postgresql-product-change-backup-pvc
```

## 8. 모니터링 설정

### 8.1 모니터링 지표
| 지표 유형 | 메트릭 | 임계값 | 알람 조건 |
|-----------|--------|--------|-----------|
| 성능 | CPU 사용률 | > 80% | 5분 지속 |
| 성능 | Memory 사용률 | > 85% | 3분 지속 |
| 가용성 | Connection Count | > 80 | 즉시 |
| 디스크 | Storage 사용률 | > 80% | 즉시 |
| 쿼리 | Slow Query | > 5초 | 즉시 |

### 8.2 헬스 체크 구성
| 체크 유형 | 설정 | 값 |
|-----------|------|---|
| Liveness Probe | 초기 지연 | 30초 |
| Liveness Probe | 체크 주기 | 10초 |
| Liveness Probe | 타임아웃 | 5초 |
| Readiness Probe | 초기 지연 | 5초 |
| Readiness Probe | 체크 주기 | 5초 |
| Readiness Probe | 타임아웃 | 3초 |

## 9. 보안 설정

### 9.1 접근 제어
| 보안 요소 | 설정 | 설명 |
|-----------|------|------|
| 사용자 인증 | Password 기반 | 개발환경 단순 인증 |
| 네트워크 정책 | ClusterIP 전용 | 클러스터 내부에서만 접근 |
| TLS 암호화 | 미적용 | 개발환경 성능 우선 |
| 권한 분리 | 애플리케이션/관리자 | 최소 권한 원칙 |

### 9.2 사용자 계정
| 계정 유형 | 사용자명 | 권한 | 용도 |
|-----------|----------|------|------|
| 애플리케이션 | product_change_app | SELECT, INSERT, UPDATE | 서비스 운영 |
| 관리자 | product_change_admin | ALL PRIVILEGES | 스키마 관리 |
| 읽기전용 | product_change_readonly | SELECT | 모니터링, 분석 |

## 10. 설치 절차

### 10.1 사전 준비 사항
1. **AKS 클러스터 준비 확인**
   ```bash
   kubectl get nodes
   kubectl get ns phonebill-dev
   ```

2. **스토리지 클래스 확인**
   ```bash
   kubectl get storageclass managed-standard
   ```

3. **이미지 Pull 권한 확인**
   ```bash
   kubectl auth can-i create pods --namespace=phonebill-dev
   ```

### 10.2 설치 단계
1. **네임스페이스 생성**
   ```bash
   kubectl create namespace phonebill-dev
   ```

2. **Secret 생성**
   ```bash
   kubectl apply -f postgresql-product-change-secret.yaml
   ```

3. **ConfigMap 생성**
   ```bash
   kubectl apply -f postgresql-product-change-config.yaml
   kubectl apply -f postgresql-product-change-schema.yaml
   ```

4. **StatefulSet 배포**
   ```bash
   kubectl apply -f postgresql-product-change-statefulset.yaml
   ```

5. **Service 생성**
   ```bash
   kubectl apply -f postgresql-product-change-service.yaml
   ```

6. **스키마 초기화**
   ```bash
   kubectl apply -f postgresql-product-change-schema-init-job.yaml
   ```

### 10.3 설치 검증
1. **Pod 상태 확인**
   ```bash
   kubectl get pods -n phonebill-dev -l app=postgresql-product-change
   kubectl logs -n phonebill-dev postgresql-product-change-0
   ```

2. **서비스 연결 테스트**
   ```bash
   kubectl exec -it postgresql-product-change-0 -n phonebill-dev -- psql -U product_change_app -d product_change_db -c "SELECT version();"
   ```

3. **스키마 확인**
   ```bash
   kubectl exec -it postgresql-product-change-0 -n phonebill-dev -- psql -U product_change_app -d product_change_db -c "\dt product_change.*"
   ```

## 11. 운영 관리

### 11.1 일상 운영 작업
| 작업 유형 | 주기 | 명령어 | 설명 |
|-----------|------|--------|------|
| 상태 모니터링 | 일일 | `kubectl get pods -n phonebill-dev` | Pod 상태 확인 |
| 로그 확인 | 필요시 | `kubectl logs postgresql-product-change-0 -n phonebill-dev` | 로그 분석 |
| 백업 확인 | 일일 | `kubectl get jobs -n phonebill-dev` | 백업 작업 상태 |
| 디스크 사용량 | 주간 | `kubectl exec -it postgresql-product-change-0 -n phonebill-dev -- df -h` | 스토리지 모니터링 |

### 11.2 트러블슈팅
| 문제 유형 | 원인 | 해결 방법 |
|-----------|------|----------|
| Pod Pending | 리소스 부족 | 노드 스케일업 또는 리소스 조정 |
| Connection Refused | 서비스 미준비 | Readiness Probe 확인, 로그 분석 |
| Slow Query | 인덱스 누락 | 쿼리 플랜 분석, 인덱스 추가 |
| Disk Full | 로그/데이터 증가 | 백업 후 정리, 스토리지 확장 |

## 12. 성능 최적화

### 12.1 개발환경 최적화 설정
| 최적화 항목 | 설정 | 효과 |
|-------------|------|------|
| fsync | off | 30% I/O 성능 향상 |
| synchronous_commit | off | 20% 트랜잭션 성능 향상 |
| full_page_writes | off | 15% WAL 성능 향상 |
| checkpoint_completion_target | 0.7 | I/O 부하 분산 |

### 12.2 리소스 튜닝
| 리소스 | 기본값 | 튜닝값 | 근거 |
|--------|--------|--------|------|
| shared_buffers | 128MB | 256MB | 메모리의 25% 활용 |
| effective_cache_size | 4GB | 1GB | 실제 메모리 반영 |
| work_mem | 1MB | 4MB | 개발환경 동시성 고려 |

## 13. 비용 최적화

### 13.1 개발환경 비용 구성
| 구성 요소 | 사양 | 월간 예상 비용 (USD) |
|-----------|------|---------------------|
| Azure Disk Standard | 20GB | $2.40 |
| Compute (포함) | 1 vCPU, 2GB | AKS 노드 비용에 포함 |
| Backup Storage | 10GB | $0.50 |
| **총합** | | **$2.90** |

### 13.2 비용 절약 전략
- **Standard Disk 사용**: Premium SSD 대비 60% 절약
- **단일 인스턴스**: 고가용성 구성 대비 50% 절약  
- **자동 정리**: 오래된 백업 자동 삭제로 스토리지 비용 절약

## 14. 완료 체크리스트

### 14.1 설치 완료 확인
- [ ] StatefulSet 정상 배포 및 Ready 상태
- [ ] Service 생성 및 Endpoint 연결 확인  
- [ ] Secret, ConfigMap 생성 확인
- [ ] 스키마 초기화 Job 성공 완료
- [ ] 데이터베이스 연결 테스트 통과

### 14.2 기능 검증 완료
- [ ] 테이블 생성 확인 (3개 테이블)
- [ ] 인덱스 생성 확인 (12개 인덱스)
- [ ] 초기 데이터 삽입 확인 (Circuit Breaker 상태)
- [ ] 트리거 함수 동작 확인
- [ ] 모니터링 뷰 생성 확인

### 14.3 운영 준비 완료
- [ ] 백업 CronJob 설정 및 테스트
- [ ] 모니터링 메트릭 수집 확인
- [ ] 로그 정상 출력 확인
- [ ] 헬스 체크 정상 동작 확인
- [ ] 문서화 완료

---

**작성자**: 데옵스 (최운영)  
**검토자**: 백엔더 (이개발), QA매니저 (정테스트)  
**작성일**: 2025-09-08  
**버전**: v1.0

**최운영/데옵스**: Product-Change 서비스용 개발환경 데이터베이스 설치 계획서를 작성했습니다. Kubernetes StatefulSet 기반으로 PostgreSQL 14를 배포하며, 개발팀의 생산성 향상과 비용 효율성을 동시에 고려한 구성으로 설계했습니다.