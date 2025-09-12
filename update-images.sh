#!/bin/bash
set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-$(date +%Y%m%d%H%M%S)}

echo "🚀 Updating images for environment: $ENVIRONMENT with tag: $IMAGE_TAG"

# 환경별 디렉토리로 이동
OVERLAY_DIR="deployment/cicd/kustomize/overlays/${ENVIRONMENT}"
cd $OVERLAY_DIR

# 서비스 목록
services="api-gateway user-service bill-service product-service kos-mock"

echo "📦 Updating image tags in kustomization.yaml for services: $services"

# images 섹션이 있는지 확인하고 없으면 추가
if ! grep -q "^images:" kustomization.yaml; then
    echo "" >> kustomization.yaml
    echo "images:" >> kustomization.yaml
fi

# 기존 images 섹션 제거 (단순화를 위해)
sed -i.bak '/^images:/,$d' kustomization.yaml

# 새로운 images 섹션 추가
echo "images:" >> kustomization.yaml

# 각 서비스 이미지 태그 업데이트
for service in $services; do
    echo "  ⏳ Adding $service with tag ${ENVIRONMENT}-${IMAGE_TAG}"
    echo "  - name: acrdigitalgarage01.azurecr.io/phonebill/$service" >> kustomization.yaml
    echo "    newTag: ${ENVIRONMENT}-${IMAGE_TAG}" >> kustomization.yaml
done

echo "✅ Image tags updated successfully!"
echo ""
echo "📋 Updated kustomization.yaml:"
echo "Environment: $ENVIRONMENT"
echo "Image Tag: ${ENVIRONMENT}-${IMAGE_TAG}"
echo "Services: $services"
echo ""
echo "🚢 To deploy, run:"
echo "  kubectl apply -k $OVERLAY_DIR"
echo ""
echo "📄 Current images section:"
tail -15 kustomization.yaml