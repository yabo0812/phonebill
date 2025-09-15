# 백엔드 GitHub Actions CI/CD 파이프라인 가이드

## 📋 개요

통신요금 관리 서비스(phonebill)의 GitHub Actions 기반 CI/CD 파이프라인 구축 가이드입니다.

**실행정보**:
- ACR_NAME: acrdigitalgarage01
- RESOURCE_GROUP: rg-digitalgarage-01
- AKS_CLUSTER: aks-digitalgarage-01

**시스템 정보**:
- 시스템명: phonebill
- JDK 버전: 21
- 서비스: api-gateway, user-service, bill-service, product-service, kos-mock

## 🏗️ 구축된 파이프라인 구조

### 디렉토리 구조
```
.github/
├── kustomize/
│   ├── base/                    # 기본 매니페스트
│   │   ├── kustomization.yaml
│   │   ├── namespace.yaml
│   │   ├── common/              # 공통 리소스
│   │   └── {서비스명}/          # 각 서비스별 매니페스트
│   └── overlays/                # 환경별 오버레이
│       ├── dev/
│       ├── staging/
│       └── prod/
├── config/                      # 환경별 설정
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/                     # 배포 스크립트
│   └── deploy-actions.sh
└── workflows/                   # GitHub Actions 워크플로우
    └── backend-cicd.yaml
```

### 파이프라인 워크플로우
1. **Build & Test**: Gradle 빌드 및 단위 테스트
2. **SonarQube Analysis**: 코드 품질 분석 (선택적)
3. **Container Build & Push**: 환경별 이미지 빌드 및 ACR 푸시
4. **Kustomize Deploy**: 환경별 매니페스트 적용

## ⚙️ GitHub Repository 설정

### 1. Repository Secrets 설정

GitHub Repository > Settings > Secrets and variables > Actions > Repository secrets에 다음 항목들을 등록하세요:

#### Azure 인증 정보
```json
AZURE_CREDENTIALS:
{
  "clientId": "5e4b5b41-7208-48b7-b821-d6d5acf50ecf",
  "clientSecret": "ldu8Q~GQEzFYU.dJX7_QsahR7n7C2xqkIM6hqbV8",
  "subscriptionId": "2513dd36-7978-48e3-9a7c-b221d4874f66",
  "tenantId": "4f0a3bfd-1156-4cce-8dc2-a049a13dba23"
}
```

#### ACR Credentials
```bash
# ACR 자격 증명 확인 명령어
az acr credential show --name acrdigitalgarage01
```
```
ACR_USERNAME: acrdigitalgarage01
ACR_PASSWORD: {ACR패스워드}
```

#### SonarQube 설정
```bash
# SonarQube URL 확인
kubectl get svc -n sonarqube
```
```
SONAR_HOST_URL: http://{External IP}
SONAR_TOKEN: {SonarQube토큰}
```

**SonarQube 토큰 생성 방법**:
1. SonarQube 로그인 후 우측 상단 'Administrator' > My Account 클릭
2. Security 탭 선택 후 토큰 생성

#### Docker Hub 설정 (Rate Limit 해결)
```
DOCKERHUB_USERNAME: {Docker Hub 사용자명}
DOCKERHUB_PASSWORD: {Docker Hub 패스워드}
```

### 2. Repository Variables 설정

GitHub Repository > Settings > Secrets and variables > Actions > Variables > Repository variables에 등록:

```
ENVIRONMENT: dev
SKIP_SONARQUBE: true
```

## 🚀 파이프라인 실행 방법

### 자동 실행
- **Push/PR 트리거**: main, develop 브랜치로 push시 자동 실행
- **기본 설정**: ENVIRONMENT=dev, SKIP_SONARQUBE=true

### 수동 실행
1. GitHub Repository > Actions 탭
2. "Backend Services CI/CD" 워크플로우 선택
3. "Run workflow" 버튼 클릭
4. 환경 선택 (dev/staging/prod)
5. SonarQube 분석 여부 선택 (true/false)

## 📦 배포 환경별 설정

### DEV 환경
- **Namespace**: phonebill-dev
- **Replicas**: 모든 서비스 1개
- **Resources**: CPU 256m/1024m, Memory 256Mi/1024Mi
- **Profile**: dev (DDL_AUTO: update)
- **SSL**: 비활성화

### STAGING 환경
- **Namespace**: phonebill-staging
- **Replicas**: 모든 서비스 2개
- **Resources**: CPU 512m/2048m, Memory 512Mi/2048Mi
- **Profile**: staging (DDL_AUTO: validate)
- **SSL**: 활성화

### PROD 환경
- **Namespace**: phonebill-prod
- **Replicas**: 모든 서비스 3개
- **Resources**: CPU 1024m/4096m, Memory 1024Mi/4096Mi
- **Profile**: prod (DDL_AUTO: validate, 짧은 JWT)
- **SSL**: 활성화

## 🔧 수동 배포 방법

### 스크립트를 이용한 배포
```bash
# 기본 (dev 환경, latest 태그)
./.github/scripts/deploy-actions.sh

# 특정 환경과 태그 지정
./.github/scripts/deploy-actions.sh staging 20241215143022
```

### kubectl을 이용한 직접 배포
```bash
# Kustomize 설치
curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
sudo mv kustomize /usr/local/bin/

# 환경별 배포
cd .github/kustomize/overlays/dev
kubectl apply -k .
```

## 🔄 롤백 방법

### 1. GitHub Actions를 통한 롤백
1. GitHub > Actions > 성공한 이전 워크플로우 선택
2. "Re-run all jobs" 클릭

### 2. kubectl을 이용한 롤백
```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/user-service -n phonebill-dev --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/user-service -n phonebill-dev
```

### 3. 수동 스크립트를 이용한 롤백
```bash
# 이전 안정 버전 태그로 배포
./.github/scripts/deploy-actions.sh dev 20241215140000
```

## 📊 SonarQube Quality Gate 설정

각 서비스별 SonarQube 프로젝트에서 다음 Quality Gate 설정:

```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

## 🐛 트러블슈팅

### 1. Kustomize 빌드 실패
```bash
# Base 매니페스트 검증
kubectl kustomize .github/kustomize/base/

# 환경별 오버레이 검증
kubectl kustomize .github/kustomize/overlays/dev/
```

### 2. 이미지 Pull 실패
- ACR 자격 증명 확인
- ImagePullSecret 설정 확인

### 3. 배포 타임아웃
```bash
# 배포 상태 확인
kubectl get pods -n phonebill-dev

# 로그 확인
kubectl logs -f deployment/user-service -n phonebill-dev
```

### 4. SonarQube 연결 실패
- SONAR_HOST_URL과 SONAR_TOKEN 확인
- SonarQube 서버 상태 확인

## 📋 체크리스트

### 배포 전 확인사항
- [ ] GitHub Secrets 모든 항목 설정 완료
- [ ] Repository Variables 설정 완료
- [ ] ACR 접근 권한 확인
- [ ] AKS 클러스터 접근 권한 확인
- [ ] SonarQube 서버 상태 확인 (분석 수행시)

### 배포 후 확인사항
- [ ] 모든 Pod가 Running 상태인지 확인
- [ ] Service와 Ingress가 정상적으로 생성되었는지 확인
- [ ] Health Check 엔드포인트 응답 확인
- [ ] 로그에 에러가 없는지 확인

## 📞 지원 및 연락처

문제 발생시 다음 명령어로 디버깅 정보를 수집하여 지원팀에 문의하세요:

```bash
# 시스템 상태 확인
kubectl get all -n phonebill-{환경}

# 로그 수집
kubectl logs -l app.kubernetes.io/name=user-service -n phonebill-{환경}

# 이벤트 확인
kubectl get events -n phonebill-{환경} --sort-by='.lastTimestamp'
```

---

✅ **GitHub Actions CI/CD 파이프라인 구축이 완료되었습니다!**

이제 코드를 푸시하거나 수동으로 워크플로우를 실행하여 자동 배포를 테스트할 수 있습니다.