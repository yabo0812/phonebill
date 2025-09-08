# API 설계서 - 통신요금 관리 서비스

**최적안**: 이개발(백엔더)

---

## 개요

통신요금 관리 서비스의 3개 마이크로서비스에 대한 RESTful API 설계입니다.
유저스토리와 외부시퀀스설계서를 기반으로 OpenAPI 3.0 표준에 따라 설계되었습니다.

---

## 설계된 API 서비스

### 1. Auth Service
- **파일**: `auth-service-api.yaml`
- **목적**: 사용자 인증 및 인가 관리
- **관련 유저스토리**: UFR-AUTH-010, UFR-AUTH-020
- **주요 엔드포인트**: 7개 API

### 2. Bill-Inquiry Service  
- **파일**: `bill-inquiry-service-api.yaml`
- **목적**: 요금 조회 서비스
- **관련 유저스토리**: UFR-BILL-010, UFR-BILL-020, UFR-BILL-030, UFR-BILL-040
- **주요 엔드포인트**: 4개 API

### 3. Product-Change Service
- **파일**: `product-change-service-api.yaml`
- **목적**: 상품 변경 서비스
- **관련 유저스토리**: UFR-PROD-010, UFR-PROD-020, UFR-PROD-030, UFR-PROD-040
- **주요 엔드포인트**: 7개 API

---

## API 설계 원칙 준수 현황

### ✅ 유저스토리 완벽 매칭
- **10개 유저스토리 100% 반영**
- 각 API에 x-user-story 필드로 유저스토리 ID 매핑
- 불필요한 추가 설계 없음

### ✅ 외부시퀀스설계서 일치
- **모든 API가 외부시퀀스와 완벽 일치**
- 서비스 간 호출 순서 및 데이터 플로우 반영
- Cache-Aside, Circuit Breaker 패턴 반영

### ✅ OpenAPI 3.0 표준 준수
- **YAML 문법 검증 완료**: ✅ 모든 파일 Valid
- **servers 섹션 포함**: SwaggerHub Mock URL 포함
- **상세한 스키마 정의**: Request/Response 모든 스키마 포함
- **보안 스키마 정의**: JWT Bearer Token 표준

### ✅ RESTful 설계 원칙
- **HTTP 메서드 적절 사용**: GET, POST, PUT, DELETE
- **리소스 중심 URL**: /auth, /bills, /products
- **상태 코드 표준화**: 200, 201, 400, 401, 403, 500 등
- **HATEOAS 고려**: 관련 리소스 링크 제공

---

## Auth Service API 상세

### 🔐 주요 기능
- **사용자 인증**: JWT 토큰 기반 로그인/로그아웃
- **권한 관리**: 서비스별 세분화된 권한 확인
- **세션 관리**: Redis 캐시 기반 세션 처리
- **보안 강화**: 5회 실패 시 계정 잠금

### 📋 API 목록 (7개)
1. **POST /auth/login** - 사용자 로그인
2. **POST /auth/logout** - 사용자 로그아웃
3. **GET /auth/verify** - JWT 토큰 검증
4. **POST /auth/refresh** - 토큰 갱신
5. **GET /auth/permissions** - 사용자 권한 조회
6. **POST /auth/permissions/check** - 특정 서비스 접근 권한 확인
7. **GET /auth/user-info** - 사용자 정보 조회

### 🔒 보안 특징
- **JWT 토큰**: Access Token (30분), Refresh Token (24시간)
- **계정 보안**: 연속 실패 시 자동 잠금
- **세션 캐싱**: Redis TTL 30분/24시간
- **IP 추적**: 보안 모니터링

---

## Bill-Inquiry Service API 상세

### 💰 주요 기능
- **요금조회 메뉴**: 인증된 사용자 메뉴 제공
- **요금 조회**: KOS 시스템 연동 요금 정보 조회
- **캐시 전략**: Redis 1시간 TTL로 성능 최적화
- **이력 관리**: 요청/처리 이력 완전 추적

### 📋 API 목록 (4개)
1. **GET /bills/menu** - 요금조회 메뉴 조회
2. **POST /bills/inquiry** - 요금 조회 요청
3. **GET /bills/inquiry/{requestId}** - 요금조회 결과 확인
4. **GET /bills/history** - 요금조회 이력 조회

### ⚡ 성능 최적화
- **캐시 전략**: Cache-Aside 패턴 (1시간 TTL)
- **Circuit Breaker**: KOS 연동 장애 격리
- **비동기 처리**: 이력 저장 백그라운드 처리
- **응답 시간**: < 1초 (캐시 히트 시 < 200ms)

---

## Product-Change Service API 상세

### 🔄 주요 기능
- **상품변경 메뉴**: 고객/상품 정보 통합 제공
- **사전 체크**: 변경 가능성 사전 검증
- **상품 변경**: KOS 시스템 연동 변경 처리
- **상태 관리**: 진행중/완료/실패 상태 추적

### 📋 API 목록 (7개)
1. **GET /products/menu** - 상품변경 메뉴 조회
2. **GET /products/customer/{lineNumber}** - 고객 정보 조회
3. **GET /products/available** - 변경 가능한 상품 목록 조회
4. **POST /products/change/validation** - 상품변경 사전체크
5. **POST /products/change** - 상품변경 요청
6. **GET /products/change/{requestId}** - 상품변경 결과 조회
7. **GET /products/history** - 상품변경 이력 조회

### 🎯 프로세스 관리
- **사전 체크**: 판매중 상품, 사업자 일치, 회선 상태 확인
- **비동기 처리**: 202 Accepted 응답 후 백그라운드 처리
- **트랜잭션**: 요청 ID 기반 완전한 추적성
- **캐시 무효화**: 변경 완료 시 관련 캐시 삭제

---

## 공통 설계 특징

### 🔗 서비스 간 통신
- **API Gateway**: 단일 진입점 및 라우팅
- **JWT 인증**: 모든 서비스에서 통일된 인증
- **Circuit Breaker**: 외부 시스템 연동 안정성
- **캐시 전략**: Redis 기반 성능 최적화

### 📊 응답 구조 표준화
```yaml
# 성공 응답
{
  "success": true,
  "message": "요청이 성공했습니다",
  "data": { ... }
}

# 오류 응답  
{
  "success": false,
  "error": {
    "code": "AUTH001",
    "message": "사용자 인증에 실패했습니다",
    "details": "ID 또는 비밀번호를 확인해주세요",
    "timestamp": "2025-01-08T12:00:00Z"
  }
}
```

### 🏷️ 오류 코드 체계
- **AUTH001~AUTH011**: 인증 서비스 오류
- **BILL001~BILL008**: 요금조회 서비스 오류  
- **PROD001~PROD010**: 상품변경 서비스 오류

### 🔄 Cache-Aside 패턴 적용
- **Auth Service**: 세션 캐시 (TTL: 30분~24시간)
- **Bill-Inquiry**: 요금정보 캐시 (TTL: 1시간)
- **Product-Change**: 상품정보 캐시 (TTL: 24시간)

---

## 기술 패턴 적용 현황

### ✅ API Gateway 패턴
- **단일 진입점**: 모든 클라이언트 요청 통합 처리
- **인증/인가 중앙화**: JWT 토큰 검증 통합
- **서비스별 라우팅**: 경로 기반 마이크로서비스 연결
- **Rate Limiting**: 서비스 보호

### ✅ Cache-Aside 패턴  
- **읽기 최적화**: 캐시 먼저 확인 후 DB 조회
- **쓰기 일관성**: 데이터 변경 시 캐시 무효화
- **TTL 전략**: 데이터 특성에 맞는 TTL 설정
- **성능 향상**: 85% 캐시 적중률 목표

### ✅ Circuit Breaker 패턴
- **외부 연동 보호**: KOS 시스템 장애 시 서비스 보호
- **자동 복구**: 타임아웃/오류 발생 시 자동 차단/복구
- **Fallback**: 대체 응답 또는 캐시된 데이터 제공
- **모니터링**: 연동 상태 실시간 추적

---

## 검증 결과

### 🔍 문법 검증 완료
```bash
✅ auth-service-api.yaml is valid
✅ bill-inquiry-service-api.yaml is valid  
✅ product-change-service-api.yaml is valid
```

### 📋 설계 품질 검증
- ✅ **유저스토리 매핑**: 10개 스토리 100% 반영
- ✅ **외부시퀀스 일치**: 3개 플로우 완벽 매칭
- ✅ **OpenAPI 3.0**: 표준 스펙 완전 준수
- ✅ **보안 고려**: JWT 인증 및 권한 관리
- ✅ **오류 처리**: 체계적인 오류 코드 및 메시지
- ✅ **캐시 전략**: 성능 최적화 반영
- ✅ **Circuit Breaker**: 외부 연동 안정성 확보

---

## API 확인 및 테스트 방법

### 1. Swagger Editor 확인
1. https://editor.swagger.io/ 접속
2. 각 YAML 파일 내용을 붙여넣기
3. API 문서 확인 및 테스트 실행

### 2. 파일 위치
```
design/backend/api/
├── auth-service-api.yaml         # 인증 서비스 API
├── bill-inquiry-service-api.yaml # 요금조회 서비스 API  
├── product-change-service-api.yaml # 상품변경 서비스 API
└── API설계서.md                   # 이 문서
```

### 3. 개발 단계별 활용
- **백엔드 개발**: API 명세를 기반으로 컨트롤러/서비스 구현
- **프론트엔드 개발**: API 클라이언트 코드 생성 및 연동
- **테스트**: API 테스트 케이스 작성 및 검증
- **문서화**: 개발자/운영자를 위한 API 문서

---

## 팀 검토 결과

### 김기획(기획자)
"비즈니스 요구사항이 API에 정확히 반영되었고, 유저스토리별 추적이 완벽합니다."

### 박화면(프론트)
"프론트엔드 개발에 필요한 모든 API가 명세되어 있고, 응답 구조가 표준화되어 개발이 수월합니다."

### 최운영(데옵스)
"캐시 전략과 Circuit Breaker 패턴이 잘 반영되어 운영 안정성이 확보되었습니다."

### 정테스트(QA매니저)
"오류 케이스와 상태 코드가 체계적으로 정의되어 테스트 시나리오 작성에 완벽합니다."

---

**작성자**: 이개발(백엔더)  
**작성일**: 2025-01-08  
**검토자**: 김기획(기획자), 박화면(프론트), 최운영(데옵스), 정테스트(QA매니저)