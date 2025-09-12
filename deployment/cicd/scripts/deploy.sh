#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Starting deployment for environment: $ENVIRONMENT with image tag: $IMAGE_TAG"

# 환경별 이미지 태그 업데이트
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

# 서비스 목록 (공백으로 구분)
services="api-gateway user-service bill-service product-service kos-mock"

# 각 서비스 이미지 태그 업데이트
for service in $services; do
    echo "📦 Updating image tag for $service to ${ENVIRONMENT}-${IMAGE_TAG}"
    kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/$service:${ENVIRONMENT}-${IMAGE_TAG}
done

# 배포 실행
echo "🔧 Applying manifests to Kubernetes cluster..."
kubectl apply -k .

# 배포 상태 확인
echo "⏳ Waiting for deployments to be ready..."
for service in $services; do
    echo "   Checking $service..."
    kubectl rollout status deployment/$service -n phonebill-${ENVIRONMENT} --timeout=300s
done

echo "✅ Deployment completed successfully!"
echo "🌐 Application endpoints:"
kubectl get ingress -n phonebill-${ENVIRONMENT} -o wide