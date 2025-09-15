# 백엔드 GitHub Actions 파이프라인 구축 가이드

## 📋 프로젝트 정보

**시스템명**: phonebill
**서비스 목록**: api-gateway, user-service, bill-service, product-service, kos-mock
**JDK 버전**: 21
**실행 환경**:
- **ACR**: acrdigitalgarage01
- **리소스 그룹**: rg-digitalgarage-01
- **AKS 클러스터**: aks-digitalgarage-01

## 🚀 GitHub Repository 환경 구성

### Repository Secrets 설정
`Repository Settings > Secrets and variables > Actions > Repository secrets`에 등록:

```bash
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

# SonarQube (선택사항)
SONAR_TOKEN: {SonarQube토큰}
SONAR_HOST_URL: {SonarQube서버URL}

# Docker Hub (Rate Limit 해결용, 선택사항)
DOCKERHUB_USERNAME: {Docker Hub 사용자명}
DOCKERHUB_PASSWORD: {Docker Hub 패스워드}
```

### Repository Variables 설정
`Repository Settings > Secrets and variables > Actions > Variables > Repository variables`에 등록:

```bash
# Workflow 제어 변수
ENVIRONMENT: dev (기본값, 수동실행시 선택 가능: dev/staging/prod)
SKIP_SONARQUBE: true (기본값, 수동실행시 선택 가능: true/false)
```

### 사용 방법
- **자동 실행**: Push/PR 시 기본값 사용 (ENVIRONMENT=dev, SKIP_SONARQUBE=true)
- **수동 실행**: Actions 탭 > "Backend Services CI/CD" > "Run workflow" 버튼 클릭
  - Environment: dev/staging/prod 선택
  - Skip SonarQube Analysis: true/false 선택

## 📁 디렉토리 구조

```
.github/
├── kustomize/                    # GitHub Actions 전용 Kustomize 매니페스트
│   ├── base/                     # 기본 매니페스트
│   │   ├── kustomization.yaml
│   │   ├── namespace.yaml
│   │   ├── common/               # 공통 리소스
│   │   │   ├── cm-common.yaml
│   │   │   ├── secret-common.yaml
│   │   │   ├── secret-imagepull.yaml
│   │   │   └── ingress.yaml
│   │   ├── api-gateway/          # API Gateway 리소스
│   │   ├── user-service/         # User Service 리소스
│   │   ├── bill-service/         # Bill Service 리소스
│   │   ├── product-service/      # Product Service 리소스
│   │   └── kos-mock/             # KOS Mock 리소스
│   └── overlays/                 # 환경별 오버레이
│       ├── dev/                  # 개발 환경
│       ├── staging/              # 스테이징 환경
│       └── prod/                 # 운영 환경
├── config/                       # 환경별 배포 설정
│   ├── deploy_env_vars_dev
│   ├── deploy_env_vars_staging
│   └── deploy_env_vars_prod
├── scripts/                      # 배포 스크립트
│   └── deploy-actions.sh
└── workflows/                    # GitHub Actions 워크플로우
    └── backend-cicd.yaml
```

## 🔄 환경별 설정

### DEV 환경
- **네임스페이스**: phonebill-dev
- **프로파일**: dev
- **DDL 모드**: update
- **리플리카**: 1개
- **리소스**: 256Mi/256m (요청), 1024Mi/1024m (제한)
- **도메인**: phonebill-api.20.214.196.128.nip.io (기존과 동일)
- **HTTPS**: 비활성화

### STAGING 환경
- **네임스페이스**: phonebill-staging
- **프로파일**: staging
- **DDL 모드**: validate
- **리플리카**: 2개
- **리소스**: 512Mi/512m (요청), 2048Mi/2048m (제한)
- **도메인**: phonebill.staging.example.com
- **HTTPS**: 활성화 (ssl-redirect: true)

### PROD 환경
- **네임스페이스**: phonebill-prod
- **프로파일**: prod
- **DDL 모드**: validate
- **리플리카**: 3개
- **리소스**: 1024Mi/1024m (요청), 4096Mi/4096m (제한)
- **도메인**: phonebill.example.com
- **HTTPS**: 활성화 (ssl-redirect: true)
- **JWT 토큰**: 보안 강화 (ACCESS: 1시간, REFRESH: 12시간)

## 🚀 배포 방법

### 1. 자동 배포 (GitHub Actions)

**코드 Push 시 자동 실행**:
```bash
git add .
git commit -m "feature: 새 기능 추가"
git push origin main  # 또는 develop
```

**수동 트리거**:
1. GitHub > Actions 탭 이동
2. "Backend Services CI/CD" 선택
3. "Run workflow" 클릭
4. 환경(dev/staging/prod) 및 SonarQube 분석 여부 선택
5. "Run workflow" 실행

### 2. 수동 배포 (로컬)

```bash
# 개발 환경 배포
./.github/scripts/deploy-actions.sh dev latest

# 스테이징 환경 배포
./.github/scripts/deploy-actions.sh staging 20241215120000

# 운영 환경 배포
./.github/scripts/deploy-actions.sh prod 20241215120000
```

## 🔙 롤백 방법

### 1. GitHub Actions 롤백
```bash
# 이전 성공한 워크플로우 실행으로 롤백
1. GitHub > Actions > 성공한 이전 워크플로우 선택
2. "Re-run all jobs" 클릭
```

### 2. kubectl 롤백
```bash
# 특정 버전으로 롤백
kubectl rollout undo deployment/{환경}-{서비스명} -n phonebill-{환경} --to-revision=2

# 롤백 상태 확인
kubectl rollout status deployment/{환경}-{서비스명} -n phonebill-{환경}
```

### 3. 수동 스크립트 롤백
```bash
# 이전 안정 버전 이미지 태그로 배포
./.github/scripts/deploy-actions.sh {환경} {이전태그}
```

## 📊 SonarQube 프로젝트 설정

각 서비스별 프로젝트 생성 및 Quality Gate 설정:
```bash
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

## ✅ 체크리스트

### 사전 준비
- [ ] GitHub Repository Secrets 설정 완료
- [ ] GitHub Repository Variables 설정 완료
- [ ] Azure Service Principal 권한 확인
- [ ] ACR 접근 권한 확인
- [ ] AKS 클러스터 접근 권한 확인

### 배포 확인
- [ ] GitHub Actions 워크플로우 정상 실행
- [ ] 모든 서비스 이미지 빌드 및 푸시 성공
- [ ] Kustomize 매니페스트 적용 성공
- [ ] 모든 Deployment 정상 배포 (Available 상태)
- [ ] Health Check 통과
- [ ] Ingress 정상 동작 확인

### 서비스 검증
- [ ] API Gateway 응답 확인: `curl -f http://localhost:8080/actuator/health`
- [ ] 각 서비스별 Pod 상태 확인: `kubectl get pods -n phonebill-{환경}`
- [ ] 서비스 연결 확인: `kubectl get services -n phonebill-{환경}`
- [ ] Ingress 설정 확인: `kubectl get ingress -n phonebill-{환경}`

## 🔧 문제 해결

### 일반적인 문제
1. **이미지 빌드 실패**: Dockerfile 경로 및 빌드 컨텍스트 확인
2. **매니페스트 적용 실패**: Kustomize 구문 오류 확인
3. **Pod 시작 실패**: 환경변수 및 Secret 설정 확인
4. **Health Check 실패**: 애플리케이션 로그 확인

### 로그 확인 명령어
```bash
# Pod 로그 확인
kubectl logs -n phonebill-{환경} {pod-name}

# Deployment 상태 확인
kubectl describe deployment -n phonebill-{환경} {deployment-name}

# 이벤트 확인
kubectl get events -n phonebill-{환경} --sort-by='.lastTimestamp'
```

---

## 📞 지원

구축 과정에서 문제가 발생하거나 추가 지원이 필요한 경우, 다음 사항을 포함하여 문의:
1. 환경 정보 (dev/staging/prod)
2. 오류 메시지 및 로그
3. 실행한 명령어
4. 현재 상태 (kubectl get all -n phonebill-{환경})

**구축 완료 🎉**