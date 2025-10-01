# GitHub Actions CI/CD 파이프라인 가이드 (백엔드)

## 1. 개요

### 프로젝트 정보
- **프로젝트명**: phonebill (통신요금 관리 서비스)
- **서비스 목록**: api-gateway, user-service, bill-service, product-service, kos-mock
- **빌드 도구**: Gradle 8.x with JDK 21
- **컨테이너**: Docker (GitHub Actions 호스팅 환경)
- **배포 대상**: Azure Kubernetes Service (AKS)
- **배포 방식**: Kustomize 기반 환경별 배포

### 인프라 정보
- **Container Registry**: acrdigitalgarage03.azurecr.io
- **Resource Group**: rg-digitalgarage-03
- **AKS Cluster**: aks-digitalgarage-03
- **Namespace**: phonebill-dg0511
- **Image Tag 형식**: `{environment}-dg0511{yyyyMMddHHmmss}`

---

## 2. GitHub Secrets 설정

### 필수 Secrets
GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

| Secret Name | 설명 | 값 예시 |
|-------------|------|---------|
| `AZURE_CREDENTIALS` | Azure Service Principal 인증 정보 | JSON 형식 (아래 참조) |
| `ACR_USERNAME` | Azure Container Registry 사용자명 | acrdigitalgarage03 |
| `ACR_PASSWORD` | Azure Container Registry 비밀번호 | (ACR에서 발급) |
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 (Rate Limit 방지) | your-dockerhub-username |
| `DOCKERHUB_PASSWORD` | Docker Hub 비밀번호 | your-dockerhub-password |
| `SONAR_TOKEN` | SonarQube 인증 토큰 (선택사항) | squ_xxxxxxxxxxxx |
| `SONAR_HOST_URL` | SonarQube 서버 URL (선택사항) | https://sonarqube.example.com |

### Azure Service Principal JSON 형식
```json
{
  "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "clientSecret": "your-client-secret",
  "subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### Azure Service Principal 생성 방법
```bash
# Azure CLI로 Service Principal 생성
az ad sp create-for-rbac --name "phonebill-github-actions" \
  --role contributor \
  --scopes /subscriptions/{subscription-id}/resourceGroups/rg-digitalgarage-03 \
  --sdk-auth

# 출력된 JSON을 AZURE_CREDENTIALS Secret으로 등록
```

---

## 3. Workflow 구조

### 파일 위치
`.github/workflows/backend-cicd.yaml`

### Trigger 조건
```yaml
on:
  push:
    branches: [ main, develop ]
    paths:
      - 'api-gateway/**'
      - 'user-service/**'
      - 'bill-service/**'
      - 'product-service/**'
      - 'kos-mock/**'
      - 'common/**'
      - '.github/**'
  pull_request:
    branches: [ main ]
  workflow_dispatch:
    inputs:
      ENVIRONMENT:
        description: 'Target environment'
        required: true
        default: 'dev'
        type: choice
        options:
          - dev
          - staging
          - prod
      SKIP_SONARQUBE:
        description: 'Skip SonarQube Analysis'
        required: false
        default: 'true'
        type: choice
        options:
          - 'true'
          - 'false'
```

### Job 구조

#### Job 1: build (빌드 및 테스트)
- JDK 21 설정
- Gradle 빌드 (테스트 제외)
- SonarQube 분석 (선택사항)
- 빌드 아티팩트 업로드
- Image Tag 생성 (`dg0511{yyyyMMddHHmmss}`)

#### Job 2: release (컨테이너 이미지 빌드 및 푸시)
- Docker Buildx 설정
- Docker Hub 로그인 (Rate Limit 방지)
- ACR 로그인
- 서비스별 이미지 빌드 및 푸시
- 이미지 태그: `{environment}-{image_tag}`

#### Job 3: deploy (Kubernetes 배포)
- Azure CLI 설치
- Azure 로그인
- AKS 자격증명 획득
- Namespace 생성
- Kustomize 이미지 태그 업데이트
- Kubernetes 배포 실행
- 배포 상태 확인 (300초 타임아웃)
- 서비스 정보 출력

---

## 4. Kustomize 구성 (Jenkins와 공유)

### 디렉토리 구조
```
deployment/cicd/kustomize/
├── base/
│   ├── common/
│   │   ├── cm-common.yaml
│   │   ├── secret-common.yaml
│   │   ├── secret-imagepull.yaml
│   │   └── ingress.yaml
│   ├── api-gateway/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── cm-api-gateway.yaml
│   ├── [각 서비스별 디렉토리]
│   └── kustomization.yaml
├── overlays/
│   ├── dev/
│   │   ├── kustomization.yaml
│   │   ├── cm-common-patch.yaml
│   │   ├── deployment-{service}-patch.yaml
│   │   └── secret-{resource}-patch.yaml
│   ├── staging/
│   └── prod/
```

### 이미지 태그 업데이트 방식
```bash
# Workflow에서 실행되는 명령
cd deployment/cicd/kustomize/overlays/${{ env.ENVIRONMENT }}

kustomize edit set image \
  acrdigitalgarage03.azurecr.io/phonebill/api-gateway:${{ env.ENVIRONMENT }}-${{ env.IMAGE_TAG }}

kubectl apply -k .
```

---

## 5. 배포 방법

### 5.1 자동 배포 (코드 푸시 시)
```bash
# main 또는 develop 브랜치에 푸시하면 자동 실행
git add .
git commit -m "백엔드 서비스 업데이트"
git push origin main
```

### 5.2 수동 배포 (GitHub UI)
1. GitHub Repository → Actions 탭
2. "Backend Services CI/CD" Workflow 선택
3. "Run workflow" 버튼 클릭
4. 환경 선택 (dev/staging/prod)
5. SonarQube 분석 스킵 여부 선택
6. "Run workflow" 실행

### 5.3 수동 배포 (스크립트)
```bash
# 스크립트 실행 권한 부여
chmod +x .github/scripts/deploy-actions.sh

# 배포 실행
.github/scripts/deploy-actions.sh dev dg0511{timestamp}

# 예시
.github/scripts/deploy-actions.sh dev dg051120250101120000
```

---

## 6. SonarQube 통합 (선택사항)

### 6.1 SonarQube 설정
```yaml
# workflow에서 SKIP_SONARQUBE='false'로 설정 시 실행
- name: SonarQube Analysis & Quality Gate
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
  run: |
    services=(api-gateway user-service bill-service product-service kos-mock)
    for service in "${services[@]}"; do
      ./gradlew :$service:test :$service:jacocoTestReport :$service:sonar \
        -Dsonar.projectKey=phonebill-$service-${{ env.ENVIRONMENT }} \
        -Dsonar.projectName=phonebill-$service-${{ env.ENVIRONMENT }} \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.token=$SONAR_TOKEN
    done
```

### 6.2 SonarQube 제외 설정
```properties
sonar.exclusions=**/config/**,**/entity/**,**/dto/**,**/*Application.class,**/exception/**
sonar.java.binaries=build/classes/java/main
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
```

---

## 7. 롤백 방법

### 7.1 이전 버전으로 롤백
```bash
# AKS 자격증명 획득
az aks get-credentials --resource-group rg-digitalgarage-03 --name aks-digitalgarage-03

# 이전 이미지 태그로 변경
cd deployment/cicd/kustomize/overlays/dev
kustomize edit set image acrdigitalgarage03.azurecr.io/phonebill/api-gateway:dev-dg051120250101100000

# 배포 실행
kubectl apply -k .

# 롤백 상태 확인
kubectl rollout status deployment/api-gateway -n phonebill-dg0511
```

### 7.2 Kubernetes 기본 롤백
```bash
# 직전 버전으로 롤백
kubectl rollout undo deployment/api-gateway -n phonebill-dg0511

# 특정 리비전으로 롤백
kubectl rollout history deployment/api-gateway -n phonebill-dg0511
kubectl rollout undo deployment/api-gateway --to-revision=3 -n phonebill-dg0511
```

---

## 8. 트러블슈팅

### 8.1 빌드 실패
**증상**: Gradle 빌드 중 오류 발생
```
해결방법:
1. Actions 로그에서 에러 메시지 확인
2. 로컬에서 동일 명령 실행: ./gradlew build -x test
3. 의존성 문제 시 gradle/wrapper/gradle-wrapper.properties 확인
4. JDK 버전 확인 (21 사용 중)
```

### 8.2 이미지 푸시 실패
**증상**: ACR 푸시 권한 오류
```
해결방법:
1. ACR_USERNAME, ACR_PASSWORD Secret 값 확인
2. Azure Portal → Container Registry → Access keys 확인
3. Admin user 활성화 여부 확인
```

### 8.3 배포 타임아웃
**증상**: `kubectl wait --timeout=300s` 타임아웃 발생
```
해결방법:
1. Pod 상태 확인: kubectl get pods -n phonebill-dg0511
2. 로그 확인: kubectl logs -n phonebill-dg0511 {pod-name}
3. 이미지 Pull 문제 시 imagePullSecrets 확인
4. 리소스 부족 시 노드 상태 확인
```

### 8.4 Kustomize 오류
**증상**: `kustomize edit set image` 실패
```
해결방법:
1. kustomization.yaml 파일 존재 확인
2. 이미지 이름 형식 확인: registry/org/service:tag
3. overlays/{환경} 디렉토리 경로 확인
```

### 8.5 Azure 인증 실패
**증상**: `az aks get-credentials` 실패
```
해결방법:
1. AZURE_CREDENTIALS Secret JSON 형식 확인
2. Service Principal 권한 확인 (Contributor 역할)
3. 구독 ID, Tenant ID 확인
```

---

## 9. GitHub Actions vs Jenkins 비교

| 항목 | GitHub Actions | Jenkins |
|------|----------------|---------|
| **호스팅** | GitHub 클라우드 (ubuntu-latest) | Kubernetes Pod (자체 관리) |
| **비용** | Public repo 무료, Private 월 2,000분 | 인프라 비용 (컴퓨팅 리소스) |
| **설정 위치** | `.github/workflows/backend-cicd.yaml` | `deployment/cicd/Jenkinsfile` |
| **빌드 환경** | ubuntu-latest, JDK 21, Gradle cache | Pod template (Gradle, Podman, Azure-CLI) |
| **컨테이너 빌드** | Docker Buildx | Podman (rootless) |
| **실행 트리거** | push, PR, workflow_dispatch | 수동 또는 Webhook |
| **병렬 처리** | Job 단위 자동 병렬화 | Stage 단위 순차 실행 |
| **캐싱** | GitHub Cache Action | emptyDirVolume (Pod 수명주기) |
| **Pod 관리** | 불필요 (GitHub 관리) | podRetention: never() 자동 정리 |
| **Secrets 관리** | GitHub Secrets | Jenkins Credentials |
| **Kustomize 경로** | `deployment/cicd/kustomize` | `deployment/cicd/kustomize` (동일) |
| **이미지 태그** | `{env}-dg0511{timestamp}` | `{env}-dg0511{timestamp}` (동일) |
| **환경 선택** | workflow_dispatch 입력 | Jenkins 파라미터 |
| **로그 보관** | 90일 (기본값) | Jenkins 설정에 따름 |

---

## 10. 체크리스트

### 초기 설정 체크리스트
- [ ] GitHub Secrets 등록 완료 (AZURE_CREDENTIALS, ACR_USERNAME, ACR_PASSWORD)
- [ ] Docker Hub Secrets 등록 (DOCKERHUB_USERNAME, DOCKERHUB_PASSWORD)
- [ ] Azure Service Principal 생성 및 권한 부여
- [ ] ACR Admin user 활성화
- [ ] AKS 클러스터 접근 권한 확인
- [ ] SonarQube 설정 (선택사항)
- [ ] Kustomize 디렉토리 구조 확인 (`deployment/cicd/kustomize/`)
- [ ] 환경별 패치 파일 작성 (dev/staging/prod)

### 배포 전 체크리스트
- [ ] 배포할 브랜치 확인 (main/develop)
- [ ] 환경 선택 확인 (dev/staging/prod)
- [ ] 이미지 태그 형식 확인 (`{env}-dg0511{timestamp}`)
- [ ] Kustomize 설정 검증 (`kubectl kustomize overlays/{env}`)
- [ ] 네임스페이스 존재 확인 (`kubectl get ns phonebill-dg0511`)

### 배포 후 체크리스트
- [ ] Actions 로그에서 모든 Job 성공 확인
- [ ] Pod 상태 확인 (`kubectl get pods -n phonebill-dg0511`)
- [ ] Service 엔드포인트 확인 (`kubectl get svc -n phonebill-dg0511`)
- [ ] Ingress 설정 확인 (`kubectl get ingress -n phonebill-dg0511`)
- [ ] Health check 엔드포인트 테스트
- [ ] 로그 확인 (`kubectl logs -n phonebill-dg0511 {pod-name}`)

---

## 11. 참고 자료

### GitHub Actions 공식 문서
- [Workflow 문법](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [Secrets 관리](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Azure Login Action](https://github.com/Azure/login)
- [Docker Buildx Action](https://github.com/docker/setup-buildx-action)

### Azure 공식 문서
- [Azure Service Principal 생성](https://learn.microsoft.com/en-us/azure/aks/kubernetes-service-principal)
- [AKS 인증](https://learn.microsoft.com/en-us/azure/aks/control-kubeconfig-access)
- [ACR 인증](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-authentication)

### Kustomize 공식 문서
- [Kustomize 소개](https://kustomize.io/)
- [Strategic Merge Patch](https://kubectl.docs.kubernetes.io/references/kustomize/kustomization/patches/)

### 프로젝트 문서
- Jenkins 파이프라인 가이드: `deployment/cicd/jenkins-pipeline-guide.md`
- Kustomize Base: `deployment/cicd/kustomize/base/`
- 환경별 Overlays: `deployment/cicd/kustomize/overlays/{dev|staging|prod}/`

---

## 문서 관리 정보
- **작성일**: 2025-10-01
- **버전**: 1.0
- **최종 수정**: 2025-10-01
- **관리 위치**: `.github/actions-pipeline-guide.md`
