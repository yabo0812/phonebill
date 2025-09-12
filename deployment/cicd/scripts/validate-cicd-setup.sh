#!/bin/bash
# phonebill CI/CD 설정 검증 스크립트

echo "🔍 phonebill CI/CD 설정 검증 시작..."

BASE_DIR="deployment/cicd/kustomize/base"
MISSING_RESOURCES=0
REQUIRED_FILES=("deployment.yaml" "service.yaml")
OPTIONAL_FILES=("cm-" "secret-")

# 1. 각 서비스 디렉토리의 파일 확인
echo "1. 서비스 디렉토리별 파일 목록:"
for dir in $BASE_DIR/*/; do
    if [ -d "$dir" ] && [[ $(basename "$dir") != "common" ]]; then
        service=$(basename "$dir")
        echo "=== $service ==="
        
        # 필수 파일 확인
        for required in "${REQUIRED_FILES[@]}"; do
            if [ -f "$dir$required" ]; then
                echo "  ✅ $required"
            else
                echo "  ❌ MISSING REQUIRED: $required"
                ((MISSING_RESOURCES++))
            fi
        done
        
        # 선택적 파일 확인
        for optional in "${OPTIONAL_FILES[@]}"; do
            files=($(ls "$dir"$optional*".yaml" 2>/dev/null))
            if [ ${#files[@]} -gt 0 ]; then
                for file in "${files[@]}"; do
                    echo "  ✅ $(basename "$file")"
                done
            fi
        done
        echo ""
    fi
done

# 2. Common 리소스 확인
echo "2. Common 리소스 확인:"
COMMON_DIR="$BASE_DIR/common"
if [ -d "$COMMON_DIR" ]; then
    common_files=($(ls "$COMMON_DIR"/*.yaml 2>/dev/null))
    if [ ${#common_files[@]} -gt 0 ]; then
        for file in "${common_files[@]}"; do
            echo "  ✅ common/$(basename "$file")"
        done
    else
        echo "  ❌ Common 디렉토리에 YAML 파일이 없습니다"
        ((MISSING_RESOURCES++))
    fi
else
    echo "  ❌ Common 디렉토리가 없습니다"
    ((MISSING_RESOURCES++))
fi

# 3. kustomization.yaml과 실제 파일 비교
echo ""
echo "3. kustomization.yaml 리소스 검증:"
if [ -f "$BASE_DIR/kustomization.yaml" ]; then
    while IFS= read -r line; do
        # resources 섹션의 YAML 파일 경로 추출
        if [[ $line =~ ^[[:space:]]*-[[:space:]]*([^#]+\.yaml)[[:space:]]*$ ]]; then
            resource_path=$(echo "${BASH_REMATCH[1]}" | xargs)  # 공백 제거
            full_path="$BASE_DIR/$resource_path"
            if [ -f "$full_path" ]; then
                echo "  ✅ $resource_path"
            else
                echo "  ❌ MISSING: $resource_path"
                ((MISSING_RESOURCES++))
            fi
        fi
    done < "$BASE_DIR/kustomization.yaml"
else
    echo "  ❌ kustomization.yaml 파일이 없습니다"
    ((MISSING_RESOURCES++))
fi

# 4. kubectl kustomize 검증
echo ""
echo "4. Kustomize 빌드 테스트:"
if kubectl kustomize "$BASE_DIR" > /dev/null 2>&1; then
    echo "  ✅ Base kustomization 빌드 성공"
else
    echo "  ❌ Base kustomization 빌드 실패:"
    kubectl kustomize "$BASE_DIR" 2>&1 | head -5 | sed 's/^/     /'
    ((MISSING_RESOURCES++))
fi

# 5. 환경별 overlay 검증
echo ""
echo "5. 환경별 Overlay 검증:"
for env in dev staging prod; do
    overlay_dir="deployment/cicd/kustomize/overlays/$env"
    if [ -d "$overlay_dir" ] && [ -f "$overlay_dir/kustomization.yaml" ]; then
        if kubectl kustomize "$overlay_dir" > /dev/null 2>&1; then
            echo "  ✅ $env 환경 빌드 성공"
        else
            echo "  ❌ $env 환경 빌드 실패"
            ((MISSING_RESOURCES++))
        fi
    else
        echo "  ⚠️  $env 환경 설정 없음 (선택사항)"
    fi
done

# 6. Jenkins 파이프라인 검증
echo ""
echo "6. Jenkins 파이프라인 검증:"
if [ -f "deployment/cicd/Jenkinsfile" ]; then
    echo "  ✅ Jenkinsfile 존재"
    
    # 주요 서비스명 확인
    services_in_jenkinsfile=$(grep -o "api-gateway\|user-service\|bill-service\|product-service\|kos-mock" deployment/cicd/Jenkinsfile | sort -u | tr '\n' ' ')
    echo "  📋 Jenkinsfile의 서비스: $services_in_jenkinsfile"
    
    # ACR 이름 확인
    acr_name=$(grep -o "acrdigitalgarage01" deployment/cicd/Jenkinsfile | head -1)
    if [ -n "$acr_name" ]; then
        echo "  ✅ ACR 이름: $acr_name"
    else
        echo "  ⚠️  ACR 이름을 찾을 수 없습니다"
    fi
else
    echo "  ❌ Jenkinsfile이 없습니다"
    ((MISSING_RESOURCES++))
fi

# 7. 스크립트 파일 검증
echo ""
echo "7. 배포 스크립트 검증:"
scripts_dir="deployment/cicd/scripts"
if [ -f "$scripts_dir/deploy.sh" ]; then
    echo "  ✅ deploy.sh 존재"
    if [ -x "$scripts_dir/deploy.sh" ]; then
        echo "  ✅ deploy.sh 실행 권한 있음"
    else
        echo "  ⚠️  deploy.sh 실행 권한 없음 (chmod +x 필요)"
    fi
else
    echo "  ❌ deploy.sh가 없습니다"
    ((MISSING_RESOURCES++))
fi

# 8. 환경별 설정 파일 검증
echo ""
echo "8. 환경별 설정 파일 검증:"
config_dir="deployment/cicd/config"
for env in dev staging prod; do
    config_file="$config_dir/deploy_env_vars_$env"
    if [ -f "$config_file" ]; then
        echo "  ✅ $env 환경 설정 파일 존재"
        # 필수 설정 확인
        if grep -q "resource_group\|cluster_name" "$config_file"; then
            echo "    ✅ 필수 설정 (resource_group, cluster_name) 확인됨"
        else
            echo "    ❌ 필수 설정이 누락됨"
            ((MISSING_RESOURCES++))
        fi
    else
        echo "  ❌ $env 환경 설정 파일이 없습니다"
        ((MISSING_RESOURCES++))
    fi
done

# 결과 출력
echo ""
echo "======================================"
if [ $MISSING_RESOURCES -eq 0 ]; then
    echo "🎯 검증 완료! phonebill CI/CD 설정이 정상입니다."
    echo "======================================"
    echo ""
    echo "📋 Jenkins CI/CD 파이프라인 구성 요약:"
    echo "  • 시스템명: phonebill"
    echo "  • 서비스: api-gateway, user-service, bill-service, product-service, kos-mock"
    echo "  • 환경: dev, staging, prod"
    echo "  • 컨테이너 레지스트리: acrdigitalgarage01.azurecr.io"
    echo "  • JDK 버전: 21"
    echo ""
    echo "🚀 다음 단계:"
    echo "  1. Jenkins에서 Pipeline Job 생성"
    echo "  2. Jenkins Credentials 설정 (azure-credentials, acr-credentials, dockerhub-credentials)"
    echo "  3. SonarQube 서버 연동 설정"
    echo "  4. 파이프라인 실행 테스트"
    echo ""
    exit 0
else
    echo "❌ $MISSING_RESOURCES개의 문제가 발견되었습니다."
    echo "======================================"
    echo ""
    echo "💡 문제 해결 가이드:"
    echo "1. 누락된 파일들을 base 디렉토리에 추가하세요"
    echo "2. kustomization.yaml에서 존재하지 않는 파일 참조를 제거하세요"
    echo "3. 파일명이 명명 규칙을 따르는지 확인하세요:"
    echo "   - ConfigMap: cm-{서비스명}.yaml"
    echo "   - Secret: secret-{서비스명}.yaml"
    echo "4. 스크립트 실행 권한 설정: chmod +x scripts/*.sh"
    echo "5. 다시 검증: ./scripts/validate-cicd-setup.sh"
    exit 1
fi