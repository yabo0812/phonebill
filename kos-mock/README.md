# KOS Mock Service

KT 통신사 시스템(KOS-Order)을 모방한 Mock 서비스입니다.

## 개요

KOS Mock Service는 통신요금 관리 서비스의 다른 마이크로서비스들이 외부 시스템과의 연동을 테스트할 수 있도록 하는 내부 Mock 서비스입니다.

## 주요 기능

### 1. 요금 조회 Mock API
- 고객의 통신요금 정보 조회
- 회선번호 기반 요금 데이터 제공
- 다양한 오류 상황 시뮬레이션

### 2. 상품 변경 Mock API
- 고객의 통신상품 변경 처리
- 상품 변경 가능성 검증
- KOS 주문 번호 생성

### 3. Mock 데이터 관리
- 테스트용 고객 데이터 제공
- 요금제별 Mock 상품 데이터
- 청구월별 요금 이력 데이터

## 기술 스택

- **Framework**: Spring Boot 3.2
- **Language**: Java 17
- **Documentation**: Swagger/OpenAPI 3.0
- **Cache**: Redis (선택적)
- **Test**: JUnit 5, MockMvc

## API 엔드포인트

### 기본 정보
- **Base URL**: `http://localhost:8080/kos-mock`
- **API Version**: v1
- **Content-Type**: `application/json`

### 주요 API

#### 1. 요금 조회 API
```http
POST /api/v1/kos/bill/inquiry
```

**요청 예시:**
```json
{
  "lineNumber": "01012345678",
  "billingMonth": "202501",
  "requestId": "REQ_20250108_001",
  "requestorId": "BILL_SERVICE"
}
```

#### 2. 상품 변경 API
```http
POST /api/v1/kos/product/change
```

**요청 예시:**
```json
{
  "lineNumber": "01012345678",
  "currentProductCode": "LTE-BASIC-001",
  "targetProductCode": "5G-PREMIUM-001",
  "requestId": "REQ_20250108_002",
  "requestorId": "PRODUCT_SERVICE",
  "changeReason": "고객 요청에 의한 상품 변경"
}
```

#### 3. 서비스 상태 체크 API
```http
GET /api/v1/kos/health
```

## Mock 데이터

### 테스트용 회선번호
- `01012345678` - 김테스트 (5G 프리미엄)
- `01087654321` - 이샘플 (5G 스탠다드)
- `01055554444` - 박데모 (LTE 프리미엄)
- `01099998888` - 최모의 (LTE 베이직)
- `01000000000` - 비활성사용자 (정지 상태)

### 상품 코드
- `5G-PREMIUM-001` - 5G 프리미엄 플랜 (89,000원)
- `5G-STANDARD-001` - 5G 스탠다드 플랜 (69,000원)
- `LTE-PREMIUM-001` - LTE 프리미엄 플랜 (59,000원)
- `LTE-BASIC-001` - LTE 베이직 플랜 (39,000원)
- `3G-OLD-001` - 3G 레거시 플랜 (판매 중단)

## 실행 방법

### 1. 개발 환경에서 실행
```bash
./gradlew bootRun
```

### 2. JAR 파일로 실행
```bash
./gradlew build
java -jar build/libs/kos-mock-service-1.0.0.jar
```

### 3. 특정 프로파일로 실행
```bash
java -jar kos-mock-service-1.0.0.jar --spring.profiles.active=prod
```

## 설정

### Mock 응답 지연 설정
```yaml
kos:
  mock:
    response-delay: 1000  # 밀리초
    failure-rate: 0.05    # 5% 실패율
```

### Redis 설정 (선택적)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 테스트

### 단위 테스트 실행
```bash
./gradlew test
```

### API 테스트
Swagger UI를 통해 API를 직접 테스트할 수 있습니다:
- URL: http://localhost:8080/kos-mock/swagger-ui.html

## 모니터링

### Health Check
- URL: http://localhost:8080/kos-mock/actuator/health

### Metrics
- URL: http://localhost:8080/kos-mock/actuator/metrics

## 주의사항

1. **내부 시스템 전용**: 이 서비스는 내부 테스트 목적으로만 사용하세요.
2. **보안 설정 간소화**: Mock 서비스이므로 보안 설정이 간소화되어 있습니다.
3. **데이터 지속성**: Mock 데이터는 메모리에만 저장되며, 재시작 시 초기화됩니다.
4. **성능 제한**: 실제 부하 테스트 용도로는 적합하지 않습니다.

## 문의

KOS Mock Service 관련 문의사항이 있으시면 개발팀으로 연락해 주세요.

- 개발팀: dev@phonebill.com
- 문서 버전: v1.0.0
- 최종 업데이트: 2025-01-08