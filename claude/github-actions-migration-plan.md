# GitHub Actions CI/CD 전환 작업계획서

## 📋 개요

현재 Jenkins 기반 CI/CD 파이프라인을 GitHub Actions로 전환하여 더 효율적이고 관리하기 쉬운 DevOps 환경을 구축하는 작업계획서입니다.

## 🎯 전환 목표

- **통합 개발 환경**: GitHub과 CI/CD의 완전한 통합
- **관리 효율성**: Jenkins 인프라 관리 부담 제거
- **보안 강화**: GitHub Secrets를 통한 안전한 자격증명 관리
- **개발자 경험**: GitHub 인터페이스와 통합된 워크플로우
- **비용 최적화**: 자체 관리형 Jenkins 인프라 비용 절감

## 🔍 현재 Jenkins CI/CD 분석

### 기존 Jenkins 파이프라인 구조

```
Jenkins Pipeline
├── Pod Template (Kubernetes 기반)
│   ├── podman container (이미지 빌드)
│   ├── gradle container (빌드 & 테스트)
│   └── azure-cli container (배포)
├── 5개 서비스 병렬 처리
│   ├── api-gateway
│   ├── user-service
│   ├── bill-service
│   ├── product-service
│   └── kos-mock
└── Kustomize 기반 쿠버네티스 배포
```

### 현재 파이프라인 단계
1. **Get Source**: SCM checkout
2. **Setup AKS**: Azure 인증 및 쿠버네티스 클러스터 연결
3. **Build**: Gradle 빌드 (테스트 제외)
4. **SonarQube Analysis**: 코드 품질 분석 (선택적)
5. **Build & Push Images**: Podman으로 컨테이너 이미지 빌드/푸시
6. **Update Kustomize & Deploy**: 쿠버네티스 배포

### 사용 중인 도구 및 서비스
- **빌드 도구**: Gradle with JDK 21
- **컨테이너**: Podman (Docker 대신 사용)
- **레지스트리**: Azure Container Registry (ACR)
- **배포**: Kustomize + kubectl
- **코드 품질**: SonarQube
- **인프라**: Azure Kubernetes Service (AKS)

## 📊 전환 범위 및 우선순위

### 1단계: 핵심 워크플로우 구축 (필수)
- [ ] **기본 빌드 워크플로우**: Gradle 빌드 자동화
- [ ] **컨테이너 이미지 빌드**: Docker 기반 이미지 빌드/푸시
- [ ] **환경별 배포**: dev/staging/prod 환경 지원
- [ ] **시크릿 관리**: 자격증명 및 환경변수 관리

### 2단계: 고도화 기능 (중요)
- [ ] **코드 품질 분석**: SonarQube 통합
- [ ] **테스트 자동화**: 단위/통합 테스트 실행
- [ ] **병렬 처리**: 5개 서비스 동시 빌드/배포
- [ ] **배포 검증**: Health check 및 rollback 기능

### 3단계: 최적화 및 확장 (선택)
- [ ] **캐싱 전략**: 빌드 속도 최적화
- [ ] **매트릭스 빌드**: 다중 환경/버전 지원
- [ ] **알림 시스템**: 빌드 상태 알림
- [ ] **보안 스캔**: 컨테이너 취약점 검사

## 🗂️ 파일 구조 계획

```
.github/
└── workflows/
    ├── ci-cd.yml                    # 메인 CI/CD 워크플로우
    ├── build-and-test.yml          # 빌드 및 테스트만 실행
    ├── deploy-dev.yml              # 개발환경 배포
    ├── deploy-staging.yml          # 스테이징환경 배포
    └── deploy-prod.yml             # 운영환경 배포

scripts/
├── build-images.sh                 # 이미지 빌드 스크립트
├── deploy-services.sh              # 서비스 배포 스크립트
└── health-check.sh                 # 배포 검증 스크립트
```

## 🔧 기술 스택 매핑

### Jenkins → GitHub Actions 매핑

| Jenkins 요소 | GitHub Actions 대체 | 비고 |
|---------------|-------------------|------|
| Jenkinsfile | .github/workflows/*.yml | YAML 기반 워크플로우 |
| Pod Template | Ubuntu/Windows runners | GitHub hosted runners |
| Podman container | Docker/build-push-action | Docker 기반 이미지 빌드 |
| Gradle container | actions/setup-java | Java 21 + Gradle wrapper |
| Azure CLI container | azure/login | Azure 서비스 연결 |
| Credentials | GitHub Secrets | 암호화된 환경변수 |
| SonarQube | sonarqube-github-action | 코드 품질 분석 |

### 환경변수 및 시크릿

**GitHub Secrets 필요 항목:**
```
AZURE_CLIENT_ID
AZURE_CLIENT_SECRET
AZURE_TENANT_ID
ACR_USERNAME
ACR_PASSWORD
DOCKERHUB_USERNAME
DOCKERHUB_PASSWORD
SONARQUBE_TOKEN
SONARQUBE_HOST_URL
RESOURCE_GROUP_DEV
RESOURCE_GROUP_STAGING  
RESOURCE_GROUP_PROD
CLUSTER_NAME_DEV
CLUSTER_NAME_STAGING
CLUSTER_NAME_PROD
```

## 📝 상세 작업 단계

### Phase 1: 환경 준비 (1-2일)

#### 1.1 GitHub Repository 설정
- [ ] GitHub Actions 활성화 확인
- [ ] Branch protection rules 설정
- [ ] Required status checks 구성

#### 1.2 시크릿 구성
- [ ] Azure 서비스 주체 정보 등록
- [ ] ACR 자격증명 등록
- [ ] SonarQube 토큰 등록
- [ ] 환경별 클러스터 정보 등록

#### 1.3 권한 설정
- [ ] GitHub Actions service account 생성
- [ ] AKS 클러스터 접근 권한 부여
- [ ] ACR 이미지 푸시 권한 확인

### Phase 2: 기본 워크플로우 구축 (3-4일)

#### 2.1 빌드 워크플로우 (.github/workflows/build.yml)
```yaml
# 예시 구조
name: Build and Test
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [api-gateway, user-service, bill-service, product-service, kos-mock]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build with Gradle
        run: ./gradlew :${{ matrix.service }}:build -x test
```

#### 2.2 이미지 빌드 워크플로우
- [ ] Docker 기반 이미지 빌드
- [ ] Multi-stage build 최적화
- [ ] ACR 푸시 자동화
- [ ] 이미지 태그 전략 수립

#### 2.3 배포 워크플로우
- [ ] Kustomize 이미지 태그 업데이트
- [ ] kubectl 배포 실행
- [ ] 배포 상태 확인
- [ ] Rollback 메커니즘

### Phase 3: 고도화 기능 (2-3일)

#### 3.1 테스트 통합
- [ ] 단위 테스트 실행
- [ ] 통합 테스트 실행
- [ ] 테스트 결과 리포팅
- [ ] 커버리지 측정

#### 3.2 코드 품질 분석
- [ ] SonarQube 분석 통합
- [ ] Quality Gate 적용
- [ ] PR 댓글로 품질 리포트
- [ ] 실패 시 배포 중단

#### 3.3 병렬 처리 최적화
- [ ] Matrix strategy 활용
- [ ] Job dependencies 설정
- [ ] 빌드 시간 최적화
- [ ] 캐싱 전략 적용

### Phase 4: 검증 및 안정화 (2-3일)

#### 4.1 기능 검증
- [ ] 전체 파이프라인 End-to-End 테스트
- [ ] 각 환경별 배포 검증
- [ ] 롤백 기능 테스트
- [ ] 성능 벤치마크 비교

#### 4.2 문서화
- [ ] 워크플로우 사용법 문서
- [ ] 트러블슈팅 가이드
- [ ] 마이그레이션 체크리스트
- [ ] 운영 매뉴얼 작성

## ⚠️ 위험 요소 및 대응 방안

### 기술적 위험
| 위험 요소 | 영향도 | 대응 방안 |
|-----------|--------|----------|
| Podman → Docker 변환 | 중 | Docker 호환성 테스트, 이미지 빌드 검증 |
| Jenkins 종속성 | 높음 | 단계적 전환, 병렬 운영 기간 확보 |
| 환경별 설정 차이 | 중 | 환경별 상세 테스트, 설정 검증 도구 |
| 성능 차이 | 낮음 | 빌드 시간 벤치마크, 캐싱 최적화 |

### 운영적 위험
| 위험 요소 | 영향도 | 대응 방안 |
|-----------|--------|----------|
| 배포 중단 | 높음 | Blue-Green 배포, 즉시 롤백 가능 |
| 학습 곡선 | 중 | 교육 계획, 문서화 강화 |
| 권한 관리 복잡성 | 중 | IAM 정책 표준화, 최소 권한 원칙 |

## 📊 성공 지표

### 기술적 지표
- [ ] **빌드 시간**: 현재 대비 20% 이내 유지
- [ ] **배포 성공률**: 99% 이상
- [ ] **MTTR**: 평균 복구 시간 10분 이내
- [ ] **파이프라인 가용성**: 99.9% 이상

### 운영적 지표
- [ ] **관리 복잡성**: Jenkins 인프라 관리 불필요
- [ ] **개발자 만족도**: GitHub 통합 워크플로우
- [ ] **보안 개선**: 중앙화된 시크릿 관리
- [ ] **비용 절감**: Jenkins 인프라 비용 제거

## 🗓️ 일정 계획

### 전체 일정: 8-12 일
```
Week 1 (Day 1-4): 환경 준비 + 기본 워크플로우
├── Day 1-2: GitHub 설정, 시크릿 구성
└── Day 3-4: 빌드 워크플로우 구축

Week 2 (Day 5-8): 고도화 + 검증
├── Day 5-6: 이미지 빌드, 배포 워크플로우
├── Day 7-8: 테스트, 품질 분석 통합

Week 3 (Day 9-12): 최적화 + 안정화
├── Day 9-10: 병렬 처리, 성능 최적화
└── Day 11-12: 검증, 문서화, 운영 전환
```

### 마일스톤
- **M1** (Day 4): 기본 빌드 파이프라인 완료
- **M2** (Day 8): 전체 CI/CD 파이프라인 완료  
- **M3** (Day 12): 운영 환경 전환 완료

## ✅ 체크리스트

### 전환 전 준비사항
- [ ] 현재 Jenkins 파이프라인 백업
- [ ] GitHub Actions 사용량 한도 확인
- [ ] 팀 구성원 GitHub Actions 교육
- [ ] 롤백 계획 수립

### 전환 후 검증사항
- [ ] 전체 서비스 빌드/배포 테스트
- [ ] 환경별 배포 검증
- [ ] 성능 벤치마크 비교
- [ ] 보안 설정 점검
- [ ] 문서화 완료 확인

## 📞 담당자 및 역할

### 핵심 담당자
- **DevOps 리드**: 최운영/데옵스 - 워크플로우 설계, 인프라 연동
- **Backend 리드**: 이개발/백엔더 - 빌드 스크립트, 테스트 통합
- **QA 리드**: 정테스트/QA매니저 - 배포 검증, 품질 게이트

### 지원 역할
- **Product Owner**: 김기획/기획자 - 일정 조율, 우선순위 결정
- **Frontend Dev**: 박화면/프론트 - 프론트엔드 빌드 프로세스

## 💡 추천사항

1. **점진적 전환**: 개발환경부터 단계적으로 전환
2. **병렬 운영**: 초기 안정화까지 Jenkins와 병행 운영
3. **모니터링 강화**: 전환 후 집중 모니터링 기간 운영
4. **피드백 수집**: 개발팀 피드백 기반 지속 개선
5. **문서화 우선**: 모든 변경사항 실시간 문서화

---

**작성일**: 2025-01-14  
**작성자**: 최운영/데옵스  
**검토자**: 김기획/기획자, 이개발/백엔더  
**승인자**: Project Owner