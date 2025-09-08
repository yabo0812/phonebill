# Bill Service - 통신요금 조회 서비스

통신요금 관리 시스템의 요금조회 마이크로서비스입니다.

## 📋 서비스 개요

- **서비스명**: Bill Service (요금조회 서비스)
- **포트**: 8081
- **컨텍스트 패스**: /bill-service
- **버전**: 1.0.0

## 🏗️ 아키텍처

### 기술 스택
- **Java**: 17
- **Spring Boot**: 3.2
- **Spring Security**: JWT 기반 인증
- **Spring Data JPA**: 데이터 접근 계층
- **MySQL**: 8.0+
- **Redis**: 캐시 서버
- **Resilience4j**: Circuit Breaker, Retry, TimeLimiter
- **Swagger/OpenAPI**: API 문서화

### 주요 패턴
- **Layered Architecture**: Controller → Service → Repository
- **Circuit Breaker Pattern**: 외부 시스템 장애 격리
- **Cache-Aside Pattern**: Redis를 통한 성능 최적화
- **Async Pattern**: 이력 저장 비동기 처리

## 🚀 주요 기능

### 1. 요금조회 메뉴 (GET /api/bills/menu)
- 고객 정보 및 조회 가능한 월 목록 제공
- 캐시를 통한 빠른 응답

### 2. 요금조회 신청 (POST /api/bills/inquiry)
- 실시간 요금 정보 조회
- KOS 시스템 연동
- Circuit Breaker를 통한 장애 격리
- 비동기 이력 저장

### 3. 요금조회 결과 확인 (GET /api/bills/inquiry/{requestId})
- 비동기 처리된 요금조회 결과 확인
- 처리 상태별 응답 제공

### 4. 요금조회 이력 (GET /api/bills/history)
- 사용자별 요금조회 이력 목록
- 페이징, 필터링 지원

## 📁 프로젝트 구조

```
bill-service/
├── src/main/java/com/phonebill/bill/
│   ├── BillServiceApplication.java          # 메인 애플리케이션
│   ├── common/                              # 공통 컴포넌트
│   │   ├── entity/BaseTimeEntity.java       # 기본 엔티티
│   │   └── response/ApiResponse.java        # API 응답 래퍼
│   ├── config/                              # 설정 클래스
│   │   ├── CircuitBreakerConfig.java        # Circuit Breaker 설정
│   │   ├── KosProperties.java               # KOS 연동 설정
│   │   ├── RedisConfig.java                 # Redis 캐시 설정
│   │   ├── RestTemplateConfig.java          # HTTP 클라이언트 설정
│   │   └── SecurityConfig.java              # Spring Security 설정
│   ├── controller/                          # REST 컨트롤러
│   │   └── BillController.java              # 요금조회 API
│   ├── dto/                                 # 데이터 전송 객체
│   │   ├── BillHistoryResponse.java         # 이력 응답
│   │   ├── BillInquiryRequest.java          # 조회 요청
│   │   ├── BillInquiryResponse.java         # 조회 응답
│   │   └── BillMenuResponse.java            # 메뉴 응답
│   ├── exception/                           # 예외 처리
│   │   ├── BillInquiryException.java        # 요금조회 예외
│   │   ├── BusinessException.java           # 비즈니스 예외
│   │   ├── CircuitBreakerException.java     # Circuit Breaker 예외
│   │   ├── GlobalExceptionHandler.java     # 전역 예외 핸들러
│   │   └── KosConnectionException.java      # KOS 연동 예외
│   ├── repository/                          # 데이터 접근 계층
│   │   ├── BillInquiryHistoryRepository.java # 이력 리포지토리
│   │   └── entity/
│   │       └── BillInquiryHistoryEntity.java # 이력 엔티티
│   ├── service/                             # 비즈니스 로직
│   │   ├── BillCacheService.java            # 캐시 서비스
│   │   ├── BillHistoryService.java          # 이력 서비스
│   │   ├── BillInquiryService.java          # 조회 서비스 인터페이스
│   │   ├── BillInquiryServiceImpl.java      # 조회 서비스 구현
│   │   └── KosClientService.java            # KOS 연동 서비스
│   └── model/                               # 외부 시스템 모델
│       ├── KosRequest.java                  # KOS 요청
│       └── KosResponse.java                 # KOS 응답
└── src/main/resources/
    ├── application.yml                      # 기본 설정
    ├── application-dev.yml                  # 개발환경 설정
    └── application-prod.yml                 # 운영환경 설정
```

## 🔧 설치 및 실행

### 사전 요구사항
- Java 17
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 데이터베이스 설정
```sql
-- 데이터베이스 생성
CREATE DATABASE bill_service_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE bill_service_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER 'dev_user'@'%' IDENTIFIED BY 'dev_pass';
GRANT ALL PRIVILEGES ON bill_service_dev.* TO 'dev_user'@'%';

CREATE USER 'bill_user'@'%' IDENTIFIED BY 'bill_pass';  
GRANT ALL PRIVILEGES ON bill_service_prod.* TO 'bill_user'@'%';

FLUSH PRIVILEGES;
```

### 테이블 생성
```sql
-- 요금조회 이력 테이블
CREATE TABLE bill_inquiry_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL UNIQUE,
    line_number VARCHAR(20) NOT NULL,
    inquiry_month VARCHAR(7) NOT NULL,
    request_time DATETIME(6) NOT NULL,
    process_time DATETIME(6),
    status VARCHAR(20) NOT NULL,
    result_summary TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_line_number (line_number),
    INDEX idx_inquiry_month (inquiry_month),
    INDEX idx_request_time (request_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 애플리케이션 실행

#### 개발환경 실행
```bash
# 소스 컴파일 및 실행
./mvnw clean compile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 또는 JAR 실행
./mvnw clean package
java -jar target/bill-service-1.0.0.jar --spring.profiles.active=dev
```

#### 운영환경 실행
```bash
java -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod \
  -jar bill-service-1.0.0.jar
```

## 🔗 API 문서

### Swagger UI
- **개발환경**: http://localhost:8081/bill-service/swagger-ui.html
- **API Docs**: http://localhost:8081/bill-service/v3/api-docs

### 주요 API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/bills/menu` | 요금조회 메뉴 조회 |
| POST | `/api/bills/inquiry` | 요금조회 신청 |
| GET | `/api/bills/inquiry/{requestId}` | 요금조회 결과 확인 |
| GET | `/api/bills/history` | 요금조회 이력 목록 |

## 📊 모니터링

### Health Check
- **URL**: http://localhost:8081/bill-service/actuator/health
- **상태**: Database, Redis, Disk Space 상태 확인

### Metrics
- **Prometheus**: http://localhost:8081/bill-service/actuator/prometheus
- **Metrics**: http://localhost:8081/bill-service/actuator/metrics

### 로그 파일
- **개발환경**: `logs/bill-service-dev.log`
- **운영환경**: `logs/bill-service.log`

## ⚙️ 환경변수 설정

### 필수 환경변수 (운영환경)
```bash
# 데이터베이스 연결 정보
export DB_URL="jdbc:mysql://prod-db-host:3306/bill_service_prod"
export DB_USERNAME="bill_user"
export DB_PASSWORD="secure_password"

# Redis 연결 정보  
export REDIS_HOST="prod-redis-host"
export REDIS_PASSWORD="redis_password"

# KOS 시스템 연동
export KOS_BASE_URL="https://kos-system.company.com"
export KOS_API_KEY="production_api_key"
export KOS_SECRET_KEY="production_secret_key"
```

## 🚀 배포 가이드

### Docker 배포
```dockerfile
FROM openjdk:17-jre-slim
COPY target/bill-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes 배포
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bill-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: bill-service
  template:
    metadata:
      labels:
        app: bill-service
    spec:
      containers:
      - name: bill-service
        image: bill-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

## 📈 성능 최적화

### 캐시 전략
- **요금 데이터**: 1시간 TTL
- **고객 정보**: 4시간 TTL
- **조회 가능 월**: 24시간 TTL

### Circuit Breaker 설정
- **실패율 임계값**: 50%
- **응답시간 임계값**: 10초
- **Open 상태 유지**: 60초

### 데이터베이스 최적화
- 커넥션 풀 최대 크기: 50 (운영환경)
- 배치 처리 활성화
- 쿼리 인덱스 최적화

## 🐛 트러블슈팅

### 일반적인 문제들

1. **데이터베이스 연결 실패**
   - 연결 정보 확인
   - 방화벽 설정 확인
   - 데이터베이스 서비스 상태 확인

2. **Redis 연결 실패**
   - Redis 서비스 상태 확인
   - 네트워크 연결 확인
   - 인증 정보 확인

3. **KOS 시스템 연동 실패**
   - Circuit Breaker 상태 확인
   - API 키/시크릿 키 확인
   - 네트워크 연결 확인

## 👥 개발팀

- **Backend Developer**: 이개발(백엔더)
- **Email**: dev@phonebill.com
- **Version**: 1.0.0
- **Last Updated**: 2025-09-08