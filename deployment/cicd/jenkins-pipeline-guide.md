# phonebill Jenkins CI/CD 파이프라인 구축 가이드

## 📋 개요

**이개발/백엔더**: phonebill 프로젝트의 Jenkins 기반 CI/CD 파이프라인 구축이 완료되었습니다.

### 프로젝트 정보
- **시스템명**: phonebill
- **서비스**: api-gateway, user-service, bill-service, product-service, kos-mock
- **JDK 버전**: 21
- **환경**: dev, staging, prod
- **컨테이너 레지스트리**: acrdigitalgarage01.azurecr.io
- **Kubernetes 클러스터**: aks-digitalgarage-01 (rg-digitalgarage-01)

## 🏗️ 구축된 CI/CD 아키텍처

### 파이프라인 구성
1. **소스 체크아웃** → Git 소스 코드 가져오기
2. **AKS 설정** → Azure 인증 및 Kubernetes 클러스터 연결
3. **빌드 & SonarQube 분석** → Gradle 빌드, 테스트, 코드 품질 분석
4. **Quality Gate** → SonarQube 품질 게이트 검증
5. **컨테이너 빌드 & 푸시** → Docker 이미지 빌드 및 ACR 푸시
6. **Kustomize 배포** → 환경별 Kubernetes 매니페스트 적용

### Kustomize 구조
```
deployment/cicd/kustomize/
├── base/                           # 기본 매니페스트
│   ├── kustomization.yaml         # Base 리소스 정의
│   ├── namespace.yaml             # Namespace 정의
│   ├── common/                    # 공통 리소스
│   │   ├── cm-common.yaml
│   │   ├── secret-common.yaml
│   │   ├── secret-imagepull.yaml
│   │   └── ingress.yaml
│   └── [서비스별 디렉토리]/        # 각 서비스 매니페스트
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── cm-{서비스명}.yaml     # ConfigMap (있는 경우)
│       └── secret-{서비스명}.yaml  # Secret (있는 경우)
└── overlays/                      # 환경별 오버레이
    ├── dev/                       # 개발 환경
    ├── staging/                   # 스테이징 환경
    └── prod/                      # 운영 환경
```

## ⚙️ 구성 요소

### 1. Jenkins 파이프라인 (Jenkinsfile)
- **Pod Template**: Gradle, Podman, Azure-CLI 컨테이너 사용
- **자동 정리**: podRetention: never(), 파드 자동 정리 구성
- **병렬 처리**: 각 서비스별 SonarQube 분석 병렬 실행
- **타임아웃**: 빌드&푸시 30분, Quality Gate 10분 제한

### 2. 환경별 Configuration

#### DEV 환경
- **네임스페이스**: phonebill-dev
- **레플리카**: 1개
- **리소스**: requests(256m CPU, 256Mi Memory), limits(1024m CPU, 1024Mi Memory)
- **프로파일**: dev, DDL_AUTO: update
- **도메인**: phonebill-api.20.214.196.128.nip.io (HTTP)

#### STAGING 환경
- **네임스페이스**: phonebill-staging
- **레플리카**: 2개
- **리소스**: requests(512m CPU, 512Mi Memory), limits(2048m CPU, 2048Mi Memory)
- **프로파일**: staging, DDL_AUTO: validate
- **도메인**: phonebill-staging.yourdomain.com (HTTPS)

#### PROD 환경
- **네임스페이스**: phonebill-prod
- **레플리카**: 3개
- **리소스**: requests(1024m CPU, 1024Mi Memory), limits(4096m CPU, 4096Mi Memory)
- **프로파일**: prod, DDL_AUTO: validate
- **도메인**: phonebill.yourdomain.com (HTTPS)
- **보안**: 짧은 JWT 토큰 유효시간

### 3. 스크립트
- **deploy.sh**: 수동 배포 스크립트
- **validate-cicd-setup.sh**: CI/CD 설정 검증 스크립트

## 📦 구축된 파일 목록

### Kustomize 구성 파일
```
deployment/cicd/
├── kustomize/
│   ├── base/
│   │   ├── kustomization.yaml
│   │   ├── namespace.yaml
│   │   ├── common/ (4개 파일)
│   │   ├── api-gateway/ (3개 파일)
│   │   ├── user-service/ (4개 파일)
│   │   ├── bill-service/ (4개 파일)
│   │   ├── product-service/ (4개 파일)
│   │   └── kos-mock/ (3개 파일)
│   └── overlays/
│       ├── dev/ (12개 파일)
│       ├── staging/ (13개 파일)
│       └── prod/ (14개 파일)
├── config/
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/
│   ├── deploy.sh (실행 가능)
│   └── validate-cicd-setup.sh (실행 가능)
├── Jenkinsfile
└── jenkins-pipeline-guide.md
```

## 🚀 Jenkins 설정 방법

### 1. Jenkins 서버 환경 구성

#### 필수 플러그인 설치
```
- Kubernetes
- Pipeline Utility Steps  
- Docker Pipeline
- GitHub
- SonarQube Scanner
- Azure Credentials
```

#### Jenkins Credentials 등록
**Manage Jenkins > Credentials > Add Credentials**

1. **Azure Service Principal**
   - Kind: Microsoft Azure Service Principal
   - ID: `azure-credentials`
   - Subscription ID, Client ID, Client Secret, Tenant ID 입력
   - Azure Environment: Azure

2. **ACR Credentials**
   - Kind: Username with password
   - ID: `acr-credentials`
   - Username: `acrdigitalgarage01`
   - Password: {ACR_PASSWORD}

3. **Docker Hub Credentials** (Rate Limit 해결용)
   - Kind: Username with password
   - ID: `dockerhub-credentials`
   - Username: {DOCKERHUB_USERNAME}
   - Password: {DOCKERHUB_PASSWORD}

4. **SonarQube Token**
   - Kind: Secret text
   - ID: `sonarqube-token`
   - Secret: {SonarQube토큰}

### 2. Jenkins Pipeline Job 생성

1. **New Item > Pipeline** 선택
2. **Pipeline script from SCM** 설정:
   - SCM: Git
   - Repository URL: {Git저장소URL}
   - Branch: main
   - Script Path: `deployment/cicd/Jenkinsfile`

3. **Pipeline Parameters** 설정:
   - ENVIRONMENT: Choice Parameter (dev, staging, prod)
   - IMAGE_TAG: String Parameter (default: latest)

## 📊 SonarQube 설정

### 각 서비스별 프로젝트 생성
- 프로젝트 키: `phonebill-{서비스명}-{환경}`
- Quality Gate 설정:
  - Coverage: ≥ 80%
  - Duplicated Lines: ≤ 3%
  - Maintainability Rating: ≤ A
  - Reliability Rating: ≤ A
  - Security Rating: ≤ A

## 🔄 배포 실행 방법

### 1. Jenkins 파이프라인 실행
1. Jenkins > phonebill 프로젝트 > **Build with Parameters**
2. ENVIRONMENT 선택 (dev/staging/prod)
3. IMAGE_TAG 입력 (선택사항)
4. **Build** 클릭

### 2. 수동 배포 (선택사항)
```bash
# 개발 환경 배포
./deployment/cicd/scripts/deploy.sh dev 20240912101530

# 스테이징 환경 배포
./deployment/cicd/scripts/deploy.sh staging 20240912101530

# 운영 환경 배포
./deployment/cicd/scripts/deploy.sh prod 20240912101530
```

### 3. 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n phonebill-{환경}

# 서비스 상태 확인
kubectl get services -n phonebill-{환경}

# Ingress 상태 확인
kubectl get ingress -n phonebill-{환경}

# 배포 이력 확인
kubectl rollout history deployment/{서비스명} -n phonebill-{환경}
```

## 🔍 설정 검증

### CI/CD 설정 검증 실행
```bash
./deployment/cicd/scripts/validate-cicd-setup.sh
```

**검증 항목:**
- ✅ 서비스별 매니페스트 파일 존재 확인
- ✅ Base kustomization.yaml 유효성 검사
- ✅ 환경별 Overlay 빌드 테스트
- ✅ Jenkinsfile 구성 확인
- ✅ 환경별 설정 파일 검증
- ✅ 스크립트 실행 권한 확인

## 🔧 롤백 방법

### 1. kubectl을 이용한 롤백
```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/{서비스명} -n phonebill-{환경} --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{서비스명} -n phonebill-{환경}
```

### 2. 이미지 태그 기반 롤백
```bash
# 이전 안정 버전으로 수동 배포
cd deployment/cicd/kustomize/overlays/{환경}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/{서비스명}:{환경}-{이전태그}
kubectl apply -k .
```

## 🛡️ 보안 및 모니터링

### 파드 자동 정리
- **podRetention: never()**: 파이프라인 완료 시 파드 즉시 삭제
- **terminationGracePeriodSeconds: 3**: 3초 내 강제 종료
- **idleMinutes: 1**: 유휴 시간 1분 설정

### 리소스 제한
- **Timeout 설정**: Build&Push 30분, Quality Gate 10분
- **컨테이너 리소스**: 환경별 차등 할당
- **네트워크 격리**: 네임스페이스별 분리

## ✅ 구축 완료 체크리스트

### 📋 사전 준비
- [x] settings.gradle에서 시스템명과 서비스명 확인
- [x] 루트 build.gradle에서 JDK버전 확인 (21)
- [x] 실행정보에서 ACR명, 리소스 그룹, AKS 클러스터명 확인

### 📂 Kustomize 구성
- [x] 디렉토리 구조 생성
- [x] 기존 k8s 매니페스트를 base로 복사
- [x] Base kustomization.yaml 작성 (모든 리소스 포함)
- [x] kubectl kustomize 검증 완료

### 🔧 환경별 Overlay
- [x] DEV 환경: 12개 파일 생성 (1 replica, HTTP)
- [x] STAGING 환경: 13개 파일 생성 (2 replicas, HTTPS)
- [x] PROD 환경: 14개 파일 생성 (3 replicas, HTTPS, 보안 강화)

### ⚙️ 스크립트 및 설정
- [x] 환경별 설정 파일 작성 (dev/staging/prod)
- [x] Jenkinsfile 작성 (JDK21, 파드 자동 정리 포함)
- [x] 수동 배포 스크립트 작성 및 실행 권한 설정
- [x] 검증 스크립트 작성 및 실행 권한 설정

## 🎯 다음 단계

1. **Jenkins 서버 설정**
   - 필수 플러그인 설치
   - Credentials 등록 (azure, acr, dockerhub, sonarqube)

2. **SonarQube 연동**
   - 서비스별 프로젝트 생성
   - Quality Gate 규칙 설정

3. **파이프라인 테스트**
   - 개발 환경 배포 테스트
   - 스테이징/운영 환경 배포 준비

4. **모니터링 설정**
   - 배포 상태 모니터링
   - 알림 시스템 구성

---

**구축자**: 이개발/백엔더  
**구축일**: 2024년 12월 12일  
**버전**: v1.0.0