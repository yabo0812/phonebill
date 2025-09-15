# 백엔드 GitHub Actions CI/CD 파이프라인 가이드

## 📋 개요

GitHub Actions를 이용한 백엔드 서비스 CI/CD 파이프라인 구축 가이드입니다.
Kustomize를 활용한 환경별 배포 자동화와 SonarQube 품질 분석을 포함합니다.

### 시스템 정보
- **시스템명**: phonebill
- **서비스 목록**: api-gateway, user-service, bill-service, product-service, kos-mock
- **JDK 버전**: 21
- **ACR 이름**: acrdigitalgarage01
- **리소스 그룹**: rg-digitalgarage-01
- **AKS 클러스터**: aks-digitalgarage-01

## 🏗️ 구축된 파일 구조

```
.github/
├── kustomize/
│   ├── base/
│   │   ├── kustomization.yaml
│   │   ├── namespace.yaml
│   │   ├── common/
│   │   │   ├── cm-common.yaml
│   │   │   ├── secret-common.yaml
│   │   │   ├── secret-imagepull.yaml
│   │   │   └── ingress.yaml
│   │   └── {service-name}/
│   │       ├── deployment.yaml
│   │       ├── service.yaml
│   │       ├── cm-{service-name}.yaml (존재 시)
│   │       └── secret-{service-name}.yaml (존재 시)
│   └── overlays/
│       ├── dev/
│       │   ├── kustomization.yaml
│       │   ├── cm-common-patch.yaml
│       │   ├── secret-common-patch.yaml
│       │   ├── ingress-patch.yaml
│       │   ├── deployment-{service-name}-patch.yaml
│       │   └── secret-{service-name}-patch.yaml
│       ├── staging/
│       └── prod/
├── config/
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/
│   └── deploy-actions.sh
└── workflows/
    └── backend-cicd.yaml
```

## ⚙️ GitHub Repository 설정

### 1. Repository Secrets 설정

Repository Settings > Secrets and variables > Actions > Repository secrets에 다음을 등록:

```yaml
# Azure Service Principal
AZURE_CREDENTIALS:
{
  "clientId": "{클라이언트ID}",
  "clientSecret": "{클라이언트시크릿}",
  "subscriptionId": "{구독ID}",
  "tenantId": "{테넌트ID}"
}

# ACR Credentials
ACR_USERNAME: acrdigitalgarage01
ACR_PASSWORD: {ACR패스워드}

# Docker Hub (Rate Limit 해결용)
DOCKERHUB_USERNAME: {Docker Hub 사용자명}
DOCKERHUB_PASSWORD: {Docker Hub 패스워드}

# SonarQube (선택사항)
SONAR_TOKEN: {SonarQube토큰}
SONAR_HOST_URL: {SonarQube서버URL}
```

### 2. Repository Variables 설정

Repository Settings > Secrets and variables > Actions > Variables > Repository variables에 등록:

```yaml
ENVIRONMENT: dev  # 기본값
SKIP_SONARQUBE: true  # 기본값
```

### 3. ACR 패스워드 확인 방법

```bash
az acr credential show --name acrdigitalgarage01
```

## 🚀 CI/CD 파이프라인 구성

### 워크플로우 트리거

1. **자동 실행**:
   - `main`, `develop` 브랜치에 Push
   - `main` 브랜치로 Pull Request

2. **수동 실행**:
   - Actions 탭 > "Backend Services CI/CD" > "Run workflow"
   - 환경 선택: dev/staging/prod
   - SonarQube 분석 여부 선택

### 파이프라인 단계

#### 1. Build and Test
- Gradle 빌드 (테스트 제외)
- SonarQube 분석 (선택적)
- 빌드 아티팩트 업로드

#### 2. Build and Push Docker Images
- Docker 이미지 빌드
- ACR에 푸시 (태그: {environment}-{timestamp})

#### 3. Deploy to Kubernetes
- Kustomize를 이용한 환경별 배포
- 배포 상태 확인
- Health Check

## 🔧 환경별 설정

### 개발 환경 (dev)
- **네임스페이스**: phonebill-dev
- **Replicas**: 1
- **Resources**: 256Mi/256m → 1024Mi/1024m
- **DDL**: update
- **Host**: phonebill-api.20.214.196.128.nip.io
- **SSL**: false

### 스테이징 환경 (staging)
- **네임스페이스**: phonebill-staging
- **Replicas**: 2
- **Resources**: 512Mi/512m → 2048Mi/2048m
- **DDL**: validate
- **Host**: staging.phonebill.com
- **SSL**: true (Let's Encrypt)

### 운영 환경 (prod)
- **네임스페이스**: phonebill-prod
- **Replicas**: 3
- **Resources**: 1024Mi/1024m → 4096Mi/4096m
- **DDL**: validate
- **JWT Token**: 1시간 (보안 강화)
- **Host**: phonebill.com
- **SSL**: true (Let's Encrypt)

## 📝 수동 배포 방법

### 스크립트 사용
```bash
# 개발 환경 배포
./.github/scripts/deploy-actions.sh dev latest

# 스테이징 환경 배포
./.github/scripts/deploy-actions.sh staging 20241215123456

# 운영 환경 배포
./.github/scripts/deploy-actions.sh prod 20241215123456
```

### kubectl 직접 사용
```bash
# 환경별 디렉토리로 이동
cd .github/kustomize/overlays/dev

# 이미지 태그 업데이트
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/api-gateway:dev-20241215123456

# 배포 실행
kubectl apply -k .
```

## 🔄 롤백 방법

### 1. GitHub Actions를 통한 롤백
1. GitHub > Actions > 성공한 이전 워크플로우 선택
2. "Re-run all jobs" 클릭

### 2. kubectl을 이용한 롤백
```bash
# 특정 버전으로 롤백
kubectl rollout undo deployment/api-gateway -n phonebill-dev --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/api-gateway -n phonebill-dev
```

### 3. 수동 스크립트를 이용한 롤백
```bash
# 이전 안정 버전 이미지 태그로 배포
./.github/scripts/deploy-actions.sh dev 20241214123456
```

## 🔍 SonarQube 설정

### Quality Gate 기준
- Coverage: >= 80%
- Duplicated Lines: <= 3%
- Maintainability Rating: <= A
- Reliability Rating: <= A
- Security Rating: <= A

### 프로젝트 생성
각 서비스별로 `phonebill-{service}-{environment}` 형식으로 프로젝트 생성

## 📊 모니터링 및 확인

### 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n phonebill-dev

# 서비스 상태 확인
kubectl get services -n phonebill-dev

# Ingress 확인
kubectl get ingress -n phonebill-dev

# 로그 확인
kubectl logs -f deployment/api-gateway -n phonebill-dev
```

### Health Check
```bash
# API Gateway Health Check
kubectl -n phonebill-dev exec deployment/api-gateway -- curl -f http://localhost:8080/actuator/health
```

## ⚠️ 주의사항

1. **환경별 Secret 관리**:
   - 현재는 동일한 값으로 설정되어 있음
   - 실제 운영 시 환경별로 다른 값 설정 필요

2. **도메인 설정**:
   - staging/prod 환경의 도메인은 실제 구매한 도메인으로 변경 필요
   - SSL 인증서는 cert-manager 설정 필요

3. **리소스 한계**:
   - 환경별 리소스 설정은 실제 부하에 맞게 조정 필요

4. **데이터베이스 연결**:
   - 환경별로 다른 데이터베이스 인스턴스 사용 권장

## 🔧 문제 해결

### 일반적인 문제들

1. **이미지 Pull 실패**:
   ```bash
   # Secret 확인
   kubectl get secret secret-imagepull -n phonebill-dev -o yaml
   ```

2. **ConfigMap/Secret 업데이트 반영 안됨**:
   ```bash
   # Pod 재시작
   kubectl rollout restart deployment/api-gateway -n phonebill-dev
   ```

3. **Ingress IP 할당 안됨**:
   ```bash
   # Ingress Controller 상태 확인
   kubectl get pods -n ingress-nginx
   ```

## 📚 참고 자료

- [Kustomize 공식 문서](https://kustomize.io/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Azure Container Registry 문서](https://docs.microsoft.com/en-us/azure/container-registry/)
- [Azure Kubernetes Service 문서](https://docs.microsoft.com/en-us/azure/aks/)

---

## 체크리스트

### 초기 설정
- [ ] GitHub Repository Secrets 설정 완료
- [ ] GitHub Repository Variables 설정 완료
- [ ] Azure Service Principal 생성 및 권한 설정
- [ ] ACR 접근 권한 확인

### 배포 테스트
- [ ] 개발 환경 배포 성공
- [ ] 스테이징 환경 배포 성공
- [ ] 운영 환경 배포 성공
- [ ] Health Check 통과
- [ ] 롤백 테스트 성공

### 모니터링 설정
- [ ] SonarQube 프로젝트 생성
- [ ] Quality Gate 설정
- [ ] 알림 설정 (선택사항)

이 가이드를 통해 GitHub Actions 기반의 완전 자동화된 CI/CD 파이프라인을 구축할 수 있습니다.