# Production Overlay Configuration

This directory contains the Kustomize overlay configuration for the production environment of the phonebill project.

## Configuration Overview

### Environment Details
- **Namespace**: `phonebill-prod`
- **Environment**: Production
- **Replicas**: 3 (for all services)
- **Domain**: `phonebill.yourdomain.com`
- **Image Tag**: `prod-latest`
- **SSL**: Enabled with HTTPS redirect

### Security Configuration
- **JWT Access Token**: 30분 (1800000ms) - 보안 강화를 위한 짧은 만료시간
- **JWT Refresh Token**: 12시간 (43200000ms)
- **DDL Auto**: `validate` - 프로덕션 안전성을 위한 스키마 검증 모드
- **SSL Redirect**: 강제 HTTPS 리디렉션

### Resource Allocation
All services are configured with:
- **Requests**: 1024m CPU, 1024Mi Memory
- **Limits**: 4096m CPU, 4096Mi Memory

### Health Checks
- **Liveness Probe**: 2분 초기 지연, 30초 간격
- **Readiness Probe**: 1분 초기 지연, 10초 간격

## Files Structure

```
prod/
├── kustomization.yaml                    # 메인 오버레이 설정
├── configmap-common-patch.yaml          # 공통 설정 (프로덕션 프로파일)
├── secret-common-patch.yaml             # 공통 시크릿 (JWT, Redis)
├── ingress-patch.yaml                   # HTTPS 인그레스 설정
├── deployment-api-gateway-patch.yaml    # API Gateway 배포 설정
├── deployment-user-service-patch.yaml   # 사용자 서비스 배포 설정
├── deployment-bill-service-patch.yaml   # 요금조회 서비스 배포 설정
├── deployment-product-service-patch.yaml# 상품변경 서비스 배포 설정
├── deployment-kos-mock-patch.yaml       # KOS Mock 배포 설정
├── secret-user-service-patch.yaml       # 사용자 서비스 DB 연결정보
├── secret-bill-service-patch.yaml       # 요금조회 서비스 DB 연결정보
└── secret-product-service-patch.yaml    # 상품변경 서비스 DB 연결정보
```

## Deployment Command

```bash
# Apply production configuration
kubectl apply -k deployment/cicd/kustomize/overlays/prod/

# Validate configuration before applying
kubectl kustomize deployment/cicd/kustomize/overlays/prod/
```

## Important Notes

1. **Secret Values**: 모든 시크릿 값들은 실제 프로덕션 환경에 맞게 변경해야 합니다.
2. **Domain Configuration**: `phonebill.yourdomain.com`을 실제 도메인으로 변경하세요.
3. **Certificate**: SSL 인증서 설정을 위해 cert-manager가 구성되어 있어야 합니다.
4. **Database**: 각 서비스별 전용 데이터베이스 인스턴스가 필요합니다.
5. **Monitoring**: 프로덕션 환경에서는 모니터링 및 로깅 설정이 중요합니다.

## Database Services Required

프로덕션 환경에서는 다음 데이터베이스 서비스들이 필요합니다:
- `auth-postgres-prod-service` (사용자 서비스)
- `bill-inquiry-postgres-prod-service` (요금조회 서비스)
- `product-change-postgres-prod-service` (상품변경 서비스)
- `redis-prod-service` (공통 캐시)