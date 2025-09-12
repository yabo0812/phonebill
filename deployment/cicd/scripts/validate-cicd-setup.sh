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
        kubectl kustomize deployment/cicd/kustomize/overlays/$env 2>&1 | head -3
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

# 6. patches 문법 확인 (patchesStrategicMerge 금지)
echo "6. Kustomization patches 문법 검증..."
if grep -r "patchesStrategicMerge:" deployment/cicd/kustomize/overlays/*/kustomization.yaml > /dev/null; then
    echo "❌ 금지된 patchesStrategicMerge 사용 발견"
else
    echo "✅ patches 문법 정상"
fi

# 7. 환경별 replicas 설정 확인
echo "7. 환경별 replicas 설정 검증..."
DEV_REPLICAS=$(grep "replicas:" deployment/cicd/kustomize/overlays/dev/deployment-user-service-patch.yaml | awk '{print $2}')
STAGING_REPLICAS=$(grep "replicas:" deployment/cicd/kustomize/overlays/staging/deployment-user-service-patch.yaml | awk '{print $2}')
PROD_REPLICAS=$(grep "replicas:" deployment/cicd/kustomize/overlays/prod/deployment-user-service-patch.yaml | awk '{print $2}')

if [ "$DEV_REPLICAS" = "1" ] && [ "$STAGING_REPLICAS" = "2" ] && [ "$PROD_REPLICAS" = "3" ]; then
    echo "✅ 환경별 replicas 설정 정상 (dev:1, staging:2, prod:3)"
else
    echo "❌ 환경별 replicas 설정 확인 필요 (dev:$DEV_REPLICAS, staging:$STAGING_REPLICAS, prod:$PROD_REPLICAS)"
fi

# 8. 서비스 배열 검증
echo "8. Jenkinsfile 서비스 배열 검증..."
SERVICES_COUNT=$(grep "def services = \[" deployment/cicd/Jenkinsfile | grep -o "'" | wc -l)
if [ $SERVICES_COUNT -eq 10 ]; then  # 5개 서비스 * 2 (시작/끝 따옴표)
    echo "✅ 서비스 배열 정상 (5개 서비스)"
else
    echo "❌ 서비스 배열 확인 필요"
fi

echo ""
echo "🎯 검증 완료!"
echo ""
echo "📋 추가 수동 확인사항:"
echo "   - Jenkins Credentials 설정 (azure-credentials, acr-credentials, sonarqube-token)"
echo "   - SonarQube Quality Gate 설정"  
echo "   - 프로덕션 환경 패스워드 변경"
echo "   - SSL 인증서 설정"