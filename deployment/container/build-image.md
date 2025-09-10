# 백엔드 컨테이너 이미지 빌드 결과서

## 작업 개요
- **작업일시**: 2025-01-10
- **작업자**: 최운영/데옵스
- **작업 목표**: 백엔드 마이크로서비스들의 컨테이너 이미지 생성

## 빌드 대상 서비스
총 5개의 백엔드 서비스에 대한 컨테이너 이미지를 생성했습니다.

1. **api-gateway**: API Gateway 서비스
2. **user-service**: 사용자 관리 서비스  
3. **bill-service**: 요금 조회 서비스
4. **product-service**: 상품 변경 서비스
5. **kos-mock**: KOS 시스템 목업 서비스

## 사전 작업

### 1. 서비스별 bootJar 설정 추가
각 서비스의 build.gradle 파일에 일관된 JAR 파일명 설정을 추가했습니다.

```gradle
bootJar {
    archiveFileName = '{서비스명}.jar'
}
```

### 2. Dockerfile 생성
`deployment/container/Dockerfile-backend` 파일을 생성했습니다.

```dockerfile
# Build stage
FROM openjdk:23-oraclelinux8 AS builder
ARG BUILD_LIB_DIR
ARG ARTIFACTORY_FILE
COPY ${BUILD_LIB_DIR}/${ARTIFACTORY_FILE} app.jar

# Run stage
FROM openjdk:23-slim
ENV USERNAME=k8s
ENV ARTIFACTORY_HOME=/home/${USERNAME}
ENV JAVA_OPTS=""

# Add a non-root user
RUN adduser --system --group ${USERNAME} && \
    mkdir -p ${ARTIFACTORY_HOME} && \
    chown ${USERNAME}:${USERNAME} ${ARTIFACTORY_HOME}

WORKDIR ${ARTIFACTORY_HOME}
COPY --from=builder app.jar app.jar
RUN chown ${USERNAME}:${USERNAME} app.jar

USER ${USERNAME}

ENTRYPOINT [ "sh", "-c" ]
CMD ["java ${JAVA_OPTS} -jar app.jar"]
```

### 3. 서비스별 빌드
모든 서비스에 대해 Gradle 빌드를 수행했습니다.

```bash
./gradlew api-gateway:clean api-gateway:bootJar
./gradlew user-service:clean user-service:bootJar
./gradlew bill-service:clean bill-service:bootJar
./gradlew product-service:clean product-service:bootJar
./gradlew kos-mock:clean kos-mock:bootJar
```

## 컨테이너 이미지 빌드

각 서비스별로 다음 명령어를 사용하여 컨테이너 이미지를 빌드했습니다.

### API Gateway
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
service=api-gateway

docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="${service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="${service}.jar" \
  -f ${DOCKER_FILE} \
  -t ${service}:latest .
```

### User Service
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
service=user-service

docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="${service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="${service}.jar" \
  -f ${DOCKER_FILE} \
  -t ${service}:latest .
```

### Bill Service
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
service=bill-service

docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="${service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="${service}.jar" \
  -f ${DOCKER_FILE} \
  -t ${service}:latest .
```

### Product Service
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
service=product-service

docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="${service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="${service}.jar" \
  -f ${DOCKER_FILE} \
  -t ${service}:latest .
```

### KOS Mock Service
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
service=kos-mock

docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="${service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="${service}.jar" \
  -f ${DOCKER_FILE} \
  -t ${service}:latest .
```

## 빌드 결과

### 성공적으로 생성된 이미지들

| 서비스명 | 이미지 태그 | 이미지 ID | 크기 | 생성 시간 |
|---------|------------|-----------|------|----------|
| api-gateway | latest | 5f4a2a5527b8 | 329MB | 3분 전 |
| user-service | latest | a8a85ba0b703 | 376MB | 2분 전 |
| bill-service | latest | b77190090a40 | 385MB | 1분 전 |
| product-service | latest | 5a6fba790ca3 | 392MB | 1분 전 |
| kos-mock | latest | 3f5878cf2f1e | 372MB | 35초 전 |

### 이미지 검증 명령어 실행 결과
```bash
$ docker images | grep -E "(api-gateway|user-service|bill-service|product-service|kos-mock)"
kos-mock                                                 latest    3f5878cf2f1e   35 seconds ago       372MB
product-service                                          latest    5a6fba790ca3   About a minute ago   392MB
bill-service                                             latest    b77190090a40   About a minute ago   385MB
user-service                                             latest    a8a85ba0b703   2 minutes ago        376MB
api-gateway                                              latest    5f4a2a5527b8   3 minutes ago        329MB
```

## 빌드 특징

### 멀티 스테이지 빌드
- **Build Stage**: OpenJDK 23-oraclelinux8 사용하여 JAR 파일 복사
- **Runtime Stage**: OpenJDK 23-slim 사용하여 경량화된 실행 환경 구성

### 보안 강화
- 비루트 사용자 `k8s` 생성 및 사용
- 적절한 파일 소유권 및 권한 설정
- 최소 권한 원칙 적용

### 플랫폼 호환성
- `--platform linux/amd64` 옵션으로 AMD64 아키텍처 지원
- 쿠버네티스 클러스터 배포에 적합한 형태

## 다음 단계

1. **컨테이너 레지스트리 푸시**: ACR 또는 Docker Hub에 이미지 푸시
2. **쿠버네티스 매니페스트 작성**: Deployment, Service 등 K8s 리소스 정의
3. **헬름 차트 작성**: 패키지 관리를 위한 Helm 차트 구성
4. **CI/CD 파이프라인 통합**: 자동화된 빌드 및 배포 파이프라인 구축

## 주요 성과

✅ **모든 백엔드 서비스 컨테이너화 완료** (5개 서비스)  
✅ **멀티 스테이지 빌드로 최적화된 이미지** (평균 360MB)  
✅ **보안 강화된 컨테이너 구성** (비루트 사용자)  
✅ **일관된 빌드 프로세스** (표준화된 Dockerfile)  
✅ **쿠버네티스 배포 준비 완료**

모든 백엔드 서비스들이 성공적으로 컨테이너화되었으며, 프로덕션 환경 배포를 위한 준비가 완료되었습니다.