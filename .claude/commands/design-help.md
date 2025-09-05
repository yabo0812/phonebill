# Design Work Type Commands

**Command**: `/design-help`

## 설명
Design 워크타입의 명령어들과 작업 순서를 표시합니다.

## Design 작업 순서

Design 워크타입은 UI/UX 설계부터 물리 아키텍처까지 전체 시스템 설계를 담당합니다.

### Phase 1: UI/UX 설계 및 프로토타입
1. **UI/UX 설계**: `/design-uiux`
2. **프로토타입 작성**: `/design-prototype`
3. **프로토타입 검증**: `/design-test-prototype`
4. **프로토타입 오류수정**: `/design-fix-prototype` (필요시)
5. **프로토타입 개선**: `/design-improve-prototype` (필요시)
6. **유저스토리 품질 높이기**: `/design-improve-userstory`
7. **설계서 업데이트**: `/design-update-uiux`

### Phase 2: 아키텍처 설계
8. **클라우드 아키텍처 패턴 선정**: `/design-pattern`
9. **논리아키텍처 설계**: `/design-logical`
10. **외부 시퀀스 설계**: `/design-seq-outer`
11. **내부 시퀀스 설계**: `/design-seq-inner`
12. **API 설계**: `/design-api`
13. **클래스 설계**: `/design-class`
14. **데이터 설계**: `/design-data`

### Phase 3: 물리 설계
15. **High Level 아키텍처 정의서**: `/design-high-level`
16. **물리 아키텍처 설계**: `/design-physical`
17. **프론트엔드 설계**: `/design-front`

## 작업 흐름
```
UI/UX → 프로토타입 → 아키텍처 → 물리설계
```

## 필수 사전 요구사항
- **클래스 설계**: `[클래스설계 정보]` 섹션 필요
- **High Level 아키텍처**: `CLOUD` 정보 필요
- **물리 아키텍처**: `CLOUD` 정보 필요
- **프론트엔드 설계**: `[백엔드시스템]` 정보 필요