# 백엔드 컨테이너 실행 가이드

## 시스템 정보
- **시스템명**: phonebill
- **ACR명**: acrdigitalgarage01
- **VM 정보**:
  - IP: 4.230.5.6
  - 사용자: azureuser  
  - SSH 키: ~/home/bastion-dg0500

## 백엔드 서비스 목록
총 5개의 백엔드 서비스를 컨테이너로 실행합니다.

| 서비스명 | 포트 | 설명 |
|---------|------|------|
| api-gateway | 8080 | API Gateway 서비스 |
| user-service | 8081 | 사용자 관리 서비스 |
| bill-service | 8082 | 요금 조회 서비스 |
| product-service | 8083 | 상품 변경 서비스 |
| kos-mock | 8084 | KOS 시스템 목업 서비스 |

## 사전 준비사항

### 1. 로컬에서 컨테이너 이미지 확인
현재 생성된 컨테이너 이미지를 확인합니다.
```bash
docker images | grep -E "(api-gateway|user-service|bill-service|product-service|kos-mock)"
```

### 2. ACR 인증정보 확인
Azure CLI를 통해 ACR 인증정보를 확인합니다.
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
    },
    {
      "name": "password2", 
      "value": "{암호2}"
    }
  ],
  "username": "acrdigitalgarage01"
}
```

## 컨테이너 이미지 푸시

### 1. ACR 로그인 (로컬)
```bash
docker login acrdigitalgarage01.azurecr.io -u acrdigitalgarage01 -p {암호}
```

### 2. 이미지 태그 및 푸시
각 서비스별로 다음 명령을 실행합니다:

#### API Gateway
```bash
docker tag api-gateway:latest acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
```

#### User Service
```bash
docker tag user-service:latest acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

#### Bill Service
```bash
docker tag bill-service:latest acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
```

#### Product Service
```bash
docker tag product-service:latest acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
```

#### KOS Mock
```bash
docker tag kos-mock:latest acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

## VM 접속 및 컨테이너 실행

### 1. VM 접속

#### Linux/Mac 사용자
터미널을 실행하고 다음 명령을 실행합니다:

```bash
# SSH 키 파일 권한 설정 (최초 한번만)
chmod 400 ~/home/bastion-dg0500

# VM 접속
ssh -i ~/home/bastion-dg0500 azureuser@4.230.5.6
```

#### Windows 사용자
Windows Terminal을 실행하고 동일한 명령을 실행합니다:

```bash
# SSH 키 파일 권한 설정 (최초 한번만)
chmod 400 ~/home/bastion-dg0500

# VM 접속
ssh -i ~/home/bastion-dg0500 azureuser@4.230.5.6
```

### 2. VM에서 ACR 로그인
```bash
docker login acrdigitalgarage01.azurecr.io -u acrdigitalgarage01 -p {암호}
```

### 3. 컨테이너 실행 명령

#### API Gateway (포트: 8080)
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
-e SERVER_PORT=${SERVER_PORT} \
-e SPRING_PROFILES_ACTIVE=dev \
-e USER_SERVICE_URL=http://4.230.5.6:8081 \
acrdigitalgarage01.azurecr.io/phonebill/api-gateway:latest
```

#### User Service (포트: 8081)
```bash
SERVER_PORT=8081

docker run -d --name user-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e DB_HOST=20.249.70.6 \
-e DB_KIND=postgresql \
-e DB_NAME=phonebill_auth \
-e DB_PASSWORD="AuthUser2025!" \
-e DB_PORT=5432 \
-e DB_USERNAME=auth_user \
-e DDL_AUTO=update \
-e JWT_ACCESS_TOKEN_VALIDITY=18000000 \
-e JWT_REFRESH_TOKEN_VALIDITY=86400000 \
-e JWT_SECRET="nwe5Yo9qaJ6FBD/Thl2/j6/SFAfNwUorAY1ZcWO2KI7uA4bmVLOCPxE9hYuUpRCOkgV2UF2DdHXtqHi3+BU/ecbz2zpHyf/720h48UbA3XOMYOX1sdM+dQ==" \
-e REDIS_DATABASE=0 \
-e REDIS_HOST=20.249.193.103 \
-e REDIS_PASSWORD="Redis2025Dev!" \
-e REDIS_PORT=6379 \
-e SERVER_PORT=${SERVER_PORT} \
-e SHOW_SQL=true \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

#### Bill Service (포트: 8082)
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
-e DB_PASSWORD="BillUser2025!" \
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
-e REDIS_PASSWORD="Redis2025Dev!" \
-e REDIS_PORT=6379 \
-e REDIS_TIMEOUT=2000 \
-e SERVER_PORT=${SERVER_PORT} \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/bill-service:latest
```

#### Product Service (포트: 8083)
```bash
SERVER_PORT=8083

docker run -d --name product-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://4.230.5.6:3000" \
-e DB_HOST=20.249.107.185 \
-e DB_KIND=postgresql \
-e DB_NAME=product_change_db \
-e DB_PASSWORD="ProductUser2025!" \
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
-e REDIS_PASSWORD="Redis2025Dev!" \
-e REDIS_PORT=6379 \
-e SERVER_PORT=${SERVER_PORT} \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/product-service:latest
```

#### KOS Mock (포트: 8084)
```bash
SERVER_PORT=8084

docker run -d --name kos-mock --rm -p ${SERVER_PORT}:${SERVER_PORT} \
-e SERVER_PORT=${SERVER_PORT} \
-e SPRING_PROFILES_ACTIVE=dev \
acrdigitalgarage01.azurecr.io/phonebill/kos-mock:latest
```

### 4. 컨테이너 실행 상태 확인
모든 컨테이너가 정상적으로 실행되고 있는지 확인합니다:

```bash
# 전체 컨테이너 상태 확인
docker ps

# 서비스별 개별 확인
docker ps | grep api-gateway
docker ps | grep user-service  
docker ps | grep bill-service
docker ps | grep product-service
docker ps | grep kos-mock
```

### 5. 서비스 헬스체크
각 서비스가 정상적으로 응답하는지 확인합니다:

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# User Service  
curl http://localhost:8081/actuator/health

# Bill Service
curl http://localhost:8082/actuator/health

# Product Service
curl http://localhost:8083/actuator/health

# KOS Mock
curl http://localhost:8084/actuator/health
```

## 재배포 방법

### 1. 컨테이너 이미지 재생성 (로컬)
```bash
/deploy-build-image-back
```

### 2. 컨테이너 이미지 푸시 (로컬)
각 서비스별로 실행:
```bash
# 예: User Service 재배포
docker tag user-service:latest acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
docker push acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

### 3. VM에서 컨테이너 중지 및 재시작
```bash
# 컨테이너 중지
docker stop user-service

# 이미지 삭제 (새 버전 pull을 위해)
docker rmi acrdigitalgarage01.azurecr.io/phonebill/user-service:latest

# 컨테이너 재실행 (위의 실행 명령 재사용)
SERVER_PORT=8081
docker run -d --name user-service --rm -p ${SERVER_PORT}:${SERVER_PORT} \
[환경변수들...] \
acrdigitalgarage01.azurecr.io/phonebill/user-service:latest
```

## 트러블슈팅

### 컨테이너 로그 확인
```bash
# 실시간 로그 확인
docker logs -f {서비스명}

# 최근 로그 확인  
docker logs --tail 100 {서비스명}
```

### 포트 사용 확인
```bash
# 포트 사용 상태 확인
netstat -tulpn | grep {포트번호}
```

### 네트워크 연결 테스트
```bash
# 서비스간 연결 테스트
curl http://localhost:{포트}/actuator/health
```

## 중요사항

1. **CORS 설정**: 모든 서비스의 CORS_ALLOWED_ORIGINS에 VM IP가 추가되어 있습니다 (`http://4.230.5.6:3000`)
2. **환경변수**: 모든 환경변수가 .run.xml 파일에서 추출되어 정확히 설정됩니다
3. **포트 매핑**: 각 서비스의 내부 포트와 호스트 포트가 동일하게 설정됩니다
4. **자동 재시작**: `--rm` 옵션으로 컨테이너 종료 시 자동 삭제됩니다
5. **백그라운드 실행**: `-d` 옵션으로 데몬 모드로 실행됩니다

이제 모든 백엔드 서비스가 VM에서 컨테이너로 실행될 준비가 완료되었습니다.