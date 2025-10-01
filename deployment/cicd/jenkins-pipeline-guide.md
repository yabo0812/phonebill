# 백엔드 Jenkins CI/CD 파이프라인 가이드

## 1. 개요

본 문서는 **phonebill** 프로젝트의 Jenkins + Kustomize 기반 CI/CD 파이프라인 구축 방법을 설명합니다.

### 1.1 프로젝트 정보
- **시스템명**: phonebill
- **JDK 버전**: 21
- **빌드 도구**: Gradle
- **컨테이너 레지스트리**: ACR (acrdigitalgarage03.azurecr.io)
- **쿠버네티스 클러스터**: aks-digitalgarage-03
- **네임스페이스**: phonebill-dg0511

### 1.2 서비스 목록
1. `api-gateway` - API Gateway 서비스
2. `user-service` - 사용자 인증 서비스
3. `bill-service` - 요금 조회 서비스
4. `product-service` - 상품 변경 서비스
5. `kos-mock` - KOS Mock 서비스

### 1.3 디렉토리 구조
```
deployment/cicd/
├── Jenkinsfile                    # Jenkins 파이프라인 스크립트
├── config/                        # 환경별 설정 파일
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/                       # 배포 스크립트
│   ├── deploy.sh
│   └── validate-cicd-setup.sh
└── kustomize/                     # Kustomize 매니페스트
    ├── base/                      # Base 리소스
    │   ├── kustomization.yaml
    │   ├── common/                # 공통 리소스
    │   ├── api-gateway/
    │   ├── user-service/
    │   ├── bill-service/
    │   ├── product-service/
    │   └── kos-mock/
    └── overlays/                  # 환경별 Overlay
        ├── dev/
        │   ├── kustomization.yaml
        │   ├── cm-common-patch.yaml
        │   ├── secret-common-patch.yaml
        │   ├── ingress-patch.yaml
        │   ├── deployment-*-patch.yaml
        │   └── secret-*-patch.yaml
        ├── staging/
        └── prod/
```

---

## 2. 사전 준비

### 2.1 Jenkins 서버 환경 구성

#### 2.1.1 필수 플러그인 설치
Jenkins > Manage Jenkins > Plugin Manager에서 다음 플러그인을 설치합니다:
- **Kubernetes**
- **Pipeline Utility Steps**
- **Docker Pipeline**
- **GitHub**
- **SonarQube Scanner**
- **Azure Credentials**

#### 2.1.2 Jenkins Credentials 등록

**1) Azure Service Principal**
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

**2) ACR Credentials**
```
- Kind: Username with password
- ID: acr-credentials
- Username: acrdigitalgarage03
- Password: {ACR_PASSWORD}
```

**3) Docker Hub Credentials (Rate Limit 해결용)**
```
- Kind: Username with password
- ID: dockerhub-credentials
- Username: {DOCKERHUB_USERNAME}
- Password: {DOCKERHUB_PASSWORD}
```
> **참고**: Docker Hub 무료 계정 생성 필요 (https://hub.docker.com)

**4) SonarQube Token**
```
- Kind: Secret text
- ID: sonarqube-token
- Secret: {SonarQube토큰}
```

---

## 3. Kustomize 구성 상세

### 3.1 Base Kustomization 구조

`deployment/cicd/kustomize/base/kustomization.yaml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

metadata:
  name: phonebill-base

resources:
  # Common resources
  - common/cm-common.yaml
  - common/secret-common.yaml
  - common/secret-imagepull.yaml
  - common/ingress.yaml

  # 각 서비스별 리소스
  - api-gateway/deployment.yaml
  - api-gateway/service.yaml
  - api-gateway/cm-api-gateway.yaml
  # ... (다른 서비스들 동일 패턴)

commonLabels:
  app: phonebill
  version: v1

images:
  - name: acrdigitalgarage03.azurecr.io/phonebill/api-gateway
    newTag: latest
  # ... (다른 서비스들 동일 패턴)
```

### 3.2 환경별 Overlay 구성

#### 3.2.1 DEV 환경
- **Replicas**: 모든 서비스 1 replica
- **Resources**:
  - Requests: CPU 256m, Memory 256Mi
  - Limits: CPU 1024m, Memory 1024Mi
- **프로파일**: `SPRING_PROFILES_ACTIVE=dev`
- **DDL**: `DDL_AUTO=update`
- **Ingress**: HTTP only (ssl-redirect: false)

#### 3.2.2 STAGING 환경
- **Replicas**: 모든 서비스 2 replicas
- **Resources**:
  - Requests: CPU 512m, Memory 512Mi
  - Limits: CPU 2048m, Memory 2048Mi
- **프로파일**: `SPRING_PROFILES_ACTIVE=staging`
- **DDL**: `DDL_AUTO=validate`
- **Ingress**: HTTPS 강제 (ssl-redirect: true), TLS 인증서 설정

#### 3.2.3 PROD 환경
- **Replicas**: 모든 서비스 3 replicas
- **Resources**:
  - Requests: CPU 1024m, Memory 1024Mi
  - Limits: CPU 4096m, Memory 4096Mi
- **프로파일**: `SPRING_PROFILES_ACTIVE=prod`
- **DDL**: `DDL_AUTO=validate`
- **JWT 토큰**: 보안을 위해 짧은 유효시간 (7200000ms = 2시간)
- **Ingress**: HTTPS 강제 (ssl-redirect: true), TLS 인증서 설정

### 3.3 Patch 파일 작성 원칙

**⚠️ 중요 원칙**:
1. **Base 매니페스트에 없는 항목은 추가하지 않음**
2. **Base 매니페스트와 항목이 일치해야 함**
3. **Secret 매니페스트에서는 'data'가 아닌 'stringData' 사용**
4. **Patch 방법은 `patches` (target 명시) 사용, `patchesStrategicMerge` 사용 금지**

---

## 4. Jenkinsfile 상세 설명

### 4.1 주요 구성 요소

#### 4.1.1 Pod Template
```groovy
podTemplate(
    label: "${PIPELINE_ID}",
    serviceAccount: 'jenkins',
    podRetention: never(),  // 파드 자동 정리
    idleMinutes: 1,
    activeDeadlineSeconds: 3600,
    yaml: '''
        spec:
          terminationGracePeriodSeconds: 3
          restartPolicy: Never
    ''',
    containers: [
        // Gradle JDK21 컨테이너
        // Podman 컨테이너
        // Azure-CLI 컨테이너
    ]
)
```

**파드 자동 정리 설정**:
- `podRetention: never()`: 파이프라인 완료 시 파드 즉시 삭제
- `idleMinutes: 1`: 유휴 시간 1분으로 설정
- `terminationGracePeriodSeconds: 3`: 파드 종료 시 3초 내 강제 종료
- `restartPolicy: Never`: 파드 재시작 방지

#### 4.1.2 파이프라인 Stage

**1) Get Source**
- Git 저장소에서 소스 체크아웃
- 환경별 설정 파일 로드 (`deploy_env_vars_{환경}`)

**2) Setup AKS**
- Azure Service Principal로 로그인
- AKS 클러스터 인증 정보 가져오기
- 네임스페이스 생성 (없을 경우)

**3) Build**
- Gradle로 빌드 수행 (테스트 제외: `-x test`)

**4) SonarQube Analysis & Quality Gate**
- 각 서비스별로 개별 테스트 및 SonarQube 분석 수행
- 서비스별로 Quality Gate 확인
- `SKIP_SONARQUBE=true` 파라미터로 건너뛰기 가능

**5) Build & Push Images**
- Podman으로 컨테이너 이미지 빌드
- 환경별 이미지 태그: `{환경}-{yyyyMMddHHmmss}`
- ACR로 이미지 푸시
- Docker Hub 로그인으로 Rate Limit 회피

**6) Update Kustomize & Deploy**
- Kustomize 설치
- 이미지 태그 업데이트
- kubectl apply -k로 배포
- 각 서비스별 배포 상태 확인 (timeout: 300s)

**7) Pipeline Complete**
- 파이프라인 완료 로그
- 성공/실패 여부 로깅
- 파드 자동 정리

### 4.2 변수 참조 문법 주의사항

**올바른 문법**:
```groovy
sh """
    echo ${props.resource_group}  # Groovy 문자열 보간
"""
```

**잘못된 문법**:
```groovy
sh """
    echo \${props.resource_group}  # 에러 발생!
"""
```

### 4.3 쉘 호환성

Jenkins 컨테이너의 기본 쉘이 `/bin/sh` (dash)인 경우 Bash 배열 문법 미지원:
```bash
# ❌ 잘못된 방법 (Bash 배열)
svc_list=(service1 service2)
for service in "${svc_list[@]}"; do

# ✅ 올바른 방법 (공백 구분 문자열)
services="service1 service2"
for service in $services; do
```

---

## 5. Jenkins Pipeline Job 생성

### 5.1 Pipeline Job 설정
1. Jenkins 웹 UI > New Item > Pipeline 선택
2. Pipeline 설정:
   ```
   Definition: Pipeline script from SCM
   SCM: Git
   Repository URL: {Git저장소URL}
   Branch: main (또는 develop)
   Script Path: deployment/cicd/Jenkinsfile
   ```

### 5.2 Pipeline Parameters 설정
```
1. ENVIRONMENT: Choice Parameter
   - Choices: dev, staging, prod
   - Default: dev

2. IMAGE_TAG: String Parameter
   - Default: latest

3. SKIP_SONARQUBE: String Parameter
   - Default: true
   - Description: SonarQube 분석 건너뛰기 (true/false)
```

---

## 6. SonarQube 설정

### 6.1 프로젝트 생성
SonarQube에서 각 서비스별로 프로젝트 생성:
- phonebill-api-gateway-dev
- phonebill-user-service-dev
- phonebill-bill-service-dev
- phonebill-product-service-dev
- phonebill-kos-mock-dev

### 6.2 Quality Gate 설정
```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

---

## 7. 배포 실행 방법

### 7.1 Jenkins 파이프라인 실행
1. Jenkins > {프로젝트명} > **Build with Parameters**
2. **ENVIRONMENT** 선택 (dev/staging/prod)
3. **IMAGE_TAG** 입력 (선택사항, 기본값: latest)
4. **SKIP_SONARQUBE** 입력 (SonarQube 분석 건너뛰려면 "true", 실행하려면 "false")
5. **Build** 클릭

### 7.2 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n phonebill-dg0511

# Service 상태 확인
kubectl get services -n phonebill-dg0511

# Ingress 상태 확인
kubectl get ingress -n phonebill-dg0511

# 특정 서비스 로그 확인
kubectl logs -n phonebill-dg0511 deployment/user-service -f
```

### 7.3 수동 배포 스크립트 실행
```bash
# DEV 환경 배포
./deployment/cicd/scripts/deploy.sh dev 20250101120000

# STAGING 환경 배포
./deployment/cicd/scripts/deploy.sh staging 20250101120000

# PROD 환경 배포
./deployment/cicd/scripts/deploy.sh prod 20250101120000
```

---

## 8. 롤백 방법

### 8.1 Kubernetes 롤백
```bash
# 특정 버전으로 롤백
kubectl rollout undo deployment/user-service -n phonebill-dg0511 --to-revision=2

# 롤백 이력 확인
kubectl rollout history deployment/user-service -n phonebill-dg0511

# 롤백 상태 확인
kubectl rollout status deployment/user-service -n phonebill-dg0511
```

### 8.2 이미지 태그 기반 롤백
```bash
# 이전 안정 버전 이미지 태그로 업데이트
cd deployment/cicd/kustomize/overlays/dev
kustomize edit set image acrdigitalgarage03.azurecr.io/phonebill/user-service:dev-20241231120000
kubectl apply -k .
```

---

## 9. 리소스 검증

### 9.1 검증 스크립트 실행
```bash
./deployment/cicd/scripts/validate-cicd-setup.sh
```

### 9.2 검증 항목
1. ✅ 각 서비스 디렉토리의 필수 파일 존재 (deployment.yaml, service.yaml)
2. ✅ ConfigMap, Secret 파일 명명 규칙 준수
3. ✅ Common 리소스 존재
4. ✅ kustomization.yaml과 실제 파일 일치
5. ✅ Base kustomization 빌드 성공
6. ✅ 환경별 overlay 빌드 성공

### 9.3 문제 해결 가이드
```
문제가 발견되면 다음을 확인:
1. 누락된 파일들을 base 디렉토리에 추가
2. kustomization.yaml에서 존재하지 않는 파일 참조 제거
3. 파일명이 명명 규칙을 따르는지 확인:
   - ConfigMap: cm-{서비스명}.yaml
   - Secret: secret-{서비스명}.yaml
4. 다시 검증: ./scripts/validate-cicd-setup.sh
```

---

## 10. 트러블슈팅

### 10.1 이미지 빌드 실패
**증상**: Podman build 실패
**해결책**:
```bash
# 1. Gradle 빌드가 성공했는지 확인
./gradlew build -x test

# 2. Dockerfile 경로 확인
ls deployment/container/Dockerfile-backend

# 3. 빌드 로그 확인
```

### 10.2 Kustomize 빌드 실패
**증상**: `kubectl apply -k .` 실패
**해결책**:
```bash
# 1. Base kustomization 검증
kubectl kustomize deployment/cicd/kustomize/base/

# 2. 환경별 overlay 검증
kubectl kustomize deployment/cicd/kustomize/overlays/dev/

# 3. 리소스 누락 확인
./deployment/cicd/scripts/validate-cicd-setup.sh
```

### 10.3 SonarQube Quality Gate 실패
**증상**: Quality Gate 통과 실패
**해결책**:
```bash
# 1. 로컬에서 테스트 실행 및 커버리지 확인
./gradlew test jacocoTestReport

# 2. 커버리지 리포트 확인
open user-service/build/reports/jacoco/test/html/index.html

# 3. SonarQube 건너뛰고 배포 (임시)
SKIP_SONARQUBE=true로 빌드
```

### 10.4 Pod 배포 대기 시간 초과
**증상**: `kubectl wait --for=condition=available` 타임아웃
**해결책**:
```bash
# 1. Pod 상태 확인
kubectl get pods -n phonebill-dg0511

# 2. Pod 상세 정보 및 이벤트 확인
kubectl describe pod <pod-name> -n phonebill-dg0511

# 3. Pod 로그 확인
kubectl logs <pod-name> -n phonebill-dg0511 -f

# 4. 이미지 Pull 실패인 경우
kubectl get events -n phonebill-dg0511 | grep ImagePull
```

---

## 11. 체크리스트

### 11.1 사전 준비 체크리스트
- [ ] Jenkins 필수 플러그인 설치 완료
- [ ] Azure Service Principal Credentials 등록 완료
- [ ] ACR Credentials 등록 완료
- [ ] Docker Hub Credentials 등록 완료
- [ ] SonarQube Token 등록 완료

### 11.2 Kustomize 구조 체크리스트
- [ ] Base 디렉토리 구조 생성 완료
- [ ] 서비스별 base 디렉토리 생성 완료
- [ ] 기존 k8s 매니페스트 복사 완료
- [ ] Base kustomization.yaml 작성 완료
- [ ] 환경별 Overlay 구성 완료 (dev/staging/prod)
- [ ] 환경별 Patch 파일 생성 완료
- [ ] 리소스 검증 스크립트 실행 완료

### 11.3 Jenkins Pipeline 체크리스트
- [ ] Jenkinsfile 작성 완료
- [ ] Pipeline Job 생성 완료
- [ ] Pipeline Parameters 설정 완료
- [ ] 환경별 설정 파일 작성 완료
- [ ] 수동 배포 스크립트 작성 완료
- [ ] 스크립트 실행 권한 설정 완료

### 11.4 배포 검증 체크리스트
- [ ] DEV 환경 배포 성공
- [ ] STAGING 환경 배포 성공
- [ ] PROD 환경 배포 성공
- [ ] 모든 Pod 정상 실행 확인
- [ ] Ingress 정상 동작 확인
- [ ] 서비스 간 통신 확인

---

## 12. 참고 자료

### 12.1 공식 문서
- Kustomize: https://kustomize.io/
- Jenkins Kubernetes Plugin: https://plugins.jenkins.io/kubernetes/
- SonarQube: https://docs.sonarqube.org/

### 12.2 내부 문서
- 백엔드컨테이너이미지작성가이드: `deployment/container/README.md`
- 백엔드배포가이드: `deployment/k8s/README.md`
- 데이터베이스설치가이드: `develop/database/README.md`

---

**작성일**: 2025-10-01
**작성자**: 백엔더 (이개발)
**버전**: 1.0
