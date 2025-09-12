#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

# 서비스 목록
SERVICES=("api-gateway" "user-service" "bill-service" "product-service" "kos-mock")

echo "🚀 Starting deployment to ${ENVIRONMENT} environment..."
echo "📦 Image tag: ${ENVIRONMENT}-${IMAGE_TAG}"

# 환경별 이미지 태그 업데이트
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

# 각 서비스 이미지 태그 업데이트
echo "🔄 Updating image tags..."
for service in "${SERVICES[@]}"; do
    echo "  - Updating ${service} to acrdigitalgarage01.azurecr.io/phonebill/${service}:${ENVIRONMENT}-${IMAGE_TAG}"
    kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/${service}:${ENVIRONMENT}-${IMAGE_TAG}
done

# 배포 실행
echo "🎯 Applying Kubernetes manifests..."
kubectl apply -k .

# 배포 상태 확인
echo "⏳ Waiting for deployments to be ready..."
for service in "${SERVICES[@]}"; do
    echo "  - Checking ${service} deployment status..."
    kubectl rollout status deployment/${service} -n phonebill-${ENVIRONMENT} --timeout=300s
done

# 최종 상태 확인
echo "📋 Final deployment status:"
kubectl get pods -n phonebill-${ENVIRONMENT}
echo ""
kubectl get services -n phonebill-${ENVIRONMENT}
echo ""
kubectl get ingress -n phonebill-${ENVIRONMENT}

echo "✅ Deployment to ${ENVIRONMENT} environment completed successfully!"
echo "🌐 Access URL: https://$(kubectl get ingress -n phonebill-${ENVIRONMENT} -o jsonpath='{.items[0].spec.rules[0].host}')"