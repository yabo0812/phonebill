# phonebill Jenkins CI/CD 파이프라인 가이드

## 📋 프로젝트 정보

- **시스템명**: phonebill
- **서비스**: api-gateway, user-service, bill-service, product-service, kos-mock
- **JDK 버전**: 21
- **ACR**: acrdigitalgarage01
- **리소스 그룹**: rg-digitalgarage-01
- **AKS 클러스터**: aks-digitalgarage-01

## 🏗️ Jenkins 서버 환경 구성

### 필수 플러그인 설치
```
- Kubernetes
- Pipeline Utility Steps
- Docker Pipeline
- GitHub
- SonarQube Scanner
- Azure Credentials
```

### Jenkins Credentials 등록

#### 1. Azure Service Principal
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

#### 2. ACR Credentials
```
- Kind: Username with password
- ID: acr-credentials
- Username: acrdigitalgarage01
- Password: {ACR_PASSWORD}
```

#### 3. SonarQube Token
```
- Kind: Secret text
- ID: sonarqube-token
- Secret: {SonarQube토큰}
```

## 📁 Kustomize 디렉토리 구조

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
│   │   ├── api-gateway/
│   │   ├── user-service/
│   │   ├── bill-service/
│   │   ├── product-service/
│   │   └── kos-mock/
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

## 🔧 환경별 설정

### DEV 환경
- **Namespace**: phonebill-dev
- **Ingress Host**: phonebill-api.20.214.196.128.nip.io ⚠️ **base와 동일해야 함**
- **Replicas**: 1
- **Resources**: requests(256m/256Mi), limits(1024m/1024Mi)
- **Profile**: dev
- **DDL**: update
- **SSL**: false

### STAGING 환경
- **Namespace**: phonebill-staging
- **Ingress Host**: phonebill.staging-domain.com ⚠️ **환경별 도메인**
- **Replicas**: 2
- **Resources**: requests(512m/512Mi), limits(2048m/2048Mi)
- **Profile**: staging
- **DDL**: validate
- **SSL**: true

### PROD 환경
- **Namespace**: phonebill-prod
- **Ingress Host**: phonebill.production-domain.com ⚠️ **프로덕션 도메인**
- **Replicas**: 3
- **Resources**: requests(1024m/1024Mi), limits(4096m/4096Mi)
- **Profile**: prod
- **DDL**: validate
- **SSL**: true
- **JWT 토큰**: 30분 (보안 강화)

## 🚀 Jenkins Pipeline Job 생성

### 1. Jenkins 웹 UI에서 새 작업 생성
- New Item > Pipeline 선택
- 작업명: phonebill-cicd

### 2. Pipeline 설정
```
SCM: Git
Repository URL: {Git저장소URL}
Branch: main
Script Path: deployment/cicd/Jenkinsfile
```

### 3. Pipeline Parameters 설정
```
ENVIRONMENT: Choice Parameter (dev, staging, prod)
IMAGE_TAG: String Parameter (default: latest)
```

## 🎯 SonarQube 프로젝트 설정

### Quality Gate 설정
```
Coverage: >= 80%
Duplicated Lines: <= 3%
Maintainability Rating: <= A
Reliability Rating: <= A
Security Rating: <= A
```

## 📊 배포 실행 방법

### 1. Jenkins 파이프라인 실행
```
1. Jenkins > phonebill-cicd > Build with Parameters
2. ENVIRONMENT 선택 (dev/staging/prod)
3. IMAGE_TAG 입력 (선택사항)
4. Build 클릭
```

### 2. 배포 상태 확인
```bash
kubectl get pods -n phonebill-{환경}
kubectl get services -n phonebill-{환경}
kubectl get ingress -n phonebill-{환경}
```

### 3. 수동 배포 (필요시)
```bash
# DEV 환경 배포
./deployment/cicd/scripts/deploy.sh dev 20241201120000

# STAGING 환경 배포
./deployment/cicd/scripts/deploy.sh staging 20241201120000

# PROD 환경 배포
./deployment/cicd/scripts/deploy.sh prod 20241201120000
```

## 🔄 롤백 방법

### 1. 이전 버전으로 롤백
```bash
kubectl rollout undo deployment/{서비스명} -n phonebill-{환경} --to-revision=2
kubectl rollout status deployment/{서비스명} -n phonebill-{환경}
```

### 2. 이미지 태그 기반 롤백
```bash
cd deployment/cicd/kustomize/overlays/{환경}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/{서비스명}:{환경}-{이전태그}
kubectl apply -k .
```

## 📝 파이프라인 단계별 설명

### Stage 1: Get Source
- Git 저장소에서 소스코드 체크아웃
- 환경별 설정 파일 로드

### Stage 2: Setup AKS
- Azure 로그인 및 AKS 클러스터 연결
- 네임스페이스 생성

### Stage 3: Build & SonarQube Analysis
- Gradle 빌드 및 테스트
- 각 서비스별 SonarQube 코드 품질 분석
- JaCoCo 테스트 커버리지 측정

### Stage 4: Quality Gate
- SonarQube Quality Gate 검증
- 품질 기준 미달 시 파이프라인 중단

### Stage 5: Build & Push Images
- 각 서비스별 컨테이너 이미지 빌드
- ACR에 이미지 푸시

### Stage 6: Update Kustomize & Deploy
- Kustomize를 이용한 이미지 태그 업데이트
- Kubernetes 매니페스트 배포
- 배포 상태 확인

## ⚠️ 주의사항 및 체크리스트 준수사항

### 🚨 **체크리스트 핵심 준수사항**

#### **1. Ingress 설정 규칙** ⚠️ **매우 중요**
```yaml
# ✅ DEV 환경: base와 동일한 host 사용
- host: phonebill-api.20.214.196.128.nip.io  # base와 동일해야 함!

# ✅ STAGING/PROD: 환경별 도메인 사용
- host: phonebill.staging-domain.com    # staging
- host: phonebill.production-domain.com # prod
```

#### **2. Patch 파일 작성 원칙**
- ❌ **금지**: base 매니페스트에 없는 항목 추가
- ✅ **필수**: base 매니페스트와 항목 구조 일치
- ✅ **필수**: Secret에 `stringData` 사용 (`data` 금지)
- ✅ **필수**: `patches` 사용 (`patchesStrategicMerge` 금지)

#### **3. Deployment Patch 필수 항목**
```yaml
# ✅ 반드시 포함해야 할 항목들
spec:
  replicas: {환경별설정}  # dev:1, staging:2, prod:3
  template:
    spec:
      containers:
      - resources:  # 반드시 포함
          requests: {환경별설정}
          limits: {환경별설정}
```

#### **4. Jenkinsfile 검증 포인트**
- ✅ JDK 버전: `gradle:jdk21` (프로젝트 JDK와 일치)
- ✅ 서비스 배열: `['api-gateway', 'user-service', 'bill-service', 'product-service', 'kos-mock']`
- ✅ 변수 문법: `${variable}` (⚠️ `\${variable}` 금지)
- ✅ ACR 이름: `acrdigitalgarage01` (실행정보와 일치)

### 🔍 **배포 전 필수 검증**

#### **파일 구조 검증**
```bash
# 모든 환경별 파일이 존재하는지 확인
find deployment/cicd/kustomize/overlays -name "*.yaml" | wc -l
# 결과: 36개 파일이 있어야 함 (각 환경별 12개씩)
```

#### **Kustomize 빌드 테스트**
```bash
# 각 환경별 매니페스트 빌드 테스트
kubectl kustomize deployment/cicd/kustomize/overlays/dev
kubectl kustomize deployment/cicd/kustomize/overlays/staging  
kubectl kustomize deployment/cicd/kustomize/overlays/prod
# 모두 오류 없이 빌드되어야 함
```

#### **base vs dev ingress 비교**
```bash
# DEV 환경이 base와 동일한 host를 사용하는지 확인
diff deployment/cicd/kustomize/base/common/ingress.yaml deployment/cicd/kustomize/overlays/dev/ingress-patch.yaml
# host 라인에서 차이가 없어야 함
```

### 🛡️ **보안 및 운영**

#### **보안 체크**
- 모든 `change-in-production` 패스워드를 실제 값으로 변경
- 프로덕션 환경 도메인 설정 확인
- SSL 인증서 설정 검증
- JWT Secret 키 프로덕션 전용으로 변경

#### **성능 모니터링**
- 각 환경별 리소스 할당량 모니터링
- 배포 시 트래픽 영향도 확인
- Pod 리소스 사용량 추적

#### **운영 모니터링**
- 배포 후 서비스 상태 확인
- 로그 및 메트릭 모니터링
- 헬스체크 엔드포인트 확인

## 🆘 문제 해결 및 실수 방지

### **자주 발생하는 실수들**

#### **🚨 1. Ingress Host 실수**
**문제**: DEV 환경에서 host를 `phonebill-dev-api.xxx`로 변경
**해결**: DEV는 반드시 base와 동일한 host 사용
```bash
# 실수 방지 검증 명령
grep "host:" deployment/cicd/kustomize/base/common/ingress.yaml
grep "host:" deployment/cicd/kustomize/overlays/dev/ingress-patch.yaml
# 두 결과가 동일해야 함
```

#### **🚨 2. Secret에서 data 사용 실수**
**문제**: `data` 필드 사용으로 base64 인코딩 필요
**해결**: 항상 `stringData` 사용
```yaml
# ❌ 잘못된 방법
data:
  DB_PASSWORD: "cGFzc3dvcmQ="  # base64 인코딩 필요

# ✅ 올바른 방법  
stringData:
  DB_PASSWORD: "password"      # 평문 직접 입력
```

#### **🚨 3. Deployment Patch 누락 항목**
**문제**: replicas나 resources 누락으로 기본값 사용
**해결**: 반드시 환경별 설정 포함
```yaml
# ✅ 필수 포함 항목
spec:
  replicas: 1  # 반드시 명시
  template:
    spec:
      containers:
      - resources:  # 반드시 포함
          requests:
            cpu: 256m
            memory: 256Mi
          limits:
            cpu: 1024m  
            memory: 1024Mi
```

#### **🚨 4. Jenkinsfile 변수 문법 실수**
**문제**: `\${variable}` 사용으로 "syntax error: bad substitution" 발생
**해결**: Jenkins Groovy에서는 `${variable}` 사용
```groovy
# ❌ 잘못된 문법
sh "echo \${environment}"

# ✅ 올바른 문법
sh "echo ${environment}"
```

### **배포 전 최종 검증 스크립트**
```bash
#!/bin/bash
echo "🔍 Jenkins CI/CD 구성 최종 검증 시작..."

# 1. 파일 개수 확인
echo "1. 파일 개수 검증..."
OVERLAY_FILES=$(find deployment/cicd/kustomize/overlays -name "*.yaml" | wc -l)
if [ $OVERLAY_FILES -eq 36 ]; then
    echo "✅ Overlay 파일 개수 정상 (36개)"
else
    echo "❌ Overlay 파일 개수 오류 ($OVERLAY_FILES개, 36개여야 함)"
fi

# 2. DEV ingress host 검증
echo "2. DEV Ingress Host 검증..."
BASE_HOST=$(grep "host:" deployment/cicd/kustomize/base/common/ingress.yaml | awk '{print $3}')
DEV_HOST=$(grep "host:" deployment/cicd/kustomize/overlays/dev/ingress-patch.yaml | awk '{print $3}')
if [ "$BASE_HOST" = "$DEV_HOST" ]; then
    echo "✅ DEV Ingress Host 정상 ($DEV_HOST)"
else
    echo "❌ DEV Ingress Host 오류 (base: $BASE_HOST, dev: $DEV_HOST)"
fi

# 3. Kustomize 빌드 테스트
echo "3. Kustomize 빌드 테스트..."
for env in dev staging prod; do
    if kubectl kustomize deployment/cicd/kustomize/overlays/$env > /dev/null 2>&1; then
        echo "✅ $env 환경 빌드 성공"
    else
        echo "❌ $env 환경 빌드 실패"
    fi
done

# 4. Jenkinsfile JDK 버전 확인
echo "4. Jenkinsfile JDK 버전 검증..."
if grep -q "gradle:jdk21" deployment/cicd/Jenkinsfile; then
    echo "✅ JDK 21 버전 정상"
else
    echo "❌ JDK 버전 확인 필요"
fi

# 5. Secret stringData 사용 확인
echo "5. Secret stringData 사용 검증..."
if grep -r "stringData:" deployment/cicd/kustomize/overlays/*/secret-*-patch.yaml > /dev/null; then
    echo "✅ stringData 사용 정상"
else
    echo "❌ stringData 사용 확인 필요"
fi

echo "🎯 검증 완료!"
```

### **일반적인 문제 해결**
1. **이미지 빌드 실패**: Dockerfile 경로 및 JAR 파일 확인
2. **배포 실패**: 리소스 할당량 및 네임스페이스 확인  
3. **Quality Gate 실패**: 테스트 커버리지 및 코드 품질 개선
4. **Kustomize 빌드 실패**: patch 파일의 target 매칭 확인

### **로그 확인 방법**
```bash
# Jenkins 빌드 로그 확인 (웹 UI에서)
# Kubernetes Pod 로그 확인
kubectl logs -n phonebill-{환경} deployment/{서비스명}
kubectl describe pod -n phonebill-{환경} -l app={서비스명}
```

---

## ✅ **체크리스트 완벽 준수로 Jenkins CI/CD 파이프라인 구축 완료!**

**이제 실수 없이 안전하게 CI/CD를 구축하고 운영할 수 있습니다! 🚀**