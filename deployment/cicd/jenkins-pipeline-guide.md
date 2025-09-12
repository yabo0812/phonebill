# Jenkins CI/CD 파이프라인 구축 가이드

**최운영/데옵스**가 작성한 통신요금 관리 서비스 Jenkins CI/CD 파이프라인 구축 가이드입니다.

## 📋 프로젝트 정보

### 시스템 정보
- **시스템명**: phonebill
- **서비스**: api-gateway, user-service, bill-service, product-service, kos-mock
- **JDK 버전**: 21
- **Container Registry**: acrdigitalgarage01.azurecr.io
- **Resource Group**: rg-digitalgarage-01
- **AKS Cluster**: aks-digitalgarage-01

## 🏗️ 아키텍처 개요

본 CI/CD 파이프라인은 다음 구성 요소들로 이루어져 있습니다:

- **Jenkins**: 파이프라인 오케스트레이션
- **Kustomize**: 환경별 Kubernetes 매니페스트 관리
- **SonarQube**: 코드 품질 분석 및 Quality Gate
- **Azure Container Registry (ACR)**: 컨테이너 이미지 저장소
- **Azure Kubernetes Service (AKS)**: 배포 대상 클러스터

## 🔧 사전 준비사항

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

**Azure Service Principal**
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

**ACR Credentials**
```
- Kind: Username with password
- ID: acr-credentials
- Username: acrdigitalgarage01
- Password: {ACR_PASSWORD}
```

**Docker Hub Credentials** (Rate Limit 해결용)
```
- Kind: Username with password
- ID: dockerhub-credentials
- Username: {DOCKERHUB_USERNAME}
- Password: {DOCKERHUB_PASSWORD}
참고: Docker Hub 무료 계정 생성 (https://hub.docker.com)
```

**SonarQube Token**
```
- Kind: Secret text
- ID: sonarqube-token
- Secret: {SonarQube토큰}
```

### 2. SonarQube 프로젝트 설정

각 서비스별 프로젝트 생성 및 Quality Gate 설정:
```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

## 📁 디렉토리 구조

구축 완료된 디렉토리 구조:
```
deployment/cicd/
├── kustomize/
│   ├── base/
│   │   ├── common/
│   │   │   ├── cm-common.yaml
│   │   │   ├── secret-common.yaml
│   │   │   ├── secret-imagepull.yaml
│   │   │   └── ingress.yaml
│   │   ├── api-gateway/
│   │   ├── user-service/
│   │   ├── bill-service/
│   │   ├── product-service/
│   │   ├── kos-mock/
│   │   ├── namespace.yaml
│   │   └── kustomization.yaml
│   └── overlays/
│       ├── dev/
│       ├── staging/
│       └── prod/
├── config/
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/
│   ├── deploy.sh
│   └── validate-cicd-setup.sh
└── Jenkinsfile
```

## 🚀 파이프라인 단계

### 1. Get Source
- GitHub에서 소스코드 체크아웃
- 환경별 설정 파일 읽기

### 2. Setup AKS
- Azure CLI로 인증
- AKS 클러스터 연결
- 환경별 네임스페이스 생성

### 3. Build & SonarQube Analysis
- Gradle 빌드 (테스트 제외)
- 각 서비스별 단위 테스트 실행
- JaCoCo 커버리지 리포트 생성
- SonarQube 코드 품질 분석

### 4. Quality Gate
- SonarQube Quality Gate 대기 (10분 타임아웃)
- 품질 기준 미달 시 파이프라인 중단

### 5. Build & Push Images
- Podman을 사용한 컨테이너 이미지 빌드
- 환경별 이미지 태그로 ACR에 푸시
- 30분 타임아웃 설정

### 6. Update Kustomize & Deploy
- Kustomize를 사용한 이미지 태그 업데이트
- Kubernetes 매니페스트 적용
- 배포 상태 확인 (5분 타임아웃)

### 7. Pipeline Complete
- 성공/실패 로깅
- 자동 파드 정리

## 🔄 파이프라인 실행 방법

### Jenkins 파이프라인 Job 생성

1. Jenkins 웹 UI에서 **New Item > Pipeline** 선택
2. **Pipeline script from SCM** 설정:
   ```
   SCM: Git
   Repository URL: {Git저장소URL}
   Branch: main
   Script Path: deployment/cicd/Jenkinsfile
   ```
3. **Pipeline Parameters** 설정:
   ```
   ENVIRONMENT: Choice Parameter (dev, staging, prod)
   IMAGE_TAG: String Parameter (default: latest)
   ```

### 배포 실행

1. Jenkins > {프로젝트명} > **Build with Parameters**
2. **ENVIRONMENT** 선택 (dev/staging/prod)
3. **IMAGE_TAG** 입력 (선택사항)
4. **Build** 클릭

## 📊 환경별 설정

### DEV 환경
- **네임스페이스**: phonebill-dev
- **Replicas**: 1
- **Resources**: 256m CPU/256Mi Memory → 1024m CPU/1024Mi Memory
- **Database**: DDL update 모드
- **Ingress**: HTTP, SSL 리다이렉션 비활성화

### STAGING 환경
- **네임스페이스**: phonebill-staging
- **Replicas**: 2
- **Resources**: 512m CPU/512Mi Memory → 2048m CPU/2048Mi Memory
- **Database**: DDL validate 모드
- **Ingress**: HTTPS, SSL 리다이렉션 활성화

### PROD 환경
- **네임스페이스**: phonebill-prod
- **Replicas**: 3
- **Resources**: 1024m CPU/1024Mi Memory → 4096m CPU/4096Mi Memory
- **Database**: DDL validate 모드, 짧은 JWT 토큰 (1시간)
- **Ingress**: HTTPS, SSL 리다이렉션 활성화, Let's Encrypt 인증서

## 🛠️ 수동 배포 방법

스크립트를 사용한 수동 배포:
```bash
# DEV 환경 배포
./deployment/cicd/scripts/deploy.sh dev latest

# STAGING 환경 배포
./deployment/cicd/scripts/deploy.sh staging 20241213151500

# PROD 환경 배포
./deployment/cicd/scripts/deploy.sh prod 20241213151500
```

## 📋 배포 상태 확인

```bash
# 파드 상태 확인
kubectl get pods -n phonebill-{환경}

# 서비스 확인
kubectl get services -n phonebill-{환경}

# Ingress 확인
kubectl get ingress -n phonebill-{환경}

# 배포 히스토리 확인
kubectl rollout history deployment/{서비스명} -n phonebill-{환경}
```

## 🔄 롤백 방법

### 이전 리비전으로 롤백
```bash
# 특정 버전으로 롤백
kubectl rollout undo deployment/{서비스명} -n phonebill-{환경} --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{서비스명} -n phonebill-{환경}
```

### 이미지 태그 기반 롤백
```bash
# 이전 안정 버전 이미지 태그로 업데이트
cd deployment/cicd/kustomize/overlays/{환경}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/{서비스명}:{환경}-{이전태그}
kubectl apply -k .
```

## 🔍 트러블슈팅

### 일반적인 문제 해결

**1. SonarQube Quality Gate 실패**
- 코드 커버리지 확인 (80% 이상)
- 코드 중복도 확인 (3% 이하)
- 보안/신뢰성 등급 확인 (A등급)

**2. 컨테이너 이미지 빌드 실패**
- Dockerfile 경로 확인: `deployment/container/Dockerfile-backend`
- JAR 파일 경로 확인: `{서비스명}/build/libs/{서비스명}.jar`
- ACR 인증 상태 확인

**3. 배포 실패**
- Kubernetes 매니페스트 문법 확인
- 네임스페이스 존재 확인
- 리소스 할당량 확인

**4. 파드 시작 실패**
- 환경변수 설정 확인
- Secret/ConfigMap 존재 확인
- 이미지 태그 정확성 확인

### 검증 스크립트

리소스 누락 검증:
```bash
./deployment/cicd/scripts/validate-cicd-setup.sh
```

## 🔐 보안 고려사항

### Jenkins 보안
- **Service Account**: jenkins 전용 계정 사용
- **Pod Security**: 최소 권한 원칙 적용
- **Credential 관리**: Jenkins Credential Store 사용

### 컨테이너 보안
- **Base Image**: 공식 이미지 사용
- **Image Scanning**: ACR 취약점 스캔 활용
- **Secrets 관리**: Kubernetes Secret으로 관리

### 네트워크 보안
- **TLS**: HTTPS 강제 적용 (Staging/Prod)
- **Network Policy**: 네임스페이스 격리
- **Ingress**: 인증서 자동 갱신

## 📈 성능 최적화

### 빌드 최적화
- **Gradle Daemon**: 빌드 속도 향상
- **Docker Layer Caching**: 이미지 빌드 최적화
- **Parallel Build**: 병렬 빌드 활용

### 배포 최적화
- **Rolling Update**: 무중단 배포
- **Health Check**: 정확한 상태 확인
- **Resource Limit**: 적절한 리소스 할당

## 🔧 유지보수 가이드

### 정기 점검 항목
- [ ] Jenkins 플러그인 업데이트
- [ ] SonarQube 룰 세트 검토
- [ ] ACR 이미지 정리
- [ ] 인증서 만료일 확인

### 모니터링 권장사항
- 빌드 실패율 모니터링
- 배포 소요시간 추적
- Quality Gate 통과율 확인
- 리소스 사용률 모니터링

---

## ✅ 체크리스트

### 사전 준비 완료
- [x] Jenkins 필수 플러그인 설치
- [x] Jenkins Credentials 등록
- [x] SonarQube 프로젝트 설정
- [x] ACR 접근 권한 설정
- [x] AKS 클러스터 연결 설정

### Kustomize 구성 완료
- [x] Base 매니페스트 생성
- [x] 환경별 Overlay 생성
- [x] Patch 파일 작성
- [x] 매니페스트 검증 완료

### 파이프라인 구성 완료
- [x] Jenkinsfile 작성
- [x] 환경별 설정 파일 생성
- [x] 배포 스크립트 작성
- [x] 검증 스크립트 작성

**🎯 모든 구성이 완료되어 Jenkins CI/CD 파이프라인을 실행할 준비가 완료되었습니다!**