#!/bin/bash

# Deployment selector 문제 해결을 위한 스크립트
# 기존 Deployment 삭제 후 새로운 설정으로 재생성

echo "=== Deployment Selector 문제 해결 시작 ==="

NAMESPACE="phonebill-dev"
SERVICES=("api-gateway" "bill-service" "kos-mock" "product-service" "user-service")

# 1단계: 기존 Deployment들을 안전하게 삭제
echo "1단계: 기존 Deployment 삭제"
for service in "${SERVICES[@]}"; do
    echo "삭제 중: $service"
    kubectl delete deployment $service -n $NAMESPACE --ignore-not-found=true
    
    # Deployment가 완전히 삭제될 때까지 대기
    while kubectl get deployment $service -n $NAMESPACE &>/dev/null; do
        echo "대기 중: $service 삭제 완료 대기..."
        sleep 2
    done
    echo "완료: $service 삭제됨"
done

echo "모든 Deployment 삭제 완료"

# 2단계: 잠시 대기
echo "2단계: 리소스 정리 대기 (5초)"
sleep 5

# 3단계: Kustomize를 통해 새로운 Deployment 생성
echo "3단계: 새로운 Deployment 생성"
echo "Kustomize 적용 중..."

cd deployment/cicd/kustomize/overlays/dev
kubectl apply -k .

echo "=== Deployment Selector 문제 해결 완료 ==="

# 4단계: 결과 확인
echo "4단계: 배포 결과 확인"
kubectl get deployments -n $NAMESPACE -o wide

echo ""
echo "Pod 상태 확인:"
kubectl get pods -n $NAMESPACE