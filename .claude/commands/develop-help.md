# Develop Work Type Commands

**Command**: `/develop-help`

## 설명
Develop 워크타입의 명령어들과 작업 순서를 표시합니다.

## Develop 작업 순서

Develop 워크타입은 백엔드 서비스 설치부터 프론트엔드 개발까지 실제 개발과 배포를 담당합니다.

### Phase 1: 인프라 설치 계획
1. **데이터베이스 설치계획서 작성**: `/develop-db-guide`
2. **Message Queue 설치 계획서 작성**: `/develop-mq-guide` (필요시)

### Phase 2: 인프라 설치
3. **데이터베이스 설치**: `/develop-db-install`
4. **Message Queue 설치**: `/develop-mq-install` (필요시)

### Phase 3: 백엔드 개발
5. **백엔드 개발**: `/develop-dev-backend`
6. **백엔드 오류 해결**: `/develop-fix-backend`
7. **서비스 실행파일 작성**: `/develop-make-run-profile`
8. **백엔드 테스트**: `/develop-test-backend`

### Phase 4: 프론트엔드 개발
9. **프론트엔드 개발**: `/develop-dev-front`

### Phase 5: 인프라 정리 (필요시)
- **데이터베이스 설치 제거**: `/develop-db-remove`
- **Message Queue 설치 제거**: `/develop-mq-remove`

## 작업 흐름
```
인프라 계획 → 인프라 설치 → 백엔드 개발 → 프론트엔드 개발
```

## 필수 사전 요구사항
- **데이터베이스 설치**: `[설치정보]` 섹션 필요
- **Message Queue 설치**: `[설치정보]` 섹션 필요
- **서비스 실행파일 작성**: `[작성정보]` 섹션 (API Keys 필요)
- **백엔드 테스트**: `[테스트정보]` 섹션 (API Keys 필요)
- **프론트엔드 개발**: `[개발정보]` 섹션 (프레임워크 정보 필요)