# 백엔드 서비스 Kubernetes 배포 가이드

## 배포 정보
- **ACR명**: acrdigitalgarage01
- **Kubernetes 클러스터**: aks-digitalgarage-01  
- **네임스페이스**: phonebill-dev
- **파드 수**: 1개
- **리소스 설정**: CPU 256m-1024m, Memory 256Mi-1024Mi

## 배포가이드 검증 결과

### ✅ 체크리스트 검증 완료

1. **객체이름 네이밍룰 준수**
   - 공통 ConfigMap: cm-common ✓
   - 공통 Secret: secret-common ✓
   - 서비스별 ConfigMap: cm-{서비스명} ✓
   - 서비스별 Secret: secret-{서비스명} ✓
   - Ingress: phonebill ✓
   - Service: {서비스명} ✓
   - Deployment: {서비스명} ✓

2. **Redis Host명 ClusterIP 서비스 사용**
   - Redis Host: redis-cache-dev-master (ClusterIP) ✓

3. **Database Host명 ClusterIP 서비스 사용**
   - User Service: auth-postgres-dev-postgresql ✓
   - Bill Service: bill-inquiry-postgres-dev-postgresql ✓
   - Product Service: product-change-postgres-dev-postgresql ✓

4. **Secret에 stringData 사용** ✓

5. **JWT_SECRET openssl 생성** ✓
   - 값: lJZLB9WK5+6q3/Ob4m5MvLUqttA6qq/FPmBXX71PbzE=

6. **매니페스트에 실제 값 지정 (환경변수 미사용)** ✓

7. **Image Pull Secret에 실제 값 지정** ✓
   - Username: acrdigitalgarage01
   - Password: +OY+rmOagorjWvQe/tTk6oqvnZI8SmNbY/Y2o5EDcY+ACRDCDbYk

8. **Ingress Controller External IP 확인 및 반영** ✓
   - External IP: 20.214.196.128
   - Host: phonebill-api.20.214.196.128.nip.io

9. **Ingress와 Service port 일치 (80)** ✓

10. **Ingress path Controller 매핑 정확** ✓
    - /api/v1/auth → user-service
    - /api/v1/users → user-service
    - /api/v1/bills → bill-service
    - /api/v1/products → product-service
    - /api/v1/kos → kos-mock
    - /health → api-gateway

11. **보안 환경변수 Secret 지정** ✓

12. **REDIS_DATABASE 서비스별 다른 값** ✓
    - User Service: 0
    - Bill Service: 1
    - Product Service: 2

13. **envFrom 사용** ✓

14. **실행 프로파일 매핑 완료** ✓
    - 전체 환경변수 매핑 테이블 작성 및 검증 완료

## 사전 확인 방법

### 1. Azure 로그인 상태 확인
```bash
az account show
```

### 2. AKS Credential 확인
```bash
kubectl cluster-info
```

### 3. 네임스페이스 존재 확인
```bash
kubectl get ns phonebill-dev
```

## 매니페스트 적용 가이드

### 1. 공통 매니페스트 적용
```bash
kubectl apply -f deployment/k8s/common/
```

### 2. 서비스별 매니페스트 적용
```bash
# User Service
kubectl apply -f deployment/k8s/user-service/

# Bill Service  
kubectl apply -f deployment/k8s/bill-service/

# Product Service
kubectl apply -f deployment/k8s/product-service/

# API Gateway
kubectl apply -f deployment/k8s/api-gateway/

# KOS Mock
kubectl apply -f deployment/k8s/kos-mock/
```

## 배포 확인 가이드

### 1. Pod 상태 확인
```bash
kubectl get pods -n phonebill-dev
```

### 2. Service 상태 확인
```bash
kubectl get svc -n phonebill-dev
```

### 3. Ingress 상태 확인
```bash
kubectl get ingress -n phonebill-dev
```

### 4. 로그 확인
```bash
# 특정 서비스 로그 확인
kubectl logs -f deployment/user-service -n phonebill-dev
kubectl logs -f deployment/bill-service -n phonebill-dev
kubectl logs -f deployment/product-service -n phonebill-dev
kubectl logs -f deployment/api-gateway -n phonebill-dev
kubectl logs -f deployment/kos-mock -n phonebill-dev
```

### 5. Health Check 확인
```bash
# API Gateway Health Check
curl http://phonebill-api.20.214.196.128.nip.io/health

# 개별 서비스 Health Check  
kubectl exec -n phonebill-dev deployment/user-service -- curl localhost:8081/actuator/health
kubectl exec -n phonebill-dev deployment/bill-service -- curl localhost:8082/actuator/health
kubectl exec -n phonebill-dev deployment/product-service -- curl localhost:8083/actuator/health
kubectl exec -n phonebill-dev deployment/kos-mock -- curl localhost:8084/actuator/health
```

## 주요 접근 URL
- **API Gateway**: http://phonebill-api.20.214.196.128.nip.io/health
- **인증 API**: http://phonebill-api.20.214.196.128.nip.io/api/v1/auth/login
- **사용자 API**: http://phonebill-api.20.214.196.128.nip.io/api/v1/users/profile
- **요금조회 API**: http://phonebill-api.20.214.196.128.nip.io/api/v1/bills/recent
- **상품변경 API**: http://phonebill-api.20.214.196.128.nip.io/api/v1/products/change

## 롤백 가이드
```bash
# 특정 서비스 롤백
kubectl rollout undo deployment/user-service -n phonebill-dev

# 전체 매니페스트 삭제
kubectl delete -f deployment/k8s/user-service/
kubectl delete -f deployment/k8s/bill-service/
kubectl delete -f deployment/k8s/product-service/
kubectl delete -f deployment/k8s/api-gateway/
kubectl delete -f deployment/k8s/kos-mock/
kubectl delete -f deployment/k8s/common/
```

## 트러블슈팅

### 1. Pod 시작 실패 시
```bash
# Pod 이벤트 확인
kubectl describe pod <pod-name> -n phonebill-dev

# 상세 로그 확인
kubectl logs <pod-name> -n phonebill-dev --previous
```

### 2. Database 연결 실패 시
```bash
# Database Service 확인
kubectl get svc -n phonebill-dev | grep postgres

# 연결 테스트
kubectl exec -n phonebill-dev deployment/user-service -- nslookup auth-postgres-dev-postgresql
```

### 3. Redis 연결 실패 시
```bash
# Redis Service 확인
kubectl get svc -n phonebill-dev | grep redis

# 연결 테스트
kubectl exec -n phonebill-dev deployment/user-service -- nslookup redis-cache-dev-master
```