#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 GitHub Actions Manual deployment starting..."
echo "Environment: $ENVIRONMENT"
echo "Image Tag: $IMAGE_TAG"

# Check if kustomize is installed
if ! command -v kustomize &> /dev/null; then
    echo "Installing Kustomize..."
    curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
    sudo mv kustomize /usr/local/bin/
fi

# Set variables
REGISTRY="acrdigitalgarage03.azurecr.io"
IMAGE_ORG="phonebill"
NAMESPACE="phonebill-dg0511"

# Create namespace
echo "📝 Creating namespace $NAMESPACE..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# 환경별 이미지 태그 업데이트 (deployment/cicd/kustomize 사용)
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

echo "🔄 Updating image tags..."
# 서비스 배열 정의
services=(api-gateway user-service bill-service product-service kos-mock)

# 각 서비스별 이미지 태그 업데이트
for service in "${services[@]}"; do
  kustomize edit set image $REGISTRY/$IMAGE_ORG/$service:${ENVIRONMENT}-${IMAGE_TAG}
done

echo "🚀 Deploying to Kubernetes..."
# 배포 실행
kubectl apply -k .

echo "⏳ Waiting for deployments to be ready..."
# 서비스별 배포 상태 확인
for service in "${services[@]}"; do
  kubectl rollout status deployment/$service -n $NAMESPACE --timeout=300s
done

echo "🔍 Health check..."
# API Gateway Health Check
GATEWAY_POD=$(kubectl get pod -n $NAMESPACE -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [ -n "$GATEWAY_POD" ]; then
  kubectl -n $NAMESPACE exec $GATEWAY_POD -- curl -f http://localhost:8080/actuator/health || echo "Health check failed, but deployment completed"
else
  echo "API Gateway pod not found, skipping health check"
fi

echo "📋 Service Information:"
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE
kubectl get ingress -n $NAMESPACE

echo "✅ GitHub Actions deployment completed successfully!"
