#!/bin/bash

# Manual Deployment Script for phonebill Microservices
# Usage: ./deploy.sh <environment> [service1,service2,...] [options]
# Example: ./deploy.sh dev all --skip-build
# Example: ./deploy.sh prod user-service,bill-service --force

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
CICD_DIR="${PROJECT_ROOT}/deployment/cicd"
K8S_DIR="${PROJECT_ROOT}/deployment/k8s"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Usage information
show_usage() {
    cat << EOF
Usage: $0 <environment> [services] [options]

Arguments:
  environment     Target environment (dev|staging|prod)
  services        Services to deploy (default: all)
                  Options: all, api-gateway, user-service, bill-service, product-service, kos-mock
                  Multiple services: service1,service2,service3

Options:
  --skip-build    Skip Gradle build step
  --skip-test     Skip unit tests during build
  --skip-push     Skip container image push
  --force         Force deployment even if no changes
  --dry-run       Show what would be deployed without actually deploying
  --help          Show this help message

Examples:
  $0 dev                              # Deploy all services to dev
  $0 staging user-service             # Deploy user-service to staging
  $0 prod api-gateway,bill-service    # Deploy specific services to prod
  $0 dev all --skip-build             # Deploy without building
  $0 staging all --dry-run            # Preview deployment

Environment Files:
  dev: ${CICD_DIR}/config/deploy_env_vars_dev
  staging: ${CICD_DIR}/config/deploy_env_vars_staging
  prod: ${CICD_DIR}/config/deploy_env_vars_prod
EOF
}

# Parse command line arguments
parse_arguments() {
    if [[ $# -eq 0 ]] || [[ "$1" == "--help" ]]; then
        show_usage
        exit 0
    fi

    ENVIRONMENT="$1"
    shift

    # Validate environment
    if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
        log_error "Invalid environment: $ENVIRONMENT"
        log_error "Valid environments: dev, staging, prod"
        exit 1
    fi

    # Set services (default to 'all')
    SERVICES_TO_DEPLOY="all"
    if [[ $# -gt 0 ]] && [[ ! "$1" =~ ^-- ]]; then
        SERVICES_TO_DEPLOY="$1"
        shift
    fi

    # Parse options
    SKIP_BUILD=false
    SKIP_TEST=false
    SKIP_PUSH=false
    FORCE_DEPLOY=false
    DRY_RUN=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --skip-test)
                SKIP_TEST=true
                shift
                ;;
            --skip-push)
                SKIP_PUSH=true
                shift
                ;;
            --force)
                FORCE_DEPLOY=true
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

# Load environment configuration
load_environment_config() {
    local config_file="${CICD_DIR}/config/deploy_env_vars_${ENVIRONMENT}"
    
    if [[ ! -f "$config_file" ]]; then
        log_error "Configuration file not found: $config_file"
        exit 1
    fi

    # Source the configuration file
    set -a  # automatically export all variables
    source "$config_file"
    set +a

    log_info "Loaded configuration from $config_file"
}

# Validate prerequisites
validate_prerequisites() {
    local missing_tools=()

    # Check required tools
    command -v gradle >/dev/null 2>&1 || missing_tools+=("gradle")
    command -v docker >/dev/null 2>&1 || missing_tools+=("docker")
    command -v kubectl >/dev/null 2>&1 || missing_tools+=("kubectl")
    command -v az >/dev/null 2>&1 || missing_tools+=("azure-cli")

    if [[ ${#missing_tools[@]} -ne 0 ]]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install the missing tools and try again"
        exit 1
    fi

    # Check if we're in the project root
    if [[ ! -f "${PROJECT_ROOT}/settings.gradle" ]]; then
        log_error "Not in phonebill project root directory"
        exit 1
    fi

    # Check Azure login
    if ! az account show >/dev/null 2>&1; then
        log_error "Not logged into Azure CLI. Please run: az login"
        exit 1
    fi

    # Check kubectl context
    if ! kubectl config current-context >/dev/null 2>&1; then
        log_warn "No kubectl context set. Will attempt to configure AKS credentials"
    fi

    log_success "Prerequisites validation passed"
}

# Resolve services list
resolve_services() {
    if [[ "$SERVICES_TO_DEPLOY" == "all" ]]; then
        SERVICE_LIST=(${SERVICES//,/ })
    else
        IFS=',' read -ra SERVICE_LIST <<< "$SERVICES_TO_DEPLOY"
    fi

    log_info "Services to deploy: ${SERVICE_LIST[*]}"
    
    # Validate service names
    local valid_services=(${SERVICES//,/ })
    for service in "${SERVICE_LIST[@]}"; do
        if [[ ! " ${valid_services[*]} " =~ " ${service} " ]]; then
            log_error "Invalid service name: $service"
            log_error "Valid services: ${valid_services[*]}"
            exit 1
        fi
    done
}

# Build services
build_services() {
    if [[ "$SKIP_BUILD" == true ]]; then
        log_info "Skipping build step"
        return 0
    fi

    log_info "Building services with Gradle..."
    cd "$PROJECT_ROOT"

    for service in "${SERVICE_LIST[@]}"; do
        log_info "Building $service..."
        
        local build_cmd="./gradlew ${service}:clean ${service}:build --no-daemon --parallel"
        
        if [[ "$SKIP_TEST" == true ]]; then
            build_cmd="$build_cmd -x test"
        fi

        if [[ "$DRY_RUN" == true ]]; then
            log_info "[DRY-RUN] Would execute: $build_cmd"
        else
            if ! $build_cmd; then
                log_error "Build failed for $service"
                exit 1
            fi
        fi
    done

    log_success "Build completed successfully"
}

# Build and push container images
build_and_push_images() {
    if [[ "$SKIP_PUSH" == true ]]; then
        log_info "Skipping container image build and push"
        return 0
    fi

    log_info "Building and pushing container images..."
    
    # Generate image tag
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local build_number="${BUILD_NUMBER:-$(date +%s)}"
    IMAGE_TAG="${build_number}-${ENVIRONMENT}-${timestamp}"
    
    log_info "Using image tag: $IMAGE_TAG"

    # Login to ACR
    if [[ "$DRY_RUN" == false ]]; then
        log_info "Logging into Azure Container Registry..."
        az acr login --name "$ACR_NAME"
    fi

    for service in "${SERVICE_LIST[@]}"; do
        log_info "Building container image for $service..."
        
        local image_name="${REGISTRY_URL}/phonebill/${service}"
        local service_dir="${PROJECT_ROOT}/${service}"
        
        if [[ ! -d "$service_dir" ]]; then
            log_error "Service directory not found: $service_dir"
            exit 1
        fi

        if [[ ! -f "${service_dir}/Dockerfile" ]]; then
            log_error "Dockerfile not found: ${service_dir}/Dockerfile"
            exit 1
        fi

        if [[ "$DRY_RUN" == true ]]; then
            log_info "[DRY-RUN] Would build and push: ${image_name}:${IMAGE_TAG}"
        else
            # Build image
            docker build \
                -t "${image_name}:${IMAGE_TAG}" \
                -t "${image_name}:latest-${ENVIRONMENT}" \
                "$service_dir"

            # Push image
            docker push "${image_name}:${IMAGE_TAG}"
            docker push "${image_name}:latest-${ENVIRONMENT}"
        fi
    done

    log_success "Container images built and pushed successfully"
}

# Configure kubectl
configure_kubectl() {
    log_info "Configuring kubectl for AKS cluster..."
    
    if [[ "$DRY_RUN" == false ]]; then
        az aks get-credentials \
            --resource-group "$AZURE_RESOURCE_GROUP" \
            --name "$AKS_CLUSTER_NAME" \
            --overwrite-existing
    fi

    log_success "kubectl configured for $AKS_CLUSTER_NAME"
}

# Deploy to Kubernetes
deploy_to_kubernetes() {
    log_info "Deploying services to Kubernetes namespace: $AKS_NAMESPACE"
    
    # Ensure namespace exists
    if [[ "$DRY_RUN" == false ]]; then
        kubectl create namespace "$AKS_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    fi

    for service in "${SERVICE_LIST[@]}"; do
        log_info "Deploying $service..."
        
        local kustomize_path="${K8S_DIR}/${service}"
        local overlay_path="${kustomize_path}/${KUSTOMIZE_OVERLAY}"
        
        if [[ ! -d "$overlay_path" ]]; then
            log_error "Kustomize overlay not found: $overlay_path"
            exit 1
        fi

        if [[ "$DRY_RUN" == true ]]; then
            log_info "[DRY-RUN] Would deploy $service using: kubectl apply -k $overlay_path -n $AKS_NAMESPACE"
        else
            # Update image tag in kustomization.yaml if IMAGE_TAG is set
            if [[ -n "${IMAGE_TAG:-}" ]]; then
                local kustomization_file="${overlay_path}/kustomization.yaml"
                if [[ -f "$kustomization_file" ]]; then
                    # Backup original file
                    cp "$kustomization_file" "${kustomization_file}.backup"
                    
                    # Update image tag
                    sed -i "s|newTag:.*|newTag: ${IMAGE_TAG}|" "$kustomization_file"
                fi
            fi

            # Deploy using kubectl + kustomize
            kubectl apply -k "$overlay_path" -n "$AKS_NAMESPACE"
            
            # Wait for rollout to complete
            kubectl rollout status deployment/"$service" -n "$AKS_NAMESPACE" --timeout=300s
            
            # Restore backup if exists
            if [[ -f "${kustomization_file}.backup" ]]; then
                mv "${kustomization_file}.backup" "$kustomization_file"
            fi
        fi
    done

    log_success "Deployment completed successfully"
}

# Perform health checks
perform_health_checks() {
    if [[ "$DRY_RUN" == true ]]; then
        log_info "[DRY-RUN] Would perform health checks for deployed services"
        return 0
    fi

    log_info "Performing health checks..."

    for service in "${SERVICE_LIST[@]}"; do
        log_info "Health checking $service..."
        
        local max_retries=${HEALTH_CHECK_RETRY:-10}
        local retry_count=0
        local is_healthy=false

        while [[ $retry_count -lt $max_retries ]]; do
            if kubectl get deployment "$service" -n "$AKS_NAMESPACE" -o json | \
               jq -e '.status.readyReplicas == .status.replicas and .status.replicas > 0' >/dev/null 2>&1; then
                is_healthy=true
                break
            fi

            retry_count=$((retry_count + 1))
            log_info "Health check attempt $retry_count/$max_retries for $service..."
            sleep 30
        done

        if [[ "$is_healthy" == true ]]; then
            log_success "$service is healthy"
        else
            log_error "$service failed health check"
            kubectl describe deployment "$service" -n "$AKS_NAMESPACE"
            exit 1
        fi
    done

    log_success "All services passed health checks"
}

# Show deployment summary
show_summary() {
    log_info "========================================="
    log_info "Deployment Summary"
    log_info "========================================="
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $AKS_NAMESPACE"
    log_info "Services: ${SERVICE_LIST[*]}"
    
    if [[ -n "${IMAGE_TAG:-}" ]]; then
        log_info "Image Tag: $IMAGE_TAG"
    fi
    
    log_info "Options:"
    log_info "  Skip Build: $SKIP_BUILD"
    log_info "  Skip Test: $SKIP_TEST"
    log_info "  Skip Push: $SKIP_PUSH"
    log_info "  Force Deploy: $FORCE_DEPLOY"
    log_info "  Dry Run: $DRY_RUN"
    log_info "========================================="
}

# Cleanup function
cleanup() {
    local exit_code=$?
    if [[ $exit_code -ne 0 ]]; then
        log_error "Deployment failed with exit code: $exit_code"
        
        # Show recent events for debugging
        if [[ "$DRY_RUN" == false ]] && command -v kubectl >/dev/null 2>&1; then
            log_info "Recent events in namespace $AKS_NAMESPACE:"
            kubectl get events -n "$AKS_NAMESPACE" --sort-by='.lastTimestamp' --field-selector type=Warning | tail -10
        fi
    fi
}

# Main execution function
main() {
    # Set up error handling
    trap cleanup EXIT

    log_info "🚀 Starting phonebill deployment script"
    
    # Parse arguments and validate
    parse_arguments "$@"
    show_summary
    
    # Load configuration and validate prerequisites
    load_environment_config
    validate_prerequisites
    resolve_services
    
    # Execute deployment steps
    build_services
    build_and_push_images
    configure_kubectl
    deploy_to_kubernetes
    perform_health_checks
    
    log_success "🎉 Deployment completed successfully!"
    
    if [[ "$DRY_RUN" == false ]]; then
        log_info "You can check the deployment status with:"
        log_info "  kubectl get pods -n $AKS_NAMESPACE"
        log_info "  kubectl get services -n $AKS_NAMESPACE"
        log_info "  kubectl get ingress -n $AKS_NAMESPACE"
    fi
}

# Execute main function if script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi