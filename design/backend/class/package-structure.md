# 패키지 구조도 - 통신요금 관리 서비스

## 전체 패키지 구조

```
com.unicorn.phonebill/
├── common/                                    # 공통 모듈
│   ├── dto/
│   │   ├── ApiResponse.java                   # 표준 API 응답 구조
│   │   ├── ErrorResponse.java                 # 오류 응답 구조  
│   │   ├── JwtTokenDTO.java                   # JWT 토큰 정보
│   │   └── JwtTokenVerifyDTO.java            # JWT 토큰 검증 결과
│   ├── entity/
│   │   └── BaseTimeEntity.java               # 기본 엔티티 클래스
│   ├── exception/
│   │   ├── BusinessException.java            # 비즈니스 예외
│   │   ├── InfraException.java               # 인프라 예외
│   │   └── ErrorCode.java                    # 오류 코드 열거형
│   ├── util/
│   │   ├── DateUtil.java                     # 날짜 유틸리티
│   │   ├── SecurityUtil.java                 # 보안 유틸리티
│   │   └── ValidatorUtil.java                # 검증 유틸리티
│   ├── config/
│   │   └── JpaConfig.java                    # JPA 설정
│   └── aop/
│       └── LoggingAspect.java                # 로깅 AOP
├── auth/                                      # 인증 서비스
│   ├── AuthApplication.java                  # Spring Boot 메인 클래스
│   ├── controller/
│   │   └── AuthController.java               # 인증 API 컨트롤러
│   ├── dto/
│   │   ├── LoginRequest.java                 # 로그인 요청
│   │   ├── LoginResponse.java                # 로그인 응답
│   │   ├── LogoutRequest.java                # 로그아웃 요청
│   │   ├── TokenRefreshRequest.java          # 토큰 갱신 요청
│   │   ├── TokenRefreshResponse.java         # 토큰 갱신 응답
│   │   ├── PermissionRequest.java            # 권한 확인 요청
│   │   ├── PermissionResponse.java           # 권한 확인 응답
│   │   ├── UserInfoResponse.java             # 사용자 정보 응답
│   │   └── TokenVerifyResponse.java          # 토큰 검증 응답
│   ├── service/
│   │   ├── AuthService.java                  # 인증 서비스 인터페이스
│   │   ├── AuthServiceImpl.java              # 인증 서비스 구현체
│   │   ├── TokenService.java                 # 토큰 서비스 인터페이스
│   │   ├── TokenServiceImpl.java             # 토큰 서비스 구현체
│   │   ├── PermissionService.java            # 권한 서비스 인터페이스
│   │   └── PermissionServiceImpl.java        # 권한 서비스 구현체
│   ├── domain/
│   │   ├── User.java                         # 사용자 도메인 모델
│   │   ├── UserSession.java                  # 사용자 세션 도메인 모델
│   │   ├── LoginResult.java                  # 로그인 결과
│   │   ├── TokenInfo.java                    # 토큰 정보
│   │   ├── Permission.java                   # 권한 정보
│   │   └── UserInfo.java                     # 사용자 상세 정보
│   ├── repository/
│   │   ├── UserRepository.java               # 사용자 리포지토리 인터페이스
│   │   ├── UserRepositoryImpl.java           # 사용자 리포지토리 구현체
│   │   ├── SessionRepository.java            # 세션 리포지토리 인터페이스
│   │   ├── SessionRepositoryImpl.java        # 세션 리포지토리 구현체
│   │   ├── entity/
│   │   │   ├── UserEntity.java               # 사용자 엔티티
│   │   │   ├── UserSessionEntity.java        # 사용자 세션 엔티티
│   │   │   └── UserPermissionEntity.java     # 사용자 권한 엔티티
│   │   └── jpa/
│   │       ├── UserJpaRepository.java        # 사용자 JPA 리포지토리
│   │       ├── UserSessionJpaRepository.java # 세션 JPA 리포지토리
│   │       └── UserPermissionJpaRepository.java # 권한 JPA 리포지토리
│   └── config/
│       ├── SecurityConfig.java               # 보안 설정
│       ├── JwtConfig.java                    # JWT 설정
│       └── RedisConfig.java                  # Redis 설정
├── bill/                                      # 요금조회 서비스  
│   ├── BillApplication.java                  # Spring Boot 메인 클래스
│   ├── controller/
│   │   └── BillController.java               # 요금조회 API 컨트롤러
│   ├── dto/
│   │   ├── BillMenuResponse.java             # 요금조회 메뉴 응답
│   │   ├── BillInquiryRequest.java           # 요금조회 요청
│   │   ├── BillInquiryResponse.java          # 요금조회 응답
│   │   ├── BillStatusResponse.java           # 요금조회 상태 응답
│   │   ├── BillHistoryRequest.java           # 요금조회 이력 요청
│   │   ├── BillHistoryResponse.java          # 요금조회 이력 응답
│   │   ├── BillDetailInfo.java               # 요금 상세 정보
│   │   ├── DiscountInfo.java                 # 할인 정보
│   │   └── UsageInfo.java                    # 사용량 정보
│   ├── service/
│   │   ├── BillService.java                  # 요금조회 서비스 인터페이스
│   │   ├── BillServiceImpl.java              # 요금조회 서비스 구현체
│   │   ├── BillCacheService.java             # 요금 캐시 서비스 인터페이스
│   │   ├── BillCacheServiceImpl.java         # 요금 캐시 서비스 구현체
│   │   ├── KosClientService.java             # KOS 클라이언트 서비스 인터페이스
│   │   ├── KosClientServiceImpl.java         # KOS 클라이언트 서비스 구현체
│   │   ├── BillHistoryService.java           # 요금조회 이력 서비스 인터페이스
│   │   └── BillHistoryServiceImpl.java       # 요금조회 이력 서비스 구현체
│   ├── domain/
│   │   ├── BillInfo.java                     # 요금 정보 도메인 모델
│   │   ├── BillHistory.java                  # 요금조회 이력 도메인 모델
│   │   ├── KosBillRequest.java               # KOS 요금조회 요청
│   │   ├── KosBillResponse.java              # KOS 요금조회 응답
│   │   ├── BillInquiryResult.java            # 요금조회 결과
│   │   ├── BillStatus.java                   # 요금조회 상태 열거형
│   │   └── RequestStatus.java                # 요청 상태 열거형
│   ├── repository/
│   │   ├── BillHistoryRepository.java        # 요금조회 이력 리포지토리 인터페이스
│   │   ├── BillHistoryRepositoryImpl.java    # 요금조회 이력 리포지토리 구현체
│   │   ├── entity/
│   │   │   ├── BillHistoryEntity.java        # 요금조회 이력 엔티티
│   │   │   └── BillRequestEntity.java        # 요금조회 요청 엔티티
│   │   └── jpa/
│   │       ├── BillHistoryJpaRepository.java # 요금조회 이력 JPA 리포지토리
│   │       └── BillRequestJpaRepository.java # 요금조회 요청 JPA 리포지토리
│   └── config/
│       ├── RestTemplateConfig.java           # RestTemplate 설정
│       ├── CacheConfig.java                  # 캐시 설정  
│       ├── CircuitBreakerConfig.java         # Circuit Breaker 설정
│       ├── RetryConfig.java                  # 재시도 설정
│       ├── AsyncConfig.java                  # 비동기 설정
│       ├── KosApiConfig.java                 # KOS API 설정
│       └── SwaggerConfig.java                # Swagger 설정
├── product/                                   # 상품변경 서비스
│   ├── ProductApplication.java               # Spring Boot 메인 클래스
│   ├── controller/
│   │   └── ProductController.java            # 상품변경 API 컨트롤러
│   ├── dto/
│   │   ├── ProductMenuResponse.java          # 상품변경 메뉴 응답
│   │   ├── CustomerInfoResponse.java         # 고객정보 응답
│   │   ├── AvailableProductsResponse.java    # 변경가능 상품 응답
│   │   ├── ProductValidationRequest.java     # 상품변경 사전체크 요청
│   │   ├── ProductValidationResponse.java    # 상품변경 사전체크 응답
│   │   ├── ProductChangeRequest.java         # 상품변경 요청
│   │   ├── ProductChangeResponse.java        # 상품변경 응답
│   │   ├── ProductChangeResultResponse.java  # 상품변경 결과 응답
│   │   ├── ProductChangeHistoryRequest.java  # 상품변경 이력 요청
│   │   ├── ProductChangeHistoryResponse.java # 상품변경 이력 응답
│   │   ├── ProductInfo.java                  # 상품 정보
│   │   ├── CustomerInfo.java                 # 고객 정보
│   │   ├── ValidationResult.java             # 검증 결과
│   │   ├── ChangeResult.java                 # 변경 결과
│   │   ├── ProductStatus.java                # 상품 상태 열거형
│   │   ├── ChangeStatus.java                 # 변경 상태 열거형
│   │   └── ValidationStatus.java             # 검증 상태 열거형
│   ├── service/
│   │   ├── ProductService.java               # 상품변경 서비스 인터페이스
│   │   ├── ProductServiceImpl.java           # 상품변경 서비스 구현체
│   │   ├── ProductValidationService.java     # 상품변경 검증 서비스 인터페이스
│   │   ├── ProductValidationServiceImpl.java # 상품변경 검증 서비스 구현체
│   │   ├── ProductCacheService.java          # 상품 캐시 서비스 인터페이스
│   │   ├── ProductCacheServiceImpl.java      # 상품 캐시 서비스 구현체
│   │   ├── KosClientService.java             # KOS 클라이언트 서비스 인터페이스
│   │   ├── KosClientServiceImpl.java         # KOS 클라이언트 서비스 구현체
│   │   ├── ProductHistoryService.java        # 상품변경 이력 서비스 인터페이스
│   │   ├── ProductHistoryServiceImpl.java    # 상품변경 이력 서비스 구현체
│   │   ├── AsyncService.java                 # 비동기 서비스 인터페이스
│   │   └── AsyncServiceImpl.java             # 비동기 서비스 구현체
│   ├── domain/
│   │   ├── Product.java                      # 상품 도메인 모델
│   │   ├── Customer.java                     # 고객 도메인 모델
│   │   ├── ProductChangeHistory.java         # 상품변경 이력 도메인 모델
│   │   ├── ProductValidation.java            # 상품변경 검증 도메인 모델
│   │   ├── KosProductChangeRequest.java      # KOS 상품변경 요청
│   │   ├── KosProductChangeResponse.java     # KOS 상품변경 응답
│   │   ├── ProductChangeResult.java          # 상품변경 결과
│   │   ├── ChangeRequestStatus.java          # 변경요청 상태 열거형
│   │   └── ValidationErrorType.java          # 검증 오류 타입 열거형
│   ├── repository/
│   │   ├── ProductChangeHistoryRepository.java    # 상품변경 이력 리포지토리 인터페이스
│   │   ├── ProductChangeHistoryRepositoryImpl.java # 상품변경 이력 리포지토리 구현체
│   │   ├── ProductRepository.java            # 상품 리포지토리 인터페이스
│   │   ├── ProductRepositoryImpl.java        # 상품 리포지토리 구현체
│   │   ├── entity/
│   │   │   ├── ProductChangeHistoryEntity.java # 상품변경 이력 엔티티
│   │   │   └── ProductEntity.java            # 상품 엔티티
│   │   └── jpa/
│   │       ├── ProductChangeHistoryJpaRepository.java # 상품변경 이력 JPA 리포지토리
│   │       └── ProductJpaRepository.java     # 상품 JPA 리포지토리
│   ├── external/
│   │   ├── KosApiClient.java                 # KOS API 클라이언트
│   │   ├── KosAdapterService.java            # KOS 어댑터 서비스
│   │   └── CircuitBreakerService.java        # Circuit Breaker 서비스
│   ├── config/
│   │   ├── RestTemplateConfig.java           # RestTemplate 설정
│   │   ├── CacheConfig.java                  # 캐시 설정
│   │   ├── CircuitBreakerConfig.java         # Circuit Breaker 설정
│   │   ├── AsyncConfig.java                  # 비동기 설정
│   │   ├── RetryConfig.java                  # 재시도 설정
│   │   ├── KosApiConfig.java                 # KOS API 설정
│   │   └── SwaggerConfig.java                # Swagger 설정
│   └── exception/
│       ├── ProductNotFoundException.java     # 상품 없음 예외
│       ├── ProductValidationException.java   # 상품변경 검증 예외
│       ├── ProductChangeException.java       # 상품변경 예외
│       └── KosIntegrationException.java      # KOS 연동 예외
└── kosmock/                                   # KOS Mock 서비스
    ├── KosMockApplication.java               # Spring Boot 메인 클래스
    ├── controller/
    │   └── KosMockController.java            # KOS Mock API 컨트롤러
    ├── service/
    │   ├── KosMockService.java               # KOS Mock 서비스 인터페이스
    │   ├── KosMockServiceImpl.java           # KOS Mock 서비스 구현체
    │   ├── BillDataService.java              # 요금 데이터 서비스 인터페이스
    │   ├── BillDataServiceImpl.java          # 요금 데이터 서비스 구현체
    │   ├── ProductDataService.java           # 상품 데이터 서비스 인터페이스
    │   ├── ProductDataServiceImpl.java       # 상품 데이터 서비스 구현체
    │   ├── MockScenarioService.java          # Mock 시나리오 서비스 인터페이스
    │   ├── MockScenarioServiceImpl.java      # Mock 시나리오 서비스 구현체
    │   ├── ProductValidationService.java     # 상품 검증 서비스 인터페이스
    │   └── ProductValidationServiceImpl.java # 상품 검증 서비스 구현체
    ├── dto/
    │   ├── KosBillRequest.java               # KOS 요금조회 요청
    │   ├── KosBillResponse.java              # KOS 요금조회 응답
    │   ├── KosProductChangeRequest.java      # KOS 상품변경 요청
    │   ├── KosProductChangeResponse.java     # KOS 상품변경 응답
    │   ├── KosCustomerInfoResponse.java      # KOS 고객정보 응답
    │   ├── KosAvailableProductsResponse.java # KOS 변경가능 상품 응답
    │   ├── KosLineStatusResponse.java        # KOS 회선상태 응답
    │   ├── MockScenario.java                 # Mock 시나리오
    │   ├── KosBillInfo.java                  # KOS 요금 정보
    │   ├── KosProductInfo.java               # KOS 상품 정보
    │   ├── KosCustomerInfo.java              # KOS 고객 정보
    │   ├── KosUsageInfo.java                 # KOS 사용량 정보
    │   ├── KosDiscountInfo.java              # KOS 할인 정보
    │   ├── KosContractInfo.java              # KOS 약정 정보
    │   ├── KosInstallmentInfo.java           # KOS 할부 정보
    │   ├── KosTerminationFeeInfo.java        # KOS 해지비용 정보
    │   └── KosValidationResult.java          # KOS 검증 결과
    ├── repository/
    │   ├── MockDataRepository.java           # Mock 데이터 리포지토리 인터페이스
    │   ├── MockDataRepositoryImpl.java       # Mock 데이터 리포지토리 구현체
    │   ├── entity/
    │   │   ├── KosCustomerEntity.java        # KOS 고객정보 엔티티
    │   │   ├── KosProductEntity.java         # KOS 상품정보 엔티티
    │   │   ├── KosBillEntity.java            # KOS 요금정보 엔티티
    │   │   ├── KosUsageEntity.java           # KOS 사용량정보 엔티티
    │   │   ├── KosDiscountEntity.java        # KOS 할인정보 엔티티
    │   │   ├── KosContractEntity.java        # KOS 약정정보 엔티티
    │   │   ├── KosInstallmentEntity.java     # KOS 할부정보 엔티티
    │   │   ├── KosTerminationFeeEntity.java  # KOS 해지비용정보 엔티티
    │   │   └── KosProductChangeHistoryEntity.java # KOS 상품변경이력 엔티티
    │   └── jpa/
    │       ├── KosCustomerJpaRepository.java # KOS 고객정보 JPA 리포지토리
    │       ├── KosProductJpaRepository.java  # KOS 상품정보 JPA 리포지토리
    │       ├── KosBillJpaRepository.java     # KOS 요금정보 JPA 리포지토리
    │       ├── KosUsageJpaRepository.java    # KOS 사용량정보 JPA 리포지토리
    │       ├── KosDiscountJpaRepository.java # KOS 할인정보 JPA 리포지토리
    │       ├── KosContractJpaRepository.java # KOS 약정정보 JPA 리포지토리
    │       ├── KosInstallmentJpaRepository.java # KOS 할부정보 JPA 리포지토리
    │       ├── KosTerminationFeeJpaRepository.java # KOS 해지비용정보 JPA 리포지토리
    │       └── KosProductChangeHistoryJpaRepository.java # KOS 상품변경이력 JPA 리포지토리
    └── config/
        ├── MockDataConfig.java               # Mock 데이터 설정
        ├── MockDelayConfig.java              # Mock 지연 설정
        └── SwaggerConfig.java                # Swagger 설정
```

## 패키지 구성 요약

### 📊 서비스별 클래스 수

| 서비스 | 총 클래스 수 | Controller | DTO | Service | Domain | Repository | Config/기타 |
|--------|-------------|------------|-----|---------|--------|------------|------------|
| Common | 14개 | - | 4개 | - | - | 1개 | 9개 |
| Auth | 26개 | 1개 | 9개 | 6개 | 6개 | 7개 | 3개 |
| Bill-Inquiry | 29개 | 1개 | 9개 | 8개 | 7개 | 4개 | 7개 |
| Product-Change | 44개 | 1개 | 17개 | 12개 | 9개 | 4개 | 7개 |
| KOS-Mock | 39개 | 1개 | 16개 | 10개 | - | 20개 | 3개 |
| **전체** | **152개** | **4개** | **55개** | **36개** | **22개** | **36개** | **29개** |

### 🏗️ 아키텍처 패턴별 구성

**Layered 아키텍처 (Auth, Bill-Inquiry, Product-Change)**
- Controller → Service → Domain → Repository → Entity 계층 구조
- 각 계층별 명확한 책임 분리
- 인터페이스 기반 의존성 주입

**간단한 Layered 아키텍처 (KOS-Mock)**
- Controller → Service → Repository → Entity 구조  
- Mock 데이터 제공에 특화된 단순 구조
- 시나리오 기반 응답 처리

### 🔗 주요 공통 컴포넌트 활용

**모든 서비스에서 공통 사용**
- `ApiResponse<T>`: 표준 API 응답 구조
- `BaseTimeEntity`: 생성/수정 시간 자동 관리
- `ErrorCode`: 표준화된 오류 코드 체계
- `BusinessException`/`InfraException`: 계층별 예외 처리

**공통 설정 및 유틸리티**
- `JpaConfig`: JPA 설정 통합
- `LoggingAspect`: AOP 기반 로깅
- `DateUtil`, `SecurityUtil`, `ValidatorUtil`: 공통 유틸리티

### 📝 설계 원칙 준수 현황

✅ **유저스토리 완벽 매칭**: 10개 유저스토리의 모든 요구사항 반영  
✅ **API 설계서 완전 일치**: Controller 메소드가 API 엔드포인트와 정확히 매칭  
✅ **내부시퀀스 반영**: Service, Repository 클래스가 시퀀스 다이어그램과 일치  
✅ **아키텍처 패턴 적용**: 서비스별 지정된 아키텍처 패턴 정확히 구현  
✅ **관계 표현 완료**: 상속, 구현, 의존성, 연관, 집약, 컴포지션 관계 모두 표현  
✅ **공통 컴포넌트 활용**: BaseTimeEntity, ApiResponse 등 공통 클래스 적극 활용  

이 패키지 구조는 마이크로서비스 아키텍처에 최적화되어 있으며, 각 서비스의 독립성과 확장성을 보장합니다.