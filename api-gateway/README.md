# PhoneBill API Gateway

통신요금 관리 서비스의 API Gateway 모듈입니다.

## 개요

Spring Cloud Gateway를 사용하여 구현된 API Gateway로, 마이크로서비스들의 단일 진입점 역할을 담당합니다.

### 주요 기능

- **JWT 토큰 기반 인증/인가**: 모든 요청에 대한 통합 인증 처리
- **서비스별 라우팅**: 각 마이크로서비스로의 지능형 라우팅
- **Rate Limiting**: Redis 기반 요청 제한
- **Circuit Breaker**: 외부 시스템 장애 격리
- **CORS 설정**: 크로스 오리진 요청 처리
- **API 문서화 통합**: 모든 서비스의 Swagger 문서 통합
- **헬스체크**: 시스템 상태 모니터링
- **Fallback 처리**: 서비스 장애 시 대체 응답

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Cloud Gateway**
- **Spring Data Redis Reactive**
- **JWT (JJWT 0.12.3)**
- **Resilience4j** (Circuit Breaker)
- **SpringDoc OpenAPI 3**

## 아키텍처

### 라우팅 구성

```
/auth/**      -> auth-service (인증 서비스)
/bills/**     -> bill-service (요금조회 서비스)
/products/**  -> product-service (상품변경 서비스)
/kos/**       -> kos-mock-service (KOS 목업 서비스)
```

### 패키지 구조

```
com.unicorn.phonebill.gateway/
├── config/                     # 설정 클래스
│   ├── GatewayConfig          # Gateway 라우팅 설정
│   ├── RedisConfig            # Redis 및 Rate Limiting 설정
│   ├── SwaggerConfig          # API 문서화 설정
│   └── WebConfig              # Web 설정
├── controller/                 # 컨트롤러
│   └── HealthController       # 헬스체크 API
├── dto/                       # 데이터 전송 객체
│   └── TokenValidationResult  # JWT 검증 결과
├── exception/                 # 예외 클래스
│   └── GatewayException      # Gateway 예외
├── filter/                    # Gateway 필터
│   └── JwtAuthenticationGatewayFilterFactory # JWT 인증 필터
├── handler/                   # 핸들러
│   └── FallbackHandler       # Circuit Breaker Fallback 핸들러
├── service/                   # 서비스
│   └── JwtTokenService       # JWT 토큰 검증 서비스
└── util/                      # 유틸리티
    └── JwtUtil                # JWT 유틸리티
```

## 빌드 및 실행

### 개발 환경

```bash
# 의존성 설치 및 빌드
./gradlew build

# 개발 환경 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는
./gradlew bootRun -Pdev
```

### 운영 환경

```bash
# 운영용 JAR 빌드
./gradlew bootJar

# 운영 환경 실행
java -jar api-gateway-1.0.0.jar --spring.profiles.active=prod
```

## 환경 설정

### 개발 환경 (application-dev.yml)

- JWT 토큰 유효시간: 1시간 (개발 편의성)
- Redis: localhost:6379
- Rate Limiting: 1000 requests/minute
- Circuit Breaker: 관대한 설정
- Swagger UI: 활성화

### 운영 환경 (application-prod.yml)

- JWT 토큰 유효시간: 30분 (보안 강화)
- Redis: 클러스터 설정
- Rate Limiting: 500 requests/minute
- Circuit Breaker: 엄격한 설정
- Swagger UI: 비활성화

### 환경 변수

운영 환경에서는 다음 환경 변수를 설정해야 합니다:

```bash
JWT_SECRET=your-256-bit-secret-key
REDIS_HOST=redis-cluster.domain.com
REDIS_PASSWORD=your-redis-password
AUTH_SERVICE_URL=https://auth-service.internal.domain.com
BILL_SERVICE_URL=https://bill-service.internal.domain.com
PRODUCT_SERVICE_URL=https://product-service.internal.domain.com
KOS_MOCK_SERVICE_URL=https://kos-mock.internal.domain.com
```

## API 문서

### 개발 환경

Swagger UI는 개발 환경에서만 활성화됩니다:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

### 헬스체크

- **기본 헬스체크**: `GET /health`
- **상세 헬스체크**: `GET /health/detailed`
- **Actuator 헬스체크**: `GET /actuator/health`

## JWT 인증

### 토큰 형식

```
Authorization: Bearer <JWT_TOKEN>
```

### 토큰 페이로드 예시

```json
{
  "sub": "user123",
  "role": "USER",
  "iat": 1704700800,
  "exp": 1704704400,
  "jti": "token-unique-id"
}
```

### 인증 제외 경로

- `/auth/login` (로그인)
- `/auth/refresh` (토큰 갱신)
- `/health` (헬스체크)
- `/actuator/health` (Actuator 헬스체크)

## Rate Limiting

### 제한 정책

- **일반 사용자**: 100 requests/minute
- **VIP 사용자**: 500 requests/minute
- **IP 기반 제한**: Fallback으로 사용

### Key Resolver

1. **userKeyResolver**: JWT에서 사용자 ID 추출 (기본)
2. **ipKeyResolver**: 클라이언트 IP 기반
3. **pathKeyResolver**: API 경로 기반

## Circuit Breaker

### 설정

- **실패율 임계값**: 50% (auth), 60% (bill, product), 70% (kos)
- **최소 호출 수**: 5-20회
- **Open 상태 대기시간**: 10-60초
- **Half-Open 상태 허용 호출**: 3-10회

### Fallback

Circuit Breaker가 Open 상태일 때 Fallback 응답을 제공:

- **인증 서비스**: 503 Service Unavailable
- **요금조회 서비스**: 캐시된 메뉴 데이터 제공 가능
- **상품변경 서비스**: 고객센터 안내 메시지
- **KOS 서비스**: 외부 시스템 점검 안내

## 모니터링

### Actuator 엔드포인트

```bash
# 애플리케이션 상태
GET /actuator/health

# Gateway 라우트 정보
GET /actuator/gateway/routes

# 메트릭 정보
GET /actuator/metrics

# 환경 정보 (개발환경만)
GET /actuator/env
```

### 로깅

- **개발환경**: DEBUG 레벨, 상세한 요청/응답 로그
- **운영환경**: INFO 레벨, 성능 고려한 최적화된 로그

## 보안

### HTTPS

운영 환경에서는 반드시 HTTPS를 사용해야 합니다.

### CORS

- **개발환경**: 모든 localhost 오리진 허용
- **운영환경**: 특정 도메인만 허용

### 보안 헤더

- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- X-XSS-Protection: 1; mode=block

## 트러블슈팅

### 일반적인 문제

1. **Redis 연결 실패**
   ```bash
   # Redis 서비스 상태 확인
   systemctl status redis
   
   # Redis 연결 테스트
   redis-cli ping
   ```

2. **JWT 검증 실패**
   ```bash
   # JWT 시크릿 키 확인
   echo $JWT_SECRET
   
   # 토큰 유효성 확인 (개발용)
   curl -H "Authorization: Bearer <token>" http://localhost:8080/health
   ```

3. **Circuit Breaker Open**
   ```bash
   # Circuit Breaker 상태 확인
   curl http://localhost:8080/actuator/circuitbreakers
   ```

### 로그 확인

```bash
# 개발환경 로그
tail -f logs/api-gateway-dev.log

# 운영환경 로그
tail -f /var/log/api-gateway/api-gateway.log
```

## 성능 튜닝

### JVM 옵션 (운영환경)

```bash
java -server \
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=16m \
  -XX:+UseStringDeduplication \
  -jar api-gateway-1.0.0.jar
```

### Redis 최적화

- Connection Pool 설정 조정
- Pipeline 사용 고려
- 클러스터 모드 활용

## 개발 가이드

### 새로운 서비스 추가

1. `GatewayConfig`에 라우팅 규칙 추가
2. `SwaggerConfig`에 API 문서 URL 추가
3. `FallbackHandler`에 Fallback 로직 추가
4. Circuit Breaker 설정 추가

### 커스텀 필터 추가

```java
@Component
public class CustomGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {
    // 필터 구현
}
```

## 릴리스 노트

### v1.0.0 (2025-01-08)

- 초기 릴리스
- JWT 인증 시스템 구현
- 4개 마이크로서비스 라우팅 지원
- Circuit Breaker 및 Rate Limiting 구현
- Swagger 통합 문서화
- 헬스체크 및 모니터링 기능

## 라이선스

이 프로젝트는 회사 내부 프로젝트입니다.

## 기여

- **개발팀**: 이개발(백엔더)
- **검토**: 김기획(기획자), 박화면(프론트), 최운영(데옵스), 정테스트(QA매니저)