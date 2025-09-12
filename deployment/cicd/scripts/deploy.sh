#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Starting deployment for environment: $ENVIRONMENT with image tag: $IMAGE_TAG"

# 환경별 이미지 태그 업데이트
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

echo "📝 Updating image tags..."
# 각 서비스 이미지 태그 업데이트
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/api-gateway:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/user-service:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/bill-service:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/product-service:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/kos-mock:${ENVIRONMENT}-${IMAGE_TAG}

echo "📦 Applying manifests to Kubernetes..."
# 배포 실행
kubectl apply -k .

echo "⏳ Waiting for deployments to be ready..."
# 배포 상태 확인
kubectl rollout status deployment/${ENVIRONMENT}-api-gateway -n phonebill-${ENVIRONMENT}
kubectl rollout status deployment/${ENVIRONMENT}-user-service -n phonebill-${ENVIRONMENT}
kubectl rollout status deployment/${ENVIRONMENT}-bill-service -n phonebill-${ENVIRONMENT}
kubectl rollout status deployment/${ENVIRONMENT}-product-service -n phonebill-${ENVIRONMENT}
kubectl rollout status deployment/${ENVIRONMENT}-kos-mock -n phonebill-${ENVIRONMENT}

echo "🔍 Checking deployment status..."
kubectl get pods -n phonebill-${ENVIRONMENT}
kubectl get services -n phonebill-${ENVIRONMENT}
kubectl get ingress -n phonebill-${ENVIRONMENT}

echo "✅ Deployment completed successfully!"