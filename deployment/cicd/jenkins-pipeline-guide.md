# Jenkins CI/CD 파이프라인 구축 가이드

## 📋 개요

이 가이드는 통신요금 관리 서비스(phonebill)를 위한 Jenkins + Kustomize 기반 CI/CD 파이프라인 구축 방법을 제공합니다.

### 🎯 주요 특징
- **환경별 배포**: dev, staging, prod 환경 분리 관리
- **Kustomize 기반**: 환경별 매니페스트 관리 및 배포
- **SonarQube 연동**: 코드 품질 분석 및 Quality Gate 적용
- **Azure 통합**: ACR, AKS와 완전 통합
- **Health Check**: 배포 후 서비스 상태 자동 확인

---

## 🏗️ 시스템 아키텍처

### 서비스 구성
- **시스템명**: phonebill
- **서비스 목록**:
  - api-gateway (API 게이트웨이)
  - user-service (사용자 서비스)
  - bill-service (요금 조회 서비스)
  - product-service (상품 변경 서비스)
  - kos-mock (KOS Mock 서비스)

### Azure 리소스 정보
- **ACR**: acrdigitalgarage01.azurecr.io
- **리소스 그룹**: rg-digitalgarage-01
- **AKS 클러스터**: aks-digitalgarage-01

---

## 🛠️ Jenkins 서버 환경 구성

### 1. 필수 플러그인 설치

Jenkins 관리 > 플러그인 관리에서 다음 플러그인들을 설치하세요:

```
📦 필수 플러그인 목록:
- Kubernetes
- Pipeline Utility Steps
- Docker Pipeline
- GitHub
- SonarQube Scanner
- Azure Credentials
```

### 2. Jenkins Credentials 등록

Manage Jenkins > Credentials > Add Credentials에서 다음 정보들을 등록하세요:

#### Azure Service Principal
```yaml
Kind: Microsoft Azure Service Principal
ID: azure-credentials
Subscription ID: {구독ID}
Client ID: {클라이언트ID}
Client Secret: {클라이언트시크릿}
Tenant ID: {테넌트ID}
Azure Environment: Azure
```

#### ACR Credentials
```yaml
Kind: Username with password
ID: acr-credentials
Username: acrdigitalgarage01
Password: {ACR패스워드}
```

#### SonarQube Token
```yaml
Kind: Secret text
ID: sonarqube-token
Secret: {SonarQube토큰}
```

---

## 📂 Kustomize 구조

### 디렉토리 구조
```
deployment/cicd/
├── kustomize/
│   ├── base/                    # 기본 매니페스트
│   │   ├── common/              # 공통 리소스
│   │   ├── api-gateway/         # API Gateway 리소스
│   │   ├── user-service/        # User Service 리소스
│   │   ├── bill-service/        # Bill Service 리소스
│   │   ├── product-service/     # Product Service 리소스
│   │   ├── kos-mock/           # KOS Mock 리소스
│   │   └── kustomization.yaml  # Base Kustomization
│   └── overlays/               # 환경별 오버레이
│       ├── dev/                # 개발 환경
│       ├── staging/            # 스테이징 환경
│       └── prod/               # 운영 환경
├── config/                     # 환경별 설정
├── scripts/                    # 배포 스크립트
└── Jenkinsfile                 # Jenkins 파이프라인
```

### 환경별 특성

#### 🔧 DEV 환경
- **네임스페이스**: phonebill-dev
- **도메인**: phonebill-api.20.214.196.128.nip.io (HTTP)
- **프로파일**: dev
- **DDL**: update (테이블 자동 생성/수정)
- **JWT 토큰**: 5시간 유효
- **Replica**: 모든 서비스 1개
- **리소스**: requests(256m CPU, 256Mi Memory), limits(1024m CPU, 1024Mi Memory)

#### 🔄 STAGING 환경
- **네임스페이스**: phonebill-staging
- **도메인**: phonebill-staging.20.214.196.128.nip.io (HTTPS)
- **프로파일**: staging
- **DDL**: validate (스키마 검증만)
- **JWT 토큰**: 5시간 유효
- **Replica**: 모든 서비스 2개
- **리소스**: requests(512m CPU, 512Mi Memory), limits(2048m CPU, 2048Mi Memory)

#### 🚀 PROD 환경
- **네임스페이스**: phonebill-prod
- **도메인**: phonebill.20.214.196.128.nip.io (HTTPS + SSL 강화)
- **프로파일**: prod
- **DDL**: validate (스키마 검증만)
- **JWT 토큰**: 1시간 유효 (보안 강화)
- **Replica**: 모든 서비스 3개
- **리소스**: requests(1024m CPU, 1024Mi Memory), limits(4096m CPU, 4096Mi Memory)

---

## 🔄 CI/CD 파이프라인 단계

### Pipeline 단계 설명

1. **Get Source** 📥
   - Git 소스 코드 체크아웃
   - 환경별 설정 파일 로딩

2. **Setup AKS** ⚙️
   - Azure Service Principal로 로그인
   - AKS 클러스터 연결 설정
   - 네임스페이스 생성

3. **Build & SonarQube Analysis** 🔍
   - Gradle 빌드 실행
   - 각 서비스별 단위 테스트
   - SonarQube 코드 품질 분석
   - 테스트 커버리지 리포트 생성

4. **Quality Gate** 🚪
   - SonarQube Quality Gate 검증
   - 품질 기준 미달 시 파이프라인 중단

5. **Build & Push Images** 🐳
   - 각 서비스별 컨테이너 이미지 빌드
   - ACR에 이미지 푸시
   - 환경별 이미지 태그 적용

6. **Update Kustomize & Deploy** 🚀
   - Kustomize를 통한 매니페스트 생성
   - 이미지 태그 업데이트
   - Kubernetes 클러스터에 배포
   - 배포 완료 대기

7. **Health Check** 🔍
   - API Gateway Health Check
   - 서비스 정상 동작 확인

### SonarQube Quality Gate 기준

```yaml
품질 기준:
- Coverage: >= 80%
- Duplicated Lines: <= 3%
- Maintainability Rating: <= A
- Reliability Rating: <= A
- Security Rating: <= A
```

---

## 🚀 Jenkins Pipeline Job 생성

### 1. 새 Pipeline Job 생성

1. Jenkins 웹 UI에서 **New Item** 클릭
2. **Pipeline** 선택 후 프로젝트명 입력
3. **OK** 클릭

### 2. Pipeline 설정

#### Source Code Management
```yaml
SCM: Git
Repository URL: {Git저장소URL}
Branch: main (또는 develop)
Script Path: deployment/cicd/Jenkinsfile
```

#### Pipeline Parameters
```yaml
ENVIRONMENT: 
  - Type: Choice Parameter
  - Choices: dev, staging, prod
  - Default: dev

IMAGE_TAG:
  - Type: String Parameter  
  - Default: latest
```

---

## 📦 배포 실행 방법

### 1. Jenkins UI를 통한 배포

1. Jenkins > {프로젝트명} > **Build with Parameters** 클릭
2. **ENVIRONMENT** 선택 (dev/staging/prod)
3. **IMAGE_TAG** 입력 (선택사항, 기본값: latest)
4. **Build** 클릭

### 2. 수동 배포 스크립트 사용

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

# 로그 확인
kubectl logs -n phonebill-{환경} deployment/{환경}-api-gateway
```

---

## 🔄 롤백 방법

### 1. Kubernetes 기본 롤백

```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/{환경}-{서비스명} -n phonebill-{환경}

# 특정 리비전으로 롤백
kubectl rollout undo deployment/{환경}-{서비스명} -n phonebill-{환경} --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{환경}-{서비스명} -n phonebill-{환경}
```

### 2. 이미지 태그 기반 롤백

```bash
# 안정 버전 태그로 수동 배포
./deployment/cicd/scripts/deploy.sh prod {이전안정버전태그}
```

---

## 🔧 트러블슈팅

### 일반적인 문제 해결

#### 1. 파이프라인 실패 시
```bash
# Jenkins 콘솔 로그 확인
# SonarQube Quality Gate 상태 확인
# Kubernetes 이벤트 확인
kubectl get events -n phonebill-{환경} --sort-by='.lastTimestamp'
```

#### 2. 배포 실패 시
```bash
# Pod 상태 및 로그 확인
kubectl describe pod {pod-name} -n phonebill-{환경}
kubectl logs {pod-name} -n phonebill-{환경}

# ConfigMap/Secret 확인
kubectl get configmap -n phonebill-{환경}
kubectl get secret -n phonebill-{환경}
```

#### 3. 네트워크 연결 문제
```bash
# Service와 Endpoint 확인
kubectl get svc,endpoints -n phonebill-{환경}

# Ingress 설정 확인  
kubectl describe ingress -n phonebill-{환경}
```

---

## 📝 체크리스트

### 📋 Kustomize 구성 체크리스트

#### 📂 기본 구조 검증
- [ ] 디렉토리 구조: `deployment/cicd/kustomize/{base,overlays/{dev,staging,prod}}`
- [ ] 서비스별 base 디렉토리: `base/{common,api-gateway,user-service,bill-service,product-service,kos-mock}`
- [ ] Base kustomization.yaml 파일 생성 완료

#### 🔧 환경별 Overlay 검증
**각 환경(dev/staging/prod)별로 다음 파일들이 모두 생성되어야 함:**

**필수 파일 목록:**
- [ ] `kustomization.yaml` - 환경 설정 및 patch 파일 참조
- [ ] `configmap-common-patch.yaml` - 환경별 공통 설정 (프로파일, DDL, JWT 설정)
- [ ] `secret-common-patch.yaml` - 환경별 공통 시크릿 (JWT Secret, Redis 정보)
- [ ] `ingress-patch.yaml` - 환경별 도메인 및 보안 설정
- [ ] **`deployment-patch.yaml`** - **환경별 replicas AND resources 설정** ⚠️
- [ ] `secret-user-service-patch.yaml` - User Service DB 정보
- [ ] `secret-bill-service-patch.yaml` - Bill Service DB 정보  
- [ ] `secret-product-service-patch.yaml` - Product Service DB 정보

**⚠️ deployment-patch.yaml 필수 검증 사항:**
- [ ] **파일명이 정확한지**: `deployment-patch.yaml` (❌ `replica-patch.yaml` 아님)
- [ ] **Strategic Merge Patch 형식 사용**: YAML 형식, JSON Patch 아님
- [ ] **replicas 설정**: dev(1), staging(2), prod(3)
- [ ] **resources 설정**: 환경별 차등 적용
  - dev: requests(256m CPU, 256Mi Memory), limits(1024m CPU, 1024Mi Memory)
  - staging: requests(512m CPU, 512Mi Memory), limits(2048m CPU, 2048Mi Memory)
  - prod: requests(1024m CPU, 1024Mi Memory), limits(4096m CPU, 4096Mi Memory)
- [ ] **모든 서비스 포함**: api-gateway, user-service, bill-service, product-service, kos-mock

#### 🔍 호환성 검증
- [ ] base 매니페스트에 없는 항목을 patch에 추가하지 않음
- [ ] base 매니페스트와 patch 필드 구조 일치
- [ ] Secret 매니페스트에 'data' 대신 'stringData' 사용

### 📋 배포 전 체크리스트

- [ ] Jenkins 필수 플러그인 설치 완료
- [ ] Credentials 등록 완료 (Azure, ACR, SonarQube)
- [ ] SonarQube 프로젝트 설정 완료
- [ ] 환경별 Database/Redis 준비 완료
- [ ] 네트워크 및 도메인 설정 완료

### 🚀 배포 후 체크리스트

- [ ] 모든 Pod가 Running 상태
- [ ] Health Check 통과
- [ ] Ingress로 외부 접근 가능
- [ ] 로그에 오류 없음
- [ ] 기능 테스트 완료

### 💡 일반적인 실수 방지 가이드

**❌ 자주 발생하는 실수들:**
1. **파일명 실수**: `replica-patch.yaml` 생성 → 정답: `deployment-patch.yaml`
2. **내용 누락**: replicas만 설정하고 resources 누락 → 정답: 둘 다 설정
3. **형식 실수**: JSON Patch 사용 → 정답: Strategic Merge Patch 사용
4. **환경별 차이 없음**: 모든 환경 동일 설정 → 정답: 환경별 차등 설정

**✅ 올바른 deployment-patch.yaml 예시:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 1  # 환경별 차등 적용
  template:
    spec:
      containers:
      - name: api-gateway
        resources:  # 반드시 포함
          requests:
            cpu: 256m      # 환경별 차등 적용
            memory: 256Mi  # 환경별 차등 적용
          limits:
            cpu: 1024m     # 환경별 차등 적용
            memory: 1024Mi # 환경별 차등 적용
```

---

## 📞 지원 정보

### 환경 정보
- **시스템**: phonebill (통신요금 관리 서비스)
- **Git 저장소**: [Repository URL]
- **Jenkins**: [Jenkins URL]
- **SonarQube**: [SonarQube URL]

### 연락처
- **DevOps 팀**: 최운영 (데옵스)
- **백엔드 팀**: 이개발 (백엔더)
- **QA 팀**: 정테스트 (QA매니저)

---

## 📚 추가 리소스

- [Kustomize 공식 문서](https://kustomize.io/)
- [Jenkins Pipeline 문법](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Azure DevOps 가이드](https://docs.microsoft.com/en-us/azure/devops/)

---

*이 가이드는 phonebill 프로젝트의 CI/CD 파이프라인 구축을 위한 완전한 가이드입니다. 추가 질문이나 지원이 필요하시면 DevOps 팀에 문의하세요.*