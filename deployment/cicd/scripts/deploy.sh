#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Starting manual deployment for environment: $ENVIRONMENT with tag: $IMAGE_TAG"

# 환경별 이미지 태그 업데이트
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

# 서비스 목록 (공백으로 구분)
services="api-gateway user-service bill-service product-service kos-mock"

echo "📦 Updating image tags for services: $services"

# 각 서비스 이미지 태그 업데이트
for service in $services; do
    echo "  ⏳ Updating $service to ${ENVIRONMENT}-${IMAGE_TAG}"
    kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/$service:${ENVIRONMENT}-${IMAGE_TAG}
done

echo "🚢 Deploying to Kubernetes cluster..."

# 배포 실행
kubectl apply -k .

echo "⏰ Waiting for deployments to be ready..."

# 배포 상태 확인
for service in $services; do
    echo "  🔄 Waiting for $service deployment..."
    kubectl rollout status deployment/$service -n phonebill-${ENVIRONMENT}
done

echo "✅ Deployment completed successfully!"
echo ""
echo "📋 Deployment Summary:"
echo "  Environment: $ENVIRONMENT"
echo "  Image Tag: ${ENVIRONMENT}-${IMAGE_TAG}"
echo "  Services: $services"
echo "  Namespace: phonebill-${ENVIRONMENT}"
echo ""
echo "🔍 Check deployment status:"
echo "  kubectl get pods -n phonebill-${ENVIRONMENT}"
echo "  kubectl get services -n phonebill-${ENVIRONMENT}"
echo "  kubectl get ingress -n phonebill-${ENVIRONMENT}"