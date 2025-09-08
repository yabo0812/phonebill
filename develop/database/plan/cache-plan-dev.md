# Redis 캐시 설치 계획서 - 개발환경

## 1. 개요

### 1.1 설치 목적
- 통신요금 관리 서비스의 **개발환경** Redis 캐시 구축
- 모든 마이크로서비스 공유 캐시 서버 운영
- 성능 최적화 및 외부 시스템 호출 최소화
- KOS 시스템 연동 데이터 캐싱

### 1.2 참조 문서
- 물리아키텍처설계서: design/backend/physical/physical-architecture-dev.md
- 데이터설계서: design/backend/database/data-design-summary.md
- 백킹서비스설치방법: claude/backing-service-method.md

### 1.3 설계 원칙
- **개발 편의성**: 단순 구성, 빠른 배포
- **비용 효율성**: 메모리 전용 설정
- **성능 우선**: 모든 서비스 공유 캐시
- **단순성**: 복잡한 클러스터링 없음

## 2. 시스템 요구사항

### 2.1 환경 정보
- **환경**: Azure Kubernetes Service (AKS) 개발환경
- **네임스페이스**: phonebill-dev
- **클러스터**: phonebill-dev-aks
- **리소스 그룹**: phonebill-dev-rg
- **Azure 리전**: Korea Central

### 2.2 기술 스택
| 구성요소 | 버전/사양 | 용도 |
|----------|-----------|------|
| Redis | 7.2 | 메인 캐시 엔진 |
| Container Image | bitnami/redis:7.2 | 안정화된 Redis 이미지 |
| 배포 방식 | StatefulSet | 데이터 일관성 보장 |
| 스토리지 | 없음 (Memory Only) | 개발용 임시 데이터 |
| 네트워크 | ClusterIP + NodePort | 내부/외부 접근 지원 |

### 2.3 리소스 할당
| 리소스 유형 | 최소 요구사항 | 최대 제한 | 비고 |
|-------------|---------------|-----------|------|
| CPU | 100m | 500m | 개발환경 최적화 |
| Memory | 256Mi | 1Gi | 캐시 크기 제한 |
| 최대 메모리 | 512MB | - | 메모리 정책 적용 |
| 스토리지 | 없음 | - | 메모리 전용 |

## 3. 아키텍처 설계

### 3.1 Redis 서비스 구성

```
┌─────────────────────────────────────────────────────────────┐
│                    AKS Cluster (phonebill-dev)               │
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐                  │
│  │   Auth Service  │    │Bill-Inquiry Svc│                  │
│  │   (Port: 8080)  │    │   (Port: 8080)  │                  │
│  └─────────┬───────┘    └─────────┬───────┘                  │
│            │                      │                          │
│            │          ┌─────────────────┐                    │
│            │          │Product-Change   │                    │
│            │          │Service          │                    │
│            │          │   (Port: 8080)  │                    │
│            │          └─────────┬───────┘                    │
│            │                      │                          │
│            └──────────┬───────────┘                          │
│                       │                                      │
│              ┌─────────────────┐                             │
│              │  Redis Cache    │                             │
│              │                 │                             │
│              │ • Memory Only   │                             │
│              │ • Port: 6379    │                             │
│              │ • Password Auth │                             │
│              │ • LRU Policy    │                             │
│              └─────────────────┘                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 캐시 키 전략

#### 3.2.1 키 네이밍 규칙
```yaml
키_패턴:
  고객정보: "customer:{lineNumber}"
  상품정보: "product:{productCode}"
  세션정보: "session:{userId}:{sessionId}"
  권한정보: "permissions:{userId}"
  가용상품: "available_products:{customerType}"
  회선상태: "line_status:{lineNumber}"
  KOS응답: "kos_response:{requestId}"
```

#### 3.2.2 TTL 정책
| 캐시 유형 | TTL | 용도 | 갱신 전략 |
|-----------|-----|------|-----------|
| 고객정보 | 4시간 | 고객 기본정보 | 정보 변경시 즉시 무효화 |
| 상품정보 | 2시간 | 상품 목록/상세 | 상품 업데이트시 무효화 |
| 세션정보 | 24시간 | 사용자 세션 | 로그아웃시 삭제 |
| 권한정보 | 8시간 | 사용자 권한 | 권한 변경시 무효화 |
| 가용상품목록 | 24시간 | 변경 가능 상품 | 일 1회 갱신 |
| 회선상태 | 30분 | 실시간 회선정보 | 상태 변경시 갱신 |

### 3.3 메모리 관리 전략
```yaml
메모리_설정:
  최대_메모리: "512MB"
  정책: "allkeys-lru"  # 가장 오래된 키 제거
  기본_TTL: "30분"      # 명시되지 않은 키의 기본 만료시간
  
메모리_분배:
  세션정보: 40%  (204MB)
  고객정보: 30%  (154MB)
  상품정보: 20%  (102MB)
  기타: 10%      (52MB)
```

## 4. 설치 구성

### 4.1 Namespace 생성
```bash
# Namespace 생성
kubectl create namespace phonebill-dev

# Namespace 이동
kubectl config set-context --current --namespace=phonebill-dev
```

### 4.2 Secret 생성
```bash
# Redis 인증 정보 생성
kubectl create secret generic redis-secret \
  --from-literal=redis-password="Hi5Jessica!" \
  --namespace=phonebill-dev
```

### 4.3 ConfigMap 생성
```yaml
# redis-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
  namespace: phonebill-dev
data:
  redis.conf: |
    # Redis 7.2 개발환경 설정
    
    # 메모리 설정
    maxmemory 512mb
    maxmemory-policy allkeys-lru
    
    # 네트워크 설정
    bind 0.0.0.0
    port 6379
    tcp-keepalive 300
    timeout 30
    
    # 보안 설정 (Secret에서 주입)
    # requirepass 는 StatefulSet에서 env로 설정
    
    # 로그 설정
    loglevel notice
    logfile ""
    
    # 개발환경 설정 (데이터 지속성 없음)
    save ""
    appendonly no
    
    # 클라이언트 설정
    maxclients 100
    
    # 기타 최적화 설정
    tcp-backlog 511
    databases 16
    
    # 메모리 사용 최적화
    hash-max-ziplist-entries 512
    hash-max-ziplist-value 64
    list-max-ziplist-size -2
    set-max-intset-entries 512
    zset-max-ziplist-entries 128
    zset-max-ziplist-value 64
```

### 4.4 StatefulSet 매니페스트
```yaml
# redis-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: phonebill-dev
  labels:
    app: redis
    tier: cache
spec:
  serviceName: redis
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
        tier: cache
    spec:
      containers:
      - name: redis
        image: bitnami/redis:7.2
        imagePullPolicy: IfNotPresent
        
        # 환경 변수
        env:
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: redis-password
        - name: REDIS_DISABLE_COMMANDS
          value: "FLUSHALL"  # 개발 중 실수 방지
        
        # 포트 설정
        ports:
        - name: redis
          containerPort: 6379
          protocol: TCP
        
        # 리소스 제한
        resources:
          requests:
            cpu: 100m
            memory: 256Mi
          limits:
            cpu: 500m
            memory: 1Gi
        
        # Health Check
        livenessProbe:
          tcpSocket:
            port: redis
          initialDelaySeconds: 30
          timeoutSeconds: 5
          periodSeconds: 10
          failureThreshold: 3
          
        readinessProbe:
          exec:
            command:
            - /bin/bash
            - -c
            - redis-cli -a "$REDIS_PASSWORD" ping | grep -q PONG
          initialDelaySeconds: 5
          timeoutSeconds: 5
          periodSeconds: 5
          successThreshold: 1
          failureThreshold: 3
        
        # 볼륨 마운트
        volumeMounts:
        - name: redis-config
          mountPath: /opt/bitnami/redis/etc/redis.conf
          subPath: redis.conf
          readOnly: true
        
      # 볼륨 정의
      volumes:
      - name: redis-config
        configMap:
          name: redis-config
          
      # 보안 컨텍스트
      securityContext:
        fsGroup: 1001
        runAsUser: 1001
        runAsNonRoot: true
        
      # Pod 안정성 설정
      restartPolicy: Always
      terminationGracePeriodSeconds: 30
```

### 4.5 Service 매니페스트
```yaml
# redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: phonebill-dev
  labels:
    app: redis
    tier: cache
spec:
  type: ClusterIP
  selector:
    app: redis
  ports:
  - name: redis
    port: 6379
    targetPort: redis
    protocol: TCP
    
---
# 개발용 외부 접근 Service
apiVersion: v1
kind: Service
metadata:
  name: redis-external
  namespace: phonebill-dev
  labels:
    app: redis
    tier: cache
spec:
  type: NodePort
  selector:
    app: redis
  ports:
  - name: redis
    port: 6379
    targetPort: redis
    nodePort: 30679
    protocol: TCP
```

## 5. 배포 절차

### 5.1 사전 준비사항
```bash
# 1. AKS 클러스터 연결 확인
kubectl config current-context

# 2. 필요한 권한 확인
kubectl auth can-i create statefulsets --namespace phonebill-dev

# 3. 네임스페이스 확인
kubectl get namespaces | grep phonebill-dev
```

### 5.2 배포 순서
```bash
# 1. Namespace 생성
kubectl create namespace phonebill-dev

# 2. Secret 생성
kubectl create secret generic redis-secret \
  --from-literal=redis-password="Hi5Jessica!" \
  --namespace=phonebill-dev

# 3. ConfigMap 적용
kubectl apply -f redis-config.yaml

# 4. StatefulSet 배포
kubectl apply -f redis-statefulset.yaml

# 5. Service 생성
kubectl apply -f redis-service.yaml

# 6. 배포 상태 확인
kubectl get pods -l app=redis -n phonebill-dev -w
```

### 5.3 배포 검증
```bash
# 1. Pod 상태 확인
kubectl get pods -l app=redis -n phonebill-dev
kubectl describe pod redis-0 -n phonebill-dev

# 2. Service 확인
kubectl get services -l app=redis -n phonebill-dev
kubectl describe service redis -n phonebill-dev

# 3. Redis 연결 테스트
kubectl exec -it redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! ping

# 4. 설정 확인
kubectl exec -it redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info memory
```

## 6. 애플리케이션 연동 설정

### 6.1 Spring Boot 애플리케이션 설정
```yaml
# application-dev.yml
spring:
  redis:
    host: redis.phonebill-dev.svc.cluster.local
    port: 6379
    password: ${REDIS_PASSWORD:Hi5Jessica!}
    timeout: 2000ms
    jedis:
      pool:
        max-active: 20
        max-wait: 1000ms
        max-idle: 10
        min-idle: 2
        
# 캐시 설정
cache:
  redis:
    time-to-live: 1800  # 30분 기본 TTL
    key-prefix: "phonebill:dev:"
    enable-statistics: true
```

### 6.2 환경별 캐시 키 설정
```yaml
# 개발환경 캐시 키 설정
cache:
  keys:
    customer: "dev:customer:{lineNumber}"
    product: "dev:product:{productCode}" 
    session: "dev:session:{userId}:{sessionId}"
    permissions: "dev:permissions:{userId}"
    available-products: "dev:available_products:{customerType}"
    line-status: "dev:line_status:{lineNumber}"
    kos-response: "dev:kos_response:{requestId}"
```

### 6.3 서비스별 캐시 설정
```java
// Auth Service 캐시 설정
@Configuration
public class AuthCacheConfig {
    
    @Bean
    public RedisCacheManager authCacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(8))  // 권한정보 8시간
            .prefixKeysWith("dev:auth:");
        
        return RedisCacheManager.builder()
            .redisCacheConfiguration(config)
            .build();
    }
}

// Bill-Inquiry Service 캐시 설정  
@Configuration
public class BillCacheConfig {
    
    @Bean 
    public RedisCacheManager billCacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(4))  // 고객정보 4시간
            .prefixKeysWith("dev:bill:");
            
        return RedisCacheManager.builder()
            .redisCacheConfiguration(config)
            .build();
    }
}

// Product-Change Service 캐시 설정
@Configuration  
public class ProductCacheConfig {
    
    @Bean
    public RedisCacheManager productCacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(2))  // 상품정보 2시간
            .prefixKeysWith("dev:product:");
            
        return RedisCacheManager.builder()
            .redisCacheConfiguration(config)
            .build();
    }
}
```

## 7. 모니터링 설정

### 7.1 Redis 메트릭 수집
```yaml
# redis-metrics.yaml
apiVersion: v1
kind: Service
metadata:
  name: redis-metrics
  namespace: phonebill-dev
  labels:
    app: redis
    metrics: "true"
spec:
  selector:
    app: redis
  ports:
  - name: metrics
    port: 9121
    targetPort: 9121
    
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-exporter
  namespace: phonebill-dev
spec:
  selector:
    matchLabels:
      app: redis-exporter
  template:
    metadata:
      labels:
        app: redis-exporter
    spec:
      containers:
      - name: redis-exporter
        image: oliver006/redis_exporter:latest
        env:
        - name: REDIS_ADDR
          value: "redis://redis:6379"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: redis-password
        ports:
        - containerPort: 9121
          name: metrics
        resources:
          requests:
            cpu: 50m
            memory: 64Mi
          limits:
            cpu: 200m
            memory: 128Mi
```

### 7.2 주요 모니터링 지표
| 지표 | 임계값 | 액션 |
|------|--------|------|
| 메모리 사용률 | > 80% | 캐시 정리, 메모리 증설 검토 |
| 연결 수 | > 80 | 연결 풀 최적화 |
| Hit Rate | < 80% | 캐시 전략 재검토 |
| Evicted Keys | > 1000/min | TTL 정책 조정 |
| 응답 시간 | > 10ms | 성능 최적화 |

### 7.3 로그 모니터링
```bash
# Redis 로그 실시간 모니터링
kubectl logs -f redis-0 -n phonebill-dev

# 메모리 사용량 모니터링
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info memory

# 키 통계 모니터링
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info keyspace

# 클라이언트 연결 상태
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! client list
```

## 8. 백업 및 복구

### 8.1 백업 전략
```yaml
백업_정책:
  방식: "메모리 전용으로 백업 없음"
  복구_방법: "Pod 재시작시 캐시 재구성"
  데이터_손실: "허용 (개발환경)"
  
비상_계획:
  - Pod 장애시 자동 재시작
  - 애플리케이션에서 캐시 미스시 DB 조회
  - 캐시 warm-up 스크립트 실행
```

### 8.2 캐시 Warm-up 스크립트
```bash
#!/bin/bash
# cache-warmup.sh - Redis 재시작 후 주요 데이터 캐싱

REDIS_HOST="redis.phonebill-dev.svc.cluster.local"
REDIS_PORT="6379"
REDIS_PASSWORD="Hi5Jessica!"

# 기본 상품 정보 캐싱
echo "상품 정보 캐싱 시작..."
curl -X POST "http://auth-service:8080/api/cache/warmup/products"

# 공통 코드 캐싱
echo "공통 코드 캐싱 시작..."
curl -X POST "http://auth-service:8080/api/cache/warmup/codes"

# 시스템 설정 캐싱
echo "시스템 설정 캐싱 시작..."
curl -X POST "http://bill-inquiry-service:8080/api/cache/warmup/config"

echo "캐시 Warm-up 완료"
```

## 9. 보안 설정

### 9.1 네트워크 보안
```yaml
# redis-network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: redis-network-policy
  namespace: phonebill-dev
spec:
  podSelector:
    matchLabels:
      app: redis
  policyTypes:
  - Ingress
  ingress:
  # 애플리케이션 서비스에서만 접근 허용
  - from:
    - podSelector:
        matchLabels:
          tier: application
    ports:
    - protocol: TCP
      port: 6379
  # 모니터링을 위한 접근 허용
  - from:
    - podSelector:
        matchLabels:
          app: redis-exporter
    ports:
    - protocol: TCP
      port: 6379
```

### 9.2 보안 체크리스트
```yaml
보안_체크리스트:
  ✓ 인증: "Redis 패스워드 인증 활성화"
  ✓ 네트워크: "ClusterIP로 내부 접근만 허용"
  ✓ 권한: "비루트 사용자로 실행"
  ✓ 명령어: "위험한 명령어 비활성화 (FLUSHALL)"
  ✓ 로그: "접근 로그 기록"
  ✗ 암호화: "개발환경에서 TLS 미적용"
  ✗ 방화벽: "기본 보안 그룹 사용"
```

## 10. 운영 가이드

### 10.1 일상 운영 작업
```bash
# Redis 상태 확인
kubectl get pods -l app=redis -n phonebill-dev
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info server

# 메모리 사용량 확인
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info memory

# 키 현황 확인
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info keyspace

# 슬로우 로그 확인
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! slowlog get 10
```

### 10.2 캐시 관리 작업
```bash
# 특정 패턴 키 삭제
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! --scan --pattern "dev:customer:*" | xargs -I {} redis-cli -a Hi5Jessica! del {}

# 캐시 통계 확인  
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! info stats

# 클라이언트 연결 확인
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! client list

# TTL 확인
kubectl exec redis-0 -n phonebill-dev -- redis-cli -a Hi5Jessica! ttl "dev:customer:010-1234-5678"
```

### 10.3 트러블슈팅 가이드
| 문제 | 원인 | 해결방안 |
|------|------|----------|
| Pod 시작 실패 | 리소스 부족 | 노드 리소스 확인, 메모리 제한 조정 |
| 연결 실패 | 네트워크/인증 문제 | 패스워드, 포트, 네트워크 정책 확인 |
| 메모리 부족 | 캐시 데이터 과다 | TTL 정책 조정, 메모리 증설 |
| 성능 저하 | 키 집중, 메모리 스왑 | 키 분산, 메모리 최적화 |
| 캐시 미스 증가 | TTL 정책, 데이터 변경 | TTL 조정, 무효화 전략 검토 |

## 11. 비용 최적화

### 11.1 개발환경 비용 구조
| 리소스 | 할당량 | 월간 예상 비용 | 절약 방안 |
|--------|---------|----------------|-----------|
| CPU | 100m-500m | $3 | requests 최소화 |
| Memory | 256Mi-1Gi | $5 | 메모리 전용으로 스토리지 비용 없음 |
| 네트워킹 | ClusterIP | $0 | 내부 통신만 사용 |
| **총합** | | **$8** | **스토리지 제거로 비용 최소화** |

### 11.2 비용 절약 전략
```yaml
절약_방안:
  - 스토리지_제거: "메모리 전용으로 스토리지 비용 제거"
  - 리소스_최적화: "requests를 최소한으로 설정"  
  - 자동_스케일링_비활성화: "개발환경에서 고정 리소스"
  - 야간_스케일다운: "비업무시간 리소스 축소"
```

## 12. 성능 튜닝 가이드

### 12.1 성능 목표
| 지표 | 목표값 | 측정 방법 |
|------|---------|-----------|
| 응답 시간 | < 5ms | Redis PING |
| 처리량 | > 1000 ops/sec | Redis INFO stats |
| 메모리 효율성 | > 90% hit rate | Redis INFO keyspace |
| 연결 처리 | < 100 concurrent | Redis CLIENT LIST |

### 12.2 튜닝 매개변수
```yaml
# redis.conf 최적화
성능_튜닝:
  # 네트워크 최적화
  tcp-keepalive: 300
  tcp-backlog: 511
  timeout: 30
  
  # 메모리 최적화  
  maxmemory-policy: "allkeys-lru"
  hash-max-ziplist-entries: 512
  list-max-ziplist-size: -2
  
  # 클라이언트 최적화
  maxclients: 100
  databases: 16
```

## 13. 마이그레이션 계획

### 13.1 운영환경 이관 준비
```yaml
운영환경_차이점:
  - 고가용성: "Master-Slave 구성"
  - 데이터_지속성: "RDB + AOF 활성화"
  - 보안_강화: "TLS 암호화, 네트워크 정책"
  - 리소스_증설: "CPU 1-2 core, Memory 4-8GB"
  - 모니터링_강화: "Prometheus, Grafana 연동"
  - 백업_정책: "자동 백업, 복구 절차"
```

### 13.2 설정 호환성
```yaml
호환성_체크:
  ✓ Redis_버전: "7.2 동일"
  ✓ 데이터_구조: "키 패턴 호환"
  ✓ 애플리케이션_설정: "연결 정보만 변경"
  ✓ 모니터링_지표: "동일한 메트릭 수집"
  ⚠ TTL_정책: "운영환경에서 조정 필요"
  ⚠ 메모리_정책: "운영환경 특성에 맞게 조정"
```

## 14. 완료 체크리스트

### 14.1 설치 검증 항목
```yaml
필수_검증_항목:
  □ Redis Pod 정상 실행
  □ Service 연결 가능  
  □ 패스워드 인증 동작
  □ 메모리 제한 적용
  □ TTL 정책 동작
  □ 애플리케이션 연동 테스트
  □ 모니터링 메트릭 수집
  □ 네트워크 정책 적용
  □ 캐시 성능 테스트
  □ 장애 복구 테스트
```

### 14.2 운영 준비 항목  
```yaml
운영_준비_항목:
  □ 운영 매뉴얼 작성
  □ 모니터링 대시보드 구성
  □ 알림 규칙 설정
  □ 백업 및 복구 절차 문서화
  □ 성능 튜닝 가이드 작성
  □ 트러블슈팅 가이드 작성  
  □ 개발팀 교육 완료
  □ 운영팀 인수인계 완료
```

---

**계획서 작성일**: `2025-09-08`  
**작성자**: 데옵스 (최운영), 백엔더 (이개발)  
**검토자**: 아키텍트 (김기획), QA매니저 (정테스트)  
**승인자**: 프로젝트 매니저

**다음 단계**: Redis 캐시 설치 실행 → develop/database/exec/cache-exec-dev.md 작성