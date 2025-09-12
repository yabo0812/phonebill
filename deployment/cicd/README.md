# Phonebill CI/CD 디렉토리 구조

## 개요
이 디렉토리는 Phonebill 마이크로서비스의 CI/CD 파이프라인을 위한 Kustomize 기반 구조를 포함합니다.

## 디렉토리 구조

```
deployment/cicd/
├── kustomize/
│   ├── base/                     # Base 매니페스트 (환경 독립적)
│   │   ├── common/              # 공통 리소스 (Ingress, Secret 등)
│   │   ├── api-gateway/         # API Gateway 서비스
│   │   ├── user-service/        # 사용자 서비스
│   │   ├── bill-service/        # 요금 조회 서비스
│   │   ├── product-service/     # 상품 변경 서비스
│   │   ├── kos-mock/           # KOS Mock 서비스
│   │   └── kustomization.yaml   # Base 통합 설정
│   └── overlays/               # 환경별 오버레이
│       ├── dev/                # 개발 환경
│       ├── staging/            # 스테이징 환경
│       └── prod/               # 프로덕션 환경
├── config/                     # CI/CD 설정 파일
└── scripts/                    # 배포 스크립트
```

## 주요 특징

### 1. 네임스페이스 분리
- **개발**: `phonebill-dev`
- **스테이징**: `phonebill-staging`
- **프로덕션**: `phonebill-prod`

### 2. 환경별 리소스 설정
- **개발**: 최소 리소스 (CPU: 250m, Memory: 512Mi)
- **스테이징**: 중간 리소스, 복제본 2개
- **프로덕션**: 최대 리소스, 복제본 3개

### 3. 서비스 구성
- **api-gateway**: API 게이트웨이
- **user-service**: 사용자 인증/관리
- **bill-service**: 요금 조회
- **product-service**: 상품 변경
- **kos-mock**: KOS 시스템 모킹

## 사용 방법

### 개발 환경 배포
```bash
kubectl apply -k deployment/cicd/kustomize/overlays/dev
```

### 스테이징 환경 배포
```bash
kubectl apply -k deployment/cicd/kustomize/overlays/staging
```

### 프로덕션 환경 배포
```bash
kubectl apply -k deployment/cicd/kustomize/overlays/prod
```

### 매니페스트 미리보기
```bash
kubectl kustomize deployment/cicd/kustomize/overlays/dev
```

## 주요 변경사항
1. 기존 `deployment/k8s/` 매니페스트를 `base/`로 복사
2. 하드코딩된 네임스페이스 제거 (`phonebill-dev`)
3. 환경별 오버레이 구조 적용
4. 리소스 제한 및 복제본 수 환경별 차별화

## Azure 연동 정보
- **ACR**: acrdigitalgarage01
- **리소스그룹**: rg-digitalgarage-01
- **AKS클러스터**: aks-digitalgarage-01