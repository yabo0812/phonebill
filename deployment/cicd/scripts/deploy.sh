#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}

echo "🚀 Starting deployment to ${ENVIRONMENT} environment with image tag: ${IMAGE_TAG}"

# 환경 검증
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    echo "❌ Error: Invalid environment. Use dev, staging, or prod"
    exit 1
fi

# Kustomize 설치 확인
if ! command -v kustomize &> /dev/null; then
    echo "📦 Installing Kustomize..."
    curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
    sudo mv kustomize /usr/local/bin/
fi

# kubectl 연결 확인
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Error: Unable to connect to Kubernetes cluster"
    echo "Please ensure kubectl is configured properly"
    exit 1
fi

# 네임스페이스 생성
echo "🔧 Creating namespace phonebill-${ENVIRONMENT} if not exists..."
kubectl create namespace phonebill-${ENVIRONMENT} --dry-run=client -o yaml | kubectl apply -f -

# 환경별 디렉토리로 이동
cd deployment/cicd/kustomize/overlays/${ENVIRONMENT}

echo "🏷️ Updating image tags..."

# 각 서비스 이미지 태그 업데이트
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/api-gateway:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/user-service:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/bill-service:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/product-service:${ENVIRONMENT}-${IMAGE_TAG}
kustomize edit set image acrdigitalgarage01.azurecr.io/phonebill/kos-mock:${ENVIRONMENT}-${IMAGE_TAG}

echo "📋 Applying Kubernetes manifests..."

# 배포 실행
kubectl apply -k .

echo "⏳ Waiting for deployments to be ready..."

# 배포 상태 확인
kubectl rollout status deployment/${ENVIRONMENT}-api-gateway -n phonebill-${ENVIRONMENT} --timeout=300s
kubectl rollout status deployment/${ENVIRONMENT}-user-service -n phonebill-${ENVIRONMENT} --timeout=300s
kubectl rollout status deployment/${ENVIRONMENT}-bill-service -n phonebill-${ENVIRONMENT} --timeout=300s
kubectl rollout status deployment/${ENVIRONMENT}-product-service -n phonebill-${ENVIRONMENT} --timeout=300s
kubectl rollout status deployment/${ENVIRONMENT}-kos-mock -n phonebill-${ENVIRONMENT} --timeout=300s

echo "🔍 Health Check..."

# API Gateway Health Check
GATEWAY_POD=$(kubectl get pod -n phonebill-${ENVIRONMENT} -l app=api-gateway -o jsonpath='{.items[0].metadata.name}')
if kubectl -n phonebill-${ENVIRONMENT} exec $GATEWAY_POD -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ API Gateway is healthy!"
else
    echo "⚠️  API Gateway health check failed, but deployment completed"
fi

echo ""
echo "✅ Deployment completed successfully!"
echo ""
echo "📊 Deployment Status:"
kubectl get pods -n phonebill-${ENVIRONMENT} -l app=phonebill
echo ""
echo "🌐 Services:"
kubectl get services -n phonebill-${ENVIRONMENT}
echo ""
echo "🔗 Ingress:"
kubectl get ingress -n phonebill-${ENVIRONMENT}
echo ""
echo "🎯 Environment: ${ENVIRONMENT}"
echo "🏷️ Image Tag: ${ENVIRONMENT}-${IMAGE_TAG}"