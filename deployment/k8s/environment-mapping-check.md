# 실행 프로파일 환경변수 매핑 테이블

## User Service 환경변수 매핑
| 서비스명 | 환경변수 | 지정 객체명 | 환경변수값 |
|---------|----------|-------------|------------|
| user-service | CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://phonebill.20.214.196.128.nip.io |
| user-service | DB_HOST | secret-user-service | auth-postgres-dev-postgresql |
| user-service | DB_KIND | cm-user-service | postgresql |
| user-service | DB_NAME | cm-user-service | phonebill_auth |
| user-service | DB_PASSWORD | secret-user-service | AuthUser2025! |
| user-service | DB_PORT | cm-user-service | 5432 |
| user-service | DB_USERNAME | cm-user-service | auth_user |
| user-service | DDL_AUTO | cm-user-service | update |
| user-service | JWT_ACCESS_TOKEN_VALIDITY | cm-common | 18000000 |
| user-service | JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400000 |
| user-service | JWT_SECRET | secret-common | lJZLB9WK5+6q3/Ob4m5MvLUqttA6qq/FPmBXX71PbzE= |
| user-service | REDIS_DATABASE | cm-user-service | 0 |
| user-service | REDIS_HOST | cm-common | redis-cache-dev-master |
| user-service | REDIS_PASSWORD | secret-common | Redis2025Dev! |
| user-service | REDIS_PORT | cm-common | 6379 |
| user-service | SERVER_PORT | cm-user-service | 8081 |
| user-service | SHOW_SQL | cm-user-service | true |
| user-service | SPRING_PROFILES_ACTIVE | cm-common | dev |

## Bill Service 환경변수 매핑
| 서비스명 | 환경변수 | 지정 객체명 | 환경변수값 |
|---------|----------|-------------|------------|
| bill-service | CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://phonebill.20.214.196.128.nip.io |
| bill-service | DB_CONNECTION_TIMEOUT | cm-bill-service | 30000 |
| bill-service | DB_HOST | secret-bill-service | bill-inquiry-postgres-dev-postgresql |
| bill-service | DB_IDLE_TIMEOUT | cm-bill-service | 600000 |
| bill-service | DB_KIND | cm-bill-service | postgresql |
| bill-service | DB_LEAK_DETECTION | cm-bill-service | 60000 |
| bill-service | DB_MAX_LIFETIME | cm-bill-service | 1800000 |
| bill-service | DB_MAX_POOL | cm-bill-service | 20 |
| bill-service | DB_MIN_IDLE | cm-bill-service | 5 |
| bill-service | DB_NAME | cm-bill-service | bill_inquiry_db |
| bill-service | DB_PASSWORD | secret-bill-service | BillUser2025! |
| bill-service | DB_PORT | cm-bill-service | 5432 |
| bill-service | DB_USERNAME | cm-bill-service | bill_inquiry_user |
| bill-service | JWT_ACCESS_TOKEN_VALIDITY | cm-common | 18000000 |
| bill-service | JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400000 |
| bill-service | JWT_SECRET | secret-common | lJZLB9WK5+6q3/Ob4m5MvLUqttA6qq/FPmBXX71PbzE= |
| bill-service | KOS_BASE_URL | cm-bill-service | http://kos-mock:80 |
| bill-service | LOG_FILE_NAME | cm-bill-service | logs/bill-service.log |
| bill-service | REDIS_DATABASE | cm-bill-service | 1 |
| bill-service | REDIS_HOST | cm-common | redis-cache-dev-master |
| bill-service | REDIS_MAX_ACTIVE | cm-bill-service | 8 |
| bill-service | REDIS_MAX_IDLE | cm-bill-service | 8 |
| bill-service | REDIS_MAX_WAIT | cm-bill-service | -1 |
| bill-service | REDIS_MIN_IDLE | cm-bill-service | 0 |
| bill-service | REDIS_PASSWORD | secret-common | Redis2025Dev! |
| bill-service | REDIS_PORT | cm-common | 6379 |
| bill-service | REDIS_TIMEOUT | cm-bill-service | 2000 |
| bill-service | SERVER_PORT | cm-bill-service | 8082 |
| bill-service | SPRING_PROFILES_ACTIVE | cm-common | dev |

## Product Service 환경변수 매핑
| 서비스명 | 환경변수 | 지정 객체명 | 환경변수값 |
|---------|----------|-------------|------------|
| product-service | CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://phonebill.20.214.196.128.nip.io |
| product-service | DB_HOST | secret-product-service | product-change-postgres-dev-postgresql |
| product-service | DB_KIND | cm-product-service | postgresql |
| product-service | DB_NAME | cm-product-service | product_change_db |
| product-service | DB_PASSWORD | secret-product-service | ProductUser2025! |
| product-service | DB_PORT | cm-product-service | 5432 |
| product-service | DB_USERNAME | cm-product-service | product_change_user |
| product-service | DDL_AUTO | cm-product-service | update |
| product-service | JWT_ACCESS_TOKEN_VALIDITY | cm-common | 18000000 |
| product-service | JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400000 |
| product-service | JWT_SECRET | secret-common | lJZLB9WK5+6q3/Ob4m5MvLUqttA6qq/FPmBXX71PbzE= |
| product-service | KOS_API_KEY | cm-product-service | dev-api-key |
| product-service | KOS_BASE_URL | cm-product-service | http://kos-mock:80 |
| product-service | KOS_CLIENT_ID | cm-product-service | product-service-dev |
| product-service | KOS_MOCK_ENABLED | cm-product-service | true |
| product-service | REDIS_DATABASE | cm-product-service | 2 |
| product-service | REDIS_HOST | cm-common | redis-cache-dev-master |
| product-service | REDIS_PASSWORD | secret-common | Redis2025Dev! |
| product-service | REDIS_PORT | cm-common | 6379 |
| product-service | SERVER_PORT | cm-product-service | 8083 |
| product-service | SPRING_PROFILES_ACTIVE | cm-common | dev |

## API Gateway 환경변수 매핑
| 서비스명 | 환경변수 | 지정 객체명 | 환경변수값 |
|---------|----------|-------------|------------|
| api-gateway | BILL_SERVICE_URL | cm-api-gateway | http://bill-service:80 |
| api-gateway | CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://phonebill.20.214.196.128.nip.io |
| api-gateway | JWT_ACCESS_TOKEN_VALIDITY | cm-common | 18000000 |
| api-gateway | JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400000 |
| api-gateway | JWT_SECRET | secret-common | lJZLB9WK5+6q3/Ob4m5MvLUqttA6qq/FPmBXX71PbzE= |
| api-gateway | KOS_MOCK_URL | cm-api-gateway | http://kos-mock:80 |
| api-gateway | PRODUCT_SERVICE_URL | cm-api-gateway | http://product-service:80 |
| api-gateway | SERVER_PORT | cm-api-gateway | 8080 |
| api-gateway | SPRING_PROFILES_ACTIVE | cm-common | dev |
| api-gateway | USER_SERVICE_URL | cm-api-gateway | http://user-service:80 |

## KOS Mock 환경변수 매핑
| 서비스명 | 환경변수 | 지정 객체명 | 환경변수값 |
|---------|----------|-------------|------------|
| kos-mock | SERVER_PORT | cm-kos-mock | 8084 |
| kos-mock | SPRING_PROFILES_ACTIVE | cm-common | dev |

## 검증 결과
✅ 모든 실행프로파일의 환경변수가 매니페스트에 매핑되었습니다.