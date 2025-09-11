# 백엔드 컨테이너 실행 가이드

## 1. 시스템 정보
- **시스템명**: phonebill
- **서비스 목록**: 
  - api-gateway (포트: 8080)
  - user-service (포트: 8081) 
  - bill-service (포트: 8082)
  - product-service (포트: 8083)
  - kos-mock (포트: 8084)

## 2. VM 접속 방법

### Windows 사용자
1. **Windows Terminal** 실행
2. 아래 명령어 순서대로 실행:

```bash
# Private Key 파일 권한 설정 (최초 1회만)
chmod 400 ~/home/bastion-dg0500

# VM 접속
ssh -i ~/home/bastion-dg0500 azureuser@4.230.5.6
```

### Linux/Mac 사용자
1. **터미널** 실행
2. 아래 명령어 순서대로 실행:

```bash
# Private Key 파일 권한 설정 (최초 1회만)
chmod 400 ~/home/bastion-dg0500

# VM 접속
ssh -i ~/home/bastion-dg0500 azureuser@4.230.5.6
```

## 3. 컨테이너 이미지 준비

### 3.1 이미지 빌드 (로컬에서 수행)
먼저 로컬에서 이미지를 빌드해야 합니다. 아래 가이드를 참고하여 수행하세요:
```bash
# 이미지 빌드 가이드 확인
cat deployment/container/build-image.md
```

### 3.2 Azure Container Registry 로그인
ACR 인증 정보를 확인하고 Docker 로그인을 수행합니다:

```bash
# ACR 인증 정보 확인
az acr credential show --name acrdigitalgarage01

# 출력 예시:
# {
#   "passwords": [
#     {
#       "name": "password", 
#       "value": "실제암호"
#     }
#   ],
#   "username": "acrdigitalgarage01"
# }

# Docker 로그인 (위에서 확인한 username과 password 사용)
docker login acrdigitalgarage01.azurecr.io -u acrdigitalgarage01 -p 실제암호
```

### 3.3 이미지 Push (로컬에서 수행)
각 서비스별로 이미지를 태깅하고 푸시합니다:

```bash
# API Gateway
docker tag api-gateway:latest acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest

# User Service
docker tag user-service:latest acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/user-service:latest

# Bill Service
docker tag bill-service:latest acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest

# Product Service
docker tag product-service:latest acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/product-service:latest

# KOS Mock
docker tag kos-mock:latest acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

## 4. 컨테이너 실행 (VM에서 수행)

### 4.1 KOS Mock 서비스 실행
```bash
SERVER_PORT=8084

docker run -d --name kos-mock --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=8084 \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

### 4.2 User Service 실행
```bash
SERVER_PORT=8081

docker run -d --name user-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e DB_HOST=20.249.70.6 \
-e DB_KIND=postgresql \
-e DB_NAME=phonebill_auth \
-e DB_PASSWORD=AuthUser2025! \
-e DB_PORT=5432 \
-e DB_USERNAME=auth_user \
-e DDL_AUTO=update \
-e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e JWT_SECRET="nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==" \
-e REDIS_DATABASE=0 \
-e REDIS_HOST=20.249.193.103 \
-e REDIS_PASSWORD=Redis2025Dev! \
-e REDIS_PORT=6379 \
-e SERVER_PORT=8081 \
-e SHOW_SQL=true \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

### 4.3 Bill Service 실행
```bash
SERVER_PORT=8082

docker run -d --name bill-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e DB_CONNECTION_TIMEOUT=30000 \
-e DB_HOST=20.249.175.46 \
-e DB_IDLE_TIMEOUT=600000 \
-e DB_KIND=postgresql \
-e DB_LEAK_DETECTION=60000 \
-e DB_MAX_LIFETIME=1800000 \
-e DB_MAX_POOL=20 \
-e DB_MIN_IDLE=5 \
-e DB_NAME=bill_inquiry_db \
-e DB_PASSWORD=BillUser2025! \
-e DB_PORT=5432 \
-e DB_USERNAME=bill_inquiry_user \
-e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e JWT_SECRET="nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==" \
-e KOS_BASE_URL=http://4.230.5.6:8084 \
-e LOG_FILE_NAME=logs/bill-service.log \
-e REDIS_DATABASE=1 \
-e REDIS_HOST=20.249.193.103 \
-e REDIS_MAX_ACTIVE=8 \
-e REDIS_MAX_IDLE=8 \
-e REDIS_MAX_WAIT=-1 \
-e REDIS_MIN_IDLE=0 \
-e REDIS_PASSWORD=Redis2025Dev! \
-e REDIS_PORT=6379 \
-e REDIS_TIMEOUT=2000 \
-e SERVER_PORT=8082 \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
```

### 4.4 Product Service 실행
```bash
SERVER_PORT=8083

docker run -d --name product-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e DB_HOST=20.249.107.185 \
-e DB_KIND=postgresql \
-e DB_NAME=product_change_db \
-e DB_PASSWORD=ProductUser2025! \
-e DB_PORT=5432 \
-e DB_USERNAME=product_change_user \
-e DDL_AUTO=update \
-e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e JWT_SECRET="nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==" \
-e KOS_API_KEY=dev-api-key \
-e KOS_BASE_URL=http://4.230.5.6:8084 \
-e KOS_CLIENT_ID=product-service-dev \
-e KOS_MOCK_ENABLED=true \
-e REDIS_DATABASE=2 \
-e REDIS_HOST=20.249.193.103 \
-e REDIS_PASSWORD=Redis2025Dev! \
-e REDIS_PORT=6379 \
-e SERVER_PORT=8083 \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
```

### 4.5 API Gateway 실행
```bash
SERVER_PORT=8080

docker run -d --name api-gateway --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e BILL_SERVICE_URL=http://4.230.5.6:8082 \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e JWT_SECRET="nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==" \
-e KOS_MOCK_URL=http://4.230.5.6:8084 \
-e PRODUCT_SERVICE_URL=http://4.230.5.6:8083 \
-e SERVER_PORT=8080 \
-e SPRING_PROFILES_ACTIVE=dev \
-e USER_SERVICE_URL=http://4.230.5.6:8081 \
acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
```

## 5. 컨테이너 실행 확인

전체 서비스가 정상적으로 실행되었는지 확인:

```bash
# 모든 서비스 확인
docker ps | grep -E "(api-gateway|user-service|bill-service|product-service|kos-mock)"

# 개별 서비스 확인
docker ps | grep api-gateway
docker ps | grep user-service
docker ps | grep bill-service
docker ps | grep product-service
docker ps | grep kos-mock
```

## 6. 서비스 접속 테스트

각 서비스의 헬스체크 엔드포인트로 정상 동작을 확인:

```bash
# API Gateway
curl http://4.230.5.6:8080/actuator/health

# User Service  
curl http://4.230.5.6:8081/actuator/health

# Bill Service
curl http://4.230.5.6:8082/actuator/health

# Product Service
curl http://4.230.5.6:8083/actuator/health

# KOS Mock
curl http://4.230.5.6:8084/actuator/health
```

## 7. 재배포 방법

### 7.1 컨테이너 이미지 재생성 (로컬에서 수행)
```bash
# 이미지 재빌드
/deploy-build-image-back
```

### 7.2 컨테이너 이미지 푸시 (로컬에서 수행)
특정 서비스만 재배포하는 경우:
```bash
# 예: user-service 재배포
docker tag user-service:latest acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

### 7.3 컨테이너 재시작 (VM에서 수행)
```bash
# 1. 기존 컨테이너 중지
docker stop user-service

# 2. 컨테이너 이미지 삭제 (캐시 갱신을 위해)
docker rmi acrdigitalgarage01.azurecr.io/phonebill/user-service:latest

# 3. 새 이미지로 컨테이너 재실행
SERVER_PORT=8081

docker run -d --name user-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e DB_HOST=20.249.70.6 \
-e DB_KIND=postgresql \
-e DB_NAME=phonebill_auth \
-e DB_PASSWORD=AuthUser2025! \
-e DB_PORT=5432 \
-e DB_USERNAME=auth_user \
-e DDL_AUTO=update \
-e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e JWT_SECRET="nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==" \
-e REDIS_DATABASE=0 \
-e REDIS_HOST=20.249.193.103 \
-e REDIS_PASSWORD=Redis2025Dev! \
-e REDIS_PORT=6379 \
-e SERVER_PORT=8081 \
-e SHOW_SQL=true \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

## 8. 전체 서비스 재시작 스크립트

모든 서비스를 한번에 재시작하려면:

```bash
# 모든 컨테이너 중지
docker stop api-gateway user-service bill-service product-service kos-mock

# 모든 이미지 삭제
docker rmi acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/user-service:latest  
docker rmi acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
docker rmi acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest

# 컨테이너 재실행 (위의 4.1 ~ 4.5 단계 순서대로 실행)
```

## 주의사항

1. **실행 순서**: KOS Mock → User Service → Bill Service → Product Service → API Gateway 순서로 실행하는 것을 권장합니다.

2. **CORS 설정**: 프론트엔드에서 접근할 수 있도록 VM IP(4.230.5.6:3000)가 CORS_ALLOWED_ORIGINS에 포함되어 있습니다.

3. **로그 확인**: 컨테이너 로그는 `docker logs [컨테이너명]` 명령으로 확인할 수 있습니다.

4. **네트워크**: 모든 서비스는 localhost로 서로 통신하도록 설정되어 있습니다.