# Product-Change Service 클래스 설계서

## 1. 개요

### 1.1 설계 목적
Product-Change Service의 상품변경 기능을 구현하기 위한 클래스 구조를 설계합니다.

### 1.2 설계 원칙
- **아키텍처 패턴**: Layered Architecture 적용
- **패키지 구조**: com.unicorn.phonebill.product 하위 계층별 구조
- **KOS 연동**: Circuit Breaker 패턴으로 외부 시스템 안정성 확보
- **캐시 전략**: Redis를 활용한 성능 최적화
- **예외 처리**: 계층별 예외 처리 및 비즈니스 예외 정의

### 1.3 주요 기능
- UFR-PROD-010: 상품변경 메뉴 조회
- UFR-PROD-020: 상품변경 화면 데이터 조회  
- UFR-PROD-030: 상품변경 요청 및 사전체크
- UFR-PROD-040: KOS 연동 상품변경 처리

## 2. 패키지 구조도

```
com.unicorn.phonebill.product
├── controller/                    # 컨트롤러 계층
│   └── ProductController         # 상품변경 API 컨트롤러
├── dto/                          # 데이터 전송 객체
│   ├── *Request                  # 요청 DTO 클래스들
│   ├── *Response                 # 응답 DTO 클래스들
│   └── *Enum                     # DTO 관련 열거형
├── service/                      # 서비스 계층
│   ├── ProductService           # 상품변경 서비스 인터페이스
│   ├── ProductServiceImpl       # 상품변경 서비스 구현체
│   ├── ProductValidationService # 상품변경 검증 서비스
│   ├── ProductCacheService      # 상품 캐시 서비스
│   ├── KosClientService         # KOS 연동 서비스
│   ├── CircuitBreakerService    # Circuit Breaker 서비스
│   └── RetryService             # 재시도 서비스
├── domain/                       # 도메인 계층
│   ├── Product                  # 상품 도메인 모델
│   ├── ProductChangeHistory     # 상품변경 이력 도메인 모델
│   ├── ProductChangeResult      # 상품변경 결과 도메인 모델
│   └── ProductStatus            # 상품 상태 도메인 모델
├── repository/                   # 저장소 계층
│   ├── ProductRepository        # 상품 저장소 인터페이스
│   ├── ProductChangeHistoryRepository # 상품변경 이력 저장소 인터페이스
│   ├── entity/                  # JPA 엔티티
│   │   └── ProductChangeHistoryEntity
│   └── jpa/                     # JPA Repository
│       └── ProductChangeHistoryJpaRepository
├── config/                       # 설정 계층
│   ├── RestTemplateConfig       # REST 통신 설정
│   ├── CacheConfig             # 캐시 설정
│   ├── CircuitBreakerConfig    # Circuit Breaker 설정
│   └── KosProperties           # KOS 연동 설정
├── external/                     # 외부 연동 계층
│   ├── KosRequest              # KOS 요청 모델
│   ├── KosResponse             # KOS 응답 모델
│   └── KosAdapterService       # KOS 어댑터 서비스
└── exception/                    # 예외 계층
    ├── ProductChangeException   # 상품변경 예외
    ├── ProductValidationException # 상품변경 검증 예외
    ├── KosConnectionException   # KOS 연결 예외
    └── CircuitBreakerException  # Circuit Breaker 예외
```

## 3. 계층별 클래스 설계

### 3.1 Controller Layer

#### ProductController
- **역할**: 상품변경 관련 REST API 엔드포인트 제공
- **주요 메소드**:
  - `getProductMenu()`: 상품변경 메뉴 조회 (GET /products/menu)
  - `getCustomerInfo(lineNumber)`: 고객 정보 조회 (GET /products/customer/{lineNumber})
  - `getAvailableProducts()`: 변경 가능한 상품 목록 조회 (GET /products/available)
  - `validateProductChange(request)`: 상품변경 사전체크 (POST /products/change/validation)
  - `requestProductChange(request)`: 상품변경 요청 (POST /products/change)
  - `getProductChangeResult(requestId)`: 상품변경 결과 조회 (GET /products/change/{requestId})
  - `getProductChangeHistory()`: 상품변경 이력 조회 (GET /products/history)

### 3.2 Service Layer

#### ProductService / ProductServiceImpl
- **역할**: 상품변경 비즈니스 로직 처리
- **의존성**: KosClientService, ProductValidationService, ProductCacheService, ProductChangeHistoryRepository
- **주요 기능**: 상품변경 프로세스 전체 조율, 캐시 무효화 처리

#### ProductValidationService
- **역할**: 상품변경 사전체크 로직 처리
- **주요 검증**: 판매중인 상품 확인, 사업자 일치 확인, 회선 사용상태 확인
- **의존성**: ProductRepository, ProductCacheService, KosClientService

#### ProductCacheService  
- **역할**: Redis 캐시를 활용한 성능 최적화
- **주요 캐시**: 고객상품정보(4시간), 현재상품정보(2시간), 가용상품목록(24시간), 상품상태(1시간), 회선상태(30분)
- **캐시 키 전략**: `{cache_type}:{identifier}` 형식

#### KosClientService
- **역할**: KOS 시스템과의 연동 처리
- **의존성**: CircuitBreakerService, RetryService, KosAdapterService
- **주요 기능**: KOS API 호출, Circuit Breaker 상태 관리, 재시도 로직

#### CircuitBreakerService / RetryService
- **역할**: 외부 시스템 연동 안정성 보장
- **패턴**: Circuit Breaker, Retry 패턴 적용
- **설정**: 실패율 임계값, 재시도 횟수, 대기 시간 등

### 3.3 Domain Layer

#### Product
- **역할**: 상품 정보 도메인 모델
- **주요 속성**: productCode, productName, monthlyFee, dataAllowance, voiceAllowance, smsAllowance, status, operatorCode
- **비즈니스 메소드**: `canChangeTo()`, `isSameOperator()`

#### ProductChangeHistory
- **역할**: 상품변경 이력 도메인 모델
- **주요 속성**: requestId, userId, lineNumber, currentProductCode, targetProductCode, processStatus, requestedAt, processedAt
- **상태 관리**: `markAsCompleted()`, `markAsFailed()`

#### ProductChangeResult
- **역할**: 상품변경 처리 결과 도메인 모델
- **팩토리 메소드**: `createSuccessResult()`, `createFailureResult()`

### 3.4 Repository Layer

#### ProductRepository
- **역할**: 상품 데이터 접근 인터페이스
- **주요 메소드**: 상품상태 조회, 상품변경 요청 저장, 상태 업데이트

#### ProductChangeHistoryRepository
- **역할**: 상품변경 이력 데이터 접근 인터페이스  
- **JPA Repository**: ProductChangeHistoryJpaRepository 활용
- **Entity**: ProductChangeHistoryEntity (BaseTimeEntity 상속)

### 3.5 Config Layer

#### RestTemplateConfig
- **역할**: REST 통신 설정
- **설정 요소**: Connection Pool, Timeout, HTTP Client 설정

#### CacheConfig
- **역할**: Redis 캐시 설정
- **설정 요소**: Redis 연결, Cache Manager, 직렬화 설정

#### CircuitBreakerConfig  
- **역할**: Circuit Breaker 및 Retry 설정
- **설정 요소**: 실패율 임계값, 최소 호출 수, 대기 시간

#### KosProperties
- **역할**: KOS 연동 설정 프로퍼티
- **설정 요소**: baseUrl, connectTimeout, readTimeout, maxRetries, retryDelay

### 3.6 External Layer

#### KosAdapterService
- **역할**: KOS 시스템 연동 어댑터
- **주요 기능**: KOS API 호출, 요청/응답 데이터 변환, HTTP 헤더 설정
- **의존성**: KosProperties, RestTemplate

#### KosRequest / KosResponse
- **역할**: KOS 시스템 연동을 위한 요청/응답 모델
- **변환**: 내부 도메인 모델 ↔ KOS API 모델

### 3.7 Exception Layer

#### ProductChangeException
- **역할**: 상품변경 관련 비즈니스 예외
- **상속**: BusinessException 상속

#### ProductValidationException  
- **역할**: 상품변경 검증 실패 예외
- **추가 정보**: 검증 상세 정보 목록 포함

#### KosConnectionException
- **역할**: KOS 연동 관련 예외
- **추가 정보**: 연동 서비스명 포함

#### CircuitBreakerException
- **역할**: Circuit Breaker Open 상태 예외
- **추가 정보**: 서비스명, 상태 정보 포함

## 4. 주요 설계 특징

### 4.1 Layered Architecture 적용
- **Controller**: API 엔드포인트 및 HTTP 요청/응답 처리
- **Service**: 비즈니스 로직 처리 및 트랜잭션 관리  
- **Domain**: 핵심 비즈니스 모델 및 도메인 규칙
- **Repository**: 데이터 접근 및 영속성 관리

### 4.2 캐시 전략
- **다층 캐시**: Redis를 활용한 성능 최적화
- **TTL 차등 적용**: 데이터 특성에 따른 캐시 수명 관리
- **캐시 무효화**: 상품변경 완료 시 관련 캐시 제거

### 4.3 외부 연동 안정성
- **Circuit Breaker**: KOS 시스템 장애 시 빠른 실패 처리
- **Retry**: 일시적 네트워크 오류에 대한 재시도 로직
- **Timeout**: 응답 시간 초과 방지

### 4.4 예외 처리 전략
- **계층별 예외**: 각 계층의 책임에 맞는 예외 정의
- **비즈니스 예외**: 도메인 규칙 위반에 대한 명확한 예외
- **인프라 예외**: 외부 시스템 연동 실패에 대한 예외

## 5. API와 클래스 매핑

| API 엔드포인트 | HTTP Method | Controller 메소드 | 주요 Service |
|---|---|---|---|
| `/products/menu` | GET | `getProductMenu()` | ProductService |
| `/products/customer/{lineNumber}` | GET | `getCustomerInfo()` | ProductService, ProductCacheService |
| `/products/available` | GET | `getAvailableProducts()` | ProductService, ProductCacheService |
| `/products/change/validation` | POST | `validateProductChange()` | ProductValidationService |
| `/products/change` | POST | `requestProductChange()` | ProductService, KosClientService |
| `/products/change/{requestId}` | GET | `getProductChangeResult()` | ProductService |
| `/products/history` | GET | `getProductChangeHistory()` | ProductService, ProductChangeHistoryRepository |

## 6. 시퀀스와 클래스 연관관계

### 6.1 상품변경 요청 시퀀스 매핑
- **ProductController** → **ProductServiceImpl** → **ProductValidationService** → **KosClientService** → **KosAdapterService**
- **캐시 처리**: ProductCacheService를 통한 Redis 연동
- **이력 관리**: ProductChangeHistoryRepository를 통한 DB 저장

### 6.2 KOS 연동 시퀀스 매핑  
- **KosClientService** → **CircuitBreakerService** → **RetryService** → **KosAdapterService**
- **상태 관리**: ProductChangeHistory 도메인 모델을 통한 상태 추적
- **결과 처리**: ProductChangeResult를 통한 성공/실패 처리

## 7. 설계 파일

- **상세 클래스 설계**: [product-change.puml](./product-change.puml)
- **간단 클래스 설계**: [product-change-simple.puml](./product-change-simple.puml)

## 8. 관련 문서

- **API 설계서**: [product-change-service-api.yaml](../api/product-change-service-api.yaml)
- **내부 시퀀스 설계서**: 
  - [product-상품변경요청.puml](../sequence/inner/product-상품변경요청.puml)
  - [product-KOS연동.puml](../sequence/inner/product-KOS연동.puml)
- **유저스토리**: [userstory.md](../../userstory.md)
- **공통 기반 클래스**: [common-base.puml](./common-base.puml)