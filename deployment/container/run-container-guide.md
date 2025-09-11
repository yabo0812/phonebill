# 백엔드 컨테이너 실행방법 가이드

## 📋 실행 정보
- **ACR명**: acrdigitalgarage01
- **VM 접속정보**:
  - KEY파일: ~/home/bastion-dg0500
  - USERID: azureuser
  - IP: 4.230.5.6

## 🏗️ 시스템 구성 정보
- **시스템명**: phonebill
- **서비스 목록**:
  - api-gateway (포트: 8080)
  - user-service (포트: 8081)
  - bill-service (포트: 8082)
  - product-service (포트: 8083)
  - kos-mock (포트: 8084)

## 🔌 VM 접속 방법

### 1. 터미널 실행
- **Linux/Mac**: 기본 터미널 실행
- **Windows**: Windows Terminal 실행

### 2. Private Key 파일 권한 설정 (최초 1회만)
```bash
chmod 400 ~/home/bastion-dg0500
```

### 3. VM 접속
```bash
ssh -i ~/home/bastion-dg0500 azureuser@4.230.5.6
```

## 🏗️ 컨테이너 이미지 빌드 및 푸시

### 1. 로컬에서 이미지 빌드
- `deployment/container/build-image.md` 파일의 가이드에 따라 어플리케이션 빌드 및 컨테이너 이미지 생성

### 2. ACR 인증정보 확인
```bash
az acr credential show --name acrdigitalgarage01
```

결과 예시:
```json
{
  "passwords": [
    {
      "name": "password",
      "value": "{암호}"
    }
  ],
  "username": "acrdigitalgarage01"
}
```

### 3. 컨테이너 레지스트리 로그인 (VM에서 수행)
```bash
docker login acrdigitalgarage01.azurecr.io -u acrdigitalgarage01 -p {암호}
```

### 4. 이미지 태그 및 푸시 (로컬에서 수행)
```bash
# 각 서비스별로 수행
docker tag api-gateway:latest acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest

docker tag user-service:latest acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/user-service:latest

docker tag bill-service:latest acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest

docker tag product-service:latest acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/product-service:latest

docker tag kos-mock:latest acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

## 🚀 컨테이너 실행 (VM에서 수행)

### 1. api-gateway 실행
```bash
SERVER_PORT=8080

docker run -d --name api-gateway --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=8080 \
-e SERVER_NETTY_CONNECTION_TIMEOUT=30s \
-e SERVER_NETTY_IDLE_TIMEOUT=60s \
-e SPRING_PROFILES_ACTIVE=dev \
-e JWT_SECRET=your-jwt-secret-key-here \
-e JWT_ACCESS_TOKEN_VALIDITY=180000 \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e USER_SERVICE_URL=http://localhost:8081 \
-e BILL_SERVICE_URL=http://localhost:8082 \
-e PRODUCT_SERVICE_URL=http://localhost:8083 \
-e KOS_MOCK_URL=http://localhost:8084 \
acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
```

### 2. user-service 실행
```bash
SERVER_PORT=8081

docker run -d --name user-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=8081 \
-e SPRING_PROFILES_ACTIVE=dev \
-e DB_KIND=postgresql \
-e DB_HOST=localhost \
-e DB_PORT=5432 \
-e DB_NAME=phonebill_auth \
-e DB_USERNAME=phonebill_user \
-e DB_PASSWORD=phonebill_pass \
-e SHOW_SQL=true \
-e DDL_AUTO=update \
-e REDIS_HOST=localhost \
-e REDIS_PORT=6379 \
-e REDIS_PASSWORD= \
-e REDIS_DATABASE=0 \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e JWT_SECRET=your-jwt-secret-key-here \
-e JWT_ACCESS_TOKEN_VALIDITY=1800000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

### 3. bill-service 실행
```bash
SERVER_PORT=8082

docker run -d --name bill-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=8082 \
-e SPRING_PROFILES_ACTIVE=dev \
-e DB_HOST=20.249.107.185 \
-e DB_PORT=5432 \
-e DB_NAME=product_change \
-e DB_USERNAME=product_user \
-e DB_PASSWORD=product_pass \
-e SHOW_SQL=true \
-e DDL_AUTO=update \
-e REDIS_HOST=localhost \
-e REDIS_PORT=6379 \
-e REDIS_PASSWORD= \
-e REDIS_DATABASE=2 \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e JWT_SECRET=your-jwt-secret-key-here \
-e JWT_ACCESS_TOKEN_VALIDITY=1800 \
-e KOS_BASE_URL=http://localhost:8084 \
-e KOS_CONNECT_TIMEOUT=5000 \
-e KOS_READ_TIMEOUT=30000 \
-e KOS_MAX_RETRIES=3 \
-e KOS_RETRY_DELAY=1000 \
-e KOS_CB_FAILURE_RATE=0.5 \
-e KOS_CB_SLOW_DURATION=10000 \
-e KOS_CB_SLOW_RATE=0.5 \
-e KOS_CB_WINDOW_SIZE=10 \
-e KOS_CB_MIN_CALLS=5 \
-e KOS_CB_HALF_OPEN_CALLS=3 \
-e KOS_CB_OPEN_DURATION=60000 \
-e LOG_PATTERN_CONSOLE= \
-e LOG_PATTERN_FILE= \
-e LOG_FILE_MAX_SIZE=100MB \
-e LOG_FILE_MAX_HISTORY=30 \
acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
```

### 4. product-service 실행
```bash
SERVER_PORT=8083

docker run -d --name product-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=8083 \
-e SPRING_PROFILES_ACTIVE=dev \
-e DB_HOST=localhost \
-e DB_PORT=5432 \
-e DB_NAME=product_change \
-e DB_USERNAME=product_user \
-e DB_PASSWORD=product_pass \
-e SHOW_SQL=true \
-e DDL_AUTO=update \
-e REDIS_HOST=localhost \
-e REDIS_PORT=6379 \
-e REDIS_PASSWORD= \
-e REDIS_DATABASE=2 \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e JWT_SECRET=your-jwt-secret-key-here \
-e JWT_ACCESS_TOKEN_VALIDITY=1800000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e KOS_BASE_URL=http://localhost:8084 \
-e KOS_CONNECT_TIMEOUT=5000 \
-e KOS_READ_TIMEOUT=10000 \
-e KOS_MAX_RETRIES=3 \
-e KOS_RETRY_DELAY=1000 \
-e KOS_CB_FAILURE_RATE=0.5 \
-e KOS_CB_SLOW_CALL_THRESHOLD=10000 \
-e KOS_CB_SLOW_CALL_RATE=0.5 \
-e KOS_CB_SLIDING_WINDOW_SIZE=10 \
-e KOS_CB_MIN_CALLS=5 \
-e KOS_CB_HALF_OPEN_CALLS=3 \
-e KOS_CB_WAIT_DURATION=60000 \
acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
```

### 5. kos-mock 실행
```bash
SERVER_PORT=8084

docker run -d --name kos-mock --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=8084 \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

## ✅ 실행된 컨테이너 확인

### 모든 서비스 상태 확인
```bash
docker ps | grep -E "api-gateway|user-service|bill-service|product-service|kos-mock"
```

### 개별 서비스 확인
```bash
docker ps | grep api-gateway
docker ps | grep user-service
docker ps | grep bill-service
docker ps | grep product-service
docker ps | grep kos-mock
```

### 로그 확인
```bash
# 각 서비스별 로그 확인
docker logs api-gateway
docker logs user-service
docker logs bill-service
docker logs product-service
docker logs kos-mock
```

## 🔄 재배포 방법

### 1. 컨테이너 이미지 재생성 (로컬에서 수행)
```bash
/deploy-build-image-back
```

### 2. 컨테이너 이미지 푸시 (로컬에서 수행)
```bash
# 각 서비스별로 수행
docker tag api-gateway:latest acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest

docker tag user-service:latest acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/user-service:latest

docker tag bill-service:latest acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest

docker tag product-service:latest acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/product-service:latest

docker tag kos-mock:latest acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

### 3. 컨테이너 중지 (VM에서 수행)
```bash
docker stop api-gateway
docker stop user-service
docker stop bill-service
docker stop product-service
docker stop kos-mock
```

### 4. 컨테이너 이미지 삭제 (VM에서 수행)
```bash
docker rmi acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

### 5. 컨테이너 재실행 (VM에서 수행)
위의 "컨테이너 실행" 섹션의 명령어를 다시 실행

## ⚠️ 주의사항

1. **JWT_SECRET**: 실제 배포 시 보안이 강화된 비밀키로 변경 필요
2. **CORS 설정**: 프론트엔드 주소가 변경되면 CORS_ALLOWED_ORIGINS도 함께 변경
3. **데이터베이스**: 실제 데이터베이스 서버 주소와 포트로 변경 필요
4. **Redis**: 실제 Redis 서버 주소와 포트로 변경 필요
5. **네트워크**: 컨테이너 간 통신을 위해 Docker 네트워크 구성 고려
6. **서비스 시작 순서**: kos-mock → user-service → bill-service → product-service → api-gateway 순서로 시작 권장

## 🔗 서비스 접속 확인

- **API Gateway**: http://4.230.5.6:8080
- **User Service**: http://4.230.5.6:8081  
- **Bill Service**: http://4.230.5.6:8082
- **Product Service**: http://4.230.5.6:8083
- **KOS Mock**: http://4.230.5.6:8084