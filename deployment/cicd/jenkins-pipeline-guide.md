# Jenkins CI/CD 파이프라인 구축 가이드

## 📋 개요

이 가이드는 통신요금 관리 서비스(phonebill)를 위한 Jenkins 기반 CI/CD 파이프라인 구축 방법을 안내합니다.

**주요 특징:**
- Jenkins + Kustomize 기반 CI/CD 파이프라인
- 환경별(dev/staging/prod) 매니페스트 관리
- SonarQube 코드 품질 분석과 Quality Gate
- Azure Container Registry(ACR) 연동
- AKS(Azure Kubernetes Service) 자동 배포

## 🏗 아키텍처 구성

```
Jenkins Pipeline
    ↓
┌─── Build & Test (Gradle) ─────┐
│   - 소스코드 빌드               │
│   - 단위 테스트 실행            │
│   - SonarQube 품질 분석        │
│   - Quality Gate 검증          │
└─────────────────────────────┘
    ↓
┌─── Container Build ───────────┐
│   - 서비스별 이미지 빌드        │
│   - ACR에 이미지 푸시          │
│   - 환경별 태그 관리           │
└─────────────────────────────┘
    ↓
┌─── Deploy to AKS ─────────────┐
│   - Kustomize 매니페스트 적용  │
│   - 환경별 설정 적용           │
│   - 배포 상태 확인             │
└─────────────────────────────┘
```

## 🛠 사전 준비사항

### 실행 환경 정보
- **ACR명**: acrdigitalgarage01
- **리소스 그룹**: rg-digitalgarage-01  
- **AKS 클러스터**: aks-digitalgarage-01

### 서비스 구성
- **시스템명**: phonebill
- **서비스목록**:
  - api-gateway
  - user-service
  - bill-service
  - product-service
  - kos-mock

## 🔧 Jenkins 환경 구성

### 1. Jenkins 필수 플러그인 설치

```bash
# Jenkins 관리 > 플러그인 관리에서 다음 플러그인 설치
- Kubernetes
- Pipeline Utility Steps
- Docker Pipeline
- GitHub
- SonarQube Scanner
- Azure Credentials
```

### 2. Jenkins Credentials 등록

**Azure Service Principal 등록:**
```
Manage Jenkins > Credentials > Add Credentials
- Kind: Microsoft Azure Service Principal
- ID: azure-credentials
- Subscription ID: {구독ID}
- Client ID: {클라이언트ID}
- Client Secret: {클라이언트시크릿}
- Tenant ID: {테넌트ID}
- Azure Environment: Azure
```

**ACR Credentials 등록:**
```
- Kind: Username with password
- ID: acr-credentials
- Username: acrdigitalgarage01
- Password: {ACR패스워드}
```

**SonarQube Token 등록:**
```
- Kind: Secret text
- ID: sonarqube-token
- Secret: {SonarQube토큰}
```

## 📂 Kustomize 구조

프로젝트에 다음과 같은 Kustomize 구조가 생성되었습니다:

```
deployment/cicd/
├── kustomize/
│   ├── base/
│   │   ├── kustomization.yaml
│   │   ├── namespace.yaml
│   │   ├── common/
│   │   │   ├── cm-common.yaml
│   │   │   ├── secret-common.yaml
│   │   │   ├── secret-imagepull.yaml
│   │   │   └── ingress.yaml
│   │   └── [각 서비스별 매니페스트]
│   └── overlays/
│       ├── dev/
│       ├── staging/
│       └── prod/
├── config/
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/
│   └── deploy.sh
└── Jenkinsfile
```

## 🚀 Jenkins Pipeline 설정

### 1. Pipeline Job 생성

1. Jenkins 웹 UI에서 **New Item > Pipeline** 선택
2. **Pipeline script from SCM** 설정:
   ```
   SCM: Git
   Repository URL: {Git저장소URL}
   Branch: main (또는 develop)
   Script Path: deployment/cicd/Jenkinsfile
   ```

3. **Pipeline Parameters** 설정:
   ```
   ENVIRONMENT: Choice Parameter (dev, staging, prod)
   IMAGE_TAG: String Parameter (default: latest)
   ```

### 2. Pipeline 단계별 설명

**Stage 1: Get Source**
- Git 저장소에서 소스코드 체크아웃
- 환경별 설정 파일 로드

**Stage 2: Setup AKS**
- Azure 서비스 프린시팔로 로그인
- AKS 클러스터 연결 설정
- 네임스페이스 생성

**Stage 3: Build & SonarQube Analysis**
- Gradle 빌드 및 테스트 실행
- 각 서비스별 SonarQube 분석
- 코드 커버리지 리포트 생성

**Stage 4: Quality Gate**
- SonarQube Quality Gate 결과 대기
- 품질 기준 미달 시 파이프라인 중단

**Stage 5: Build & Push Images**
- 서비스별 컨테이너 이미지 빌드
- ACR에 이미지 푸시
- 환경별 태그 적용

**Stage 6: Update Kustomize & Deploy**
- 이미지 태그 업데이트
- Kustomize를 통한 매니페스트 적용
- 배포 완료 대기

## ⚙ SonarQube 설정

### Quality Gate 규칙
```yaml
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

### 프로젝트별 분석 제외 항목
```
**/config/**
**/entity/**
**/dto/**
**/*Application.class
**/exception/**
```

## 🎯 배포 실행 방법

### 1. Jenkins 파이프라인 실행

1. Jenkins > {프로젝트명} > **Build with Parameters**
2. **ENVIRONMENT** 선택 (dev/staging/prod)
3. **IMAGE_TAG** 입력 (선택사항)
4. **Build** 클릭

### 2. 수동 배포 스크립트 실행

```bash
# 개발 환경 배포
./deployment/cicd/scripts/deploy.sh dev latest

# 스테이징 환경 배포  
./deployment/cicd/scripts/deploy.sh staging v1.2.0

# 운영 환경 배포
./deployment/cicd/scripts/deploy.sh prod v1.2.0
```

### 3. 배포 상태 확인

```bash
# Pod 상태 확인
kubectl get pods -n phonebill-{환경}

# 서비스 상태 확인
kubectl get services -n phonebill-{환경}

# Ingress 상태 확인
kubectl get ingress -n phonebill-{환경}
```

## 🔄 롤백 방법

### 1. Kubernetes 롤백

```bash
# 특정 버전으로 롤백
kubectl rollout undo deployment/{환경}-{서비스명} -n phonebill-{환경} --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{환경}-{서비스명} -n phonebill-{환경}
```

### 2. 이미지 태그 기반 롤백

```bash
# 이전 안정 버전 이미지 태그로 업데이트
cd deployment/cicd/kustomize/overlays/{환경}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/{서비스명}:{환경}-{이전태그}
kubectl apply -k .
```

## 🏷 환경별 설정 차이점

### DEV 환경
- **Replicas**: 1개
- **Resources**: requests(256m/256Mi), limits(1024m/1024Mi)
- **Domain**: phonebill-api.20.214.196.128.nip.io
- **SSL**: 비활성화
- **DDL**: update

### STAGING 환경  
- **Replicas**: 2개
- **Resources**: requests(512m/512Mi), limits(2048m/2048Mi)
- **Domain**: phonebill-staging.example.com
- **SSL**: 활성화 (Let's Encrypt)
- **DDL**: validate

### PROD 환경
- **Replicas**: 3개
- **Resources**: requests(1024m/1024Mi), limits(4096m/4096Mi)
- **Domain**: phonebill-prod.example.com
- **SSL**: 활성화 (Let's Encrypt)
- **DDL**: validate
- **JWT**: 보안 강화 (짧은 유효시간)

## 📋 체크리스트

### 사전 준비
- [ ] settings.gradle에서 시스템명과 서비스명 확인
- [ ] Azure 환경 정보 확인 (ACR, 리소스 그룹, AKS 클러스터)
- [ ] Jenkins 플러그인 설치 완료
- [ ] Jenkins Credentials 등록 완료

### Kustomize 구성
- [ ] Base 매니페스트 복사 및 설정 완료
- [ ] 환경별 Overlay 구성 완료
- [ ] Patch 파일 작성 완료 (replicas, resources 포함)
- [ ] 환경별 설정 파일 생성 완료

### Jenkins Pipeline
- [ ] Jenkinsfile 작성 완료
- [ ] Pipeline Job 생성 및 설정 완료
- [ ] SonarQube 연동 설정 완료
- [ ] 배포 스크립트 생성 및 권한 설정 완료

### 배포 테스트
- [ ] DEV 환경 배포 테스트 완료
- [ ] STAGING 환경 배포 테스트 완료
- [ ] PROD 환경 배포 테스트 완료
- [ ] 롤백 테스트 완료

## 🚨 트러블슈팅

### 일반적인 문제들

**1. Quality Gate 실패**
```bash
# 해결방법: SonarQube 분석 결과 확인 및 코드 개선
./gradlew sonar
```

**2. 이미지 빌드 실패**
```bash
# 해결방법: Dockerfile 및 빌드 컨텍스트 확인
podman build --no-cache -f deployment/container/Dockerfile-backend .
```

**3. 배포 타임아웃**
```bash
# 해결방법: 리소스 사용량 및 노드 상태 확인
kubectl describe pods -n phonebill-{환경}
kubectl top nodes
```

**4. 네임스페이스 관련 오류**
```bash
# 해결방법: 네임스페이스 수동 생성
kubectl create namespace phonebill-{환경}
```

## 📞 지원 및 문의

Jenkins CI/CD 파이프라인 운영 중 문제가 발생하면 다음을 확인해 주세요:

1. Jenkins 빌드 로그 확인
2. SonarQube Quality Gate 결과 확인  
3. Kubernetes 클러스터 상태 확인
4. Azure Container Registry 연결 상태 확인

---

**데옵스**: Jenkins CI/CD 파이프라인이 성공적으로 구축되었습니다! 🎉

이제 각 환경별로 자동화된 빌드, 테스트, 배포가 가능합니다. SonarQube를 통한 코드 품질 관리와 Kustomize를 통한 환경별 설정 관리로 안정적인 DevOps 환경을 구축했습니다.