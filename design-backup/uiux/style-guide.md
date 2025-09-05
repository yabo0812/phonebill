# 통신요금 관리 서비스 - 스타일 가이드

- [통신요금 관리 서비스 - 스타일 가이드](#통신요금-관리-서비스---스타일-가이드)
  - [브랜드 아이덴티티](#브랜드-아이덴티티)
  - [디자인 원칙](#디자인-원칙)
  - [컬러 시스템](#컬러-시스템)
  - [타이포그래피](#타이포그래피)
  - [간격 시스템](#간격-시스템)
  - [컴포넌트 스타일](#컴포넌트-스타일)
  - [반응형 브레이크포인트](#반응형-브레이크포인트)
  - [대상 서비스 특화 컴포넌트](#대상-서비스-특화-컴포넌트)
  - [인터랙션 패턴](#인터랙션-패턴)
  - [변경 이력](#변경-이력)

---

## 브랜드 아이덴티티

### 서비스 컨셉
- **키워드**: 신뢰성, 편리함, 명확성
- **브랜드 메시지**: "간편하고 안전한 통신요금 관리"
- **타겟**: 일반 MVNO 고객 (20대~60대)

### 디자인 컨셉
- **미니멀리즘**: 불필요한 요소 제거, 핵심 기능 집중
- **명확성 우선**: 정보 전달의 명확성과 가독성
- **안정감**: 금융 서비스의 신뢰성과 보안성 강조
- **접근성**: 모든 사용자가 편리하게 이용할 수 있는 인터페이스

---

## 디자인 원칙

### 1. 명확성 (Clarity)
- 모든 UI 요소는 그 목적이 명확해야 함
- 전문용어 사용 최소화, 일반적인 표현 우선
- 중요 정보는 시각적으로 강조

### 2. 일관성 (Consistency)
- 동일한 요소는 동일한 스타일 적용
- 예측 가능한 인터랙션 패턴
- 통일된 색상과 타이포그래피 사용

### 3. 효율성 (Efficiency)
- 최소한의 클릭으로 목표 달성
- 불필요한 단계 제거
- 빠른 로딩과 반응성 보장

### 4. 안전성 (Safety)
- 중요한 액션에는 확인 단계 제공
- 오류 방지와 명확한 피드백
- 개인정보 보호 강조

### 5. 포용성 (Inclusivity)
- 접근성 지침 준수
- 다양한 디바이스와 환경 지원
- 사용자 능력과 상황 고려

---

## 컬러 시스템

### Primary Colors
```css
/* 메인 브랜드 컬러 - 신뢰감을 주는 블루 */
--primary-50: #EBF8FF;
--primary-100: #BEE3F8;
--primary-200: #90CDF4;
--primary-300: #63B3ED;
--primary-400: #4299E1;
--primary-500: #3182CE; /* Main Brand Color */
--primary-600: #2B77CB;
--primary-700: #2C5282;
--primary-800: #2A4365;
--primary-900: #1A365D;
```

### Secondary Colors
```css
/* 보조 컬러 - 포인트 및 상태 표시 */
--secondary-50: #F7FAFC;
--secondary-100: #EDF2F7;
--secondary-200: #E2E8F0;
--secondary-300: #CBD5E0;
--secondary-400: #A0AEC0;
--secondary-500: #718096;
--secondary-600: #4A5568;
--secondary-700: #2D3748;
--secondary-800: #1A202C;
--secondary-900: #171923;
```

### Status Colors
```css
/* 성공 - 그린 */
--success-50: #F0FFF4;
--success-100: #C6F6D5;
--success-500: #38A169;
--success-600: #2F855A;

/* 경고 - 오렌지 */
--warning-50: #FFFAF0;
--warning-100: #FEEBC8;
--warning-500: #ED8936;
--warning-600: #DD6B20;

/* 오류 - 레드 */
--error-50: #FED7D7;
--error-100: #FED7D7;
--error-500: #E53E3E;
--error-600: #C53030;

/* 정보 - 블루 */
--info-50: #EBF8FF;
--info-100: #BEE3F8;
--info-500: #3182CE;
--info-600: #2B77CB;
```

### Neutral Colors
```css
/* 텍스트 및 배경 */
--gray-50: #F9FAFB;   /* Background Light */
--gray-100: #F3F4F6;  /* Background */
--gray-200: #E5E7EB;  /* Border Light */
--gray-300: #D1D5DB;  /* Border */
--gray-400: #9CA3AF;  /* Text Muted */
--gray-500: #6B7280;  /* Text Secondary */
--gray-600: #4B5563;  /* Text Primary Light */
--gray-700: #374151;  /* Text Primary */
--gray-800: #1F2937;  /* Text Primary Dark */
--gray-900: #111827;  /* Text Emphasis */
```

### 컬러 사용 가이드
- **Primary**: 주요 액션 버튼, 링크, 브랜드 요소
- **Secondary**: 보조 버튼, 아이콘, 경계선
- **Success**: 성공 메시지, 완료 상태
- **Warning**: 주의 메시지, 중요 알림
- **Error**: 오류 메시지, 실패 상태
- **Gray**: 텍스트, 배경, 구분선

---

## 타이포그래피

### 폰트 패밀리
```css
/* 기본 폰트 스택 */
font-family: 
  'Noto Sans KR', /* 한글 */
  'Roboto',       /* 영문 */
  -apple-system, 
  BlinkMacSystemFont,
  'Apple SD Gothic Neo',
  'Malgun Gothic',
  sans-serif;
```

### 폰트 크기 및 행간
```css
/* Heading */
--text-4xl: 2.25rem;  /* 36px - Page Title */
--text-3xl: 1.875rem; /* 30px - Section Title */
--text-2xl: 1.5rem;   /* 24px - Card Title */
--text-xl: 1.25rem;   /* 20px - Sub Title */
--text-lg: 1.125rem;  /* 18px - Large Text */

/* Body */
--text-base: 1rem;      /* 16px - Body Text */
--text-sm: 0.875rem;    /* 14px - Small Text */
--text-xs: 0.75rem;     /* 12px - Caption */

/* Line Height */
--leading-tight: 1.25;  /* Heading */
--leading-normal: 1.5;  /* Body */
--leading-relaxed: 1.625; /* Long Text */
```

### 폰트 두께
```css
--font-light: 300;   /* Light text */
--font-normal: 400;  /* Body text */
--font-medium: 500;  /* Emphasis */
--font-semibold: 600; /* Sub heading */
--font-bold: 700;    /* Heading */
```

### 타이포그래피 클래스
```css
/* Heading Styles */
.heading-1 { font-size: 2.25rem; font-weight: 700; line-height: 1.25; }
.heading-2 { font-size: 1.875rem; font-weight: 600; line-height: 1.25; }
.heading-3 { font-size: 1.5rem; font-weight: 600; line-height: 1.25; }
.heading-4 { font-size: 1.25rem; font-weight: 500; line-height: 1.25; }

/* Body Styles */
.body-large { font-size: 1.125rem; font-weight: 400; line-height: 1.5; }
.body-normal { font-size: 1rem; font-weight: 400; line-height: 1.5; }
.body-small { font-size: 0.875rem; font-weight: 400; line-height: 1.5; }
.caption { font-size: 0.75rem; font-weight: 400; line-height: 1.25; }

/* Emphasis */
.text-emphasis { font-weight: 600; color: var(--gray-900); }
.text-muted { color: var(--gray-500); }
```

---

## 간격 시스템

### 기본 간격 단위 (8px 그리드 시스템)
```css
--space-0: 0;
--space-1: 0.25rem;  /* 4px */
--space-2: 0.5rem;   /* 8px */
--space-3: 0.75rem;  /* 12px */
--space-4: 1rem;     /* 16px */
--space-5: 1.25rem;  /* 20px */
--space-6: 1.5rem;   /* 24px */
--space-8: 2rem;     /* 32px */
--space-10: 2.5rem;  /* 40px */
--space-12: 3rem;    /* 48px */
--space-16: 4rem;    /* 64px */
--space-20: 5rem;    /* 80px */
```

### 컴포넌트별 간격 가이드
- **Component Padding**: 16px (space-4) - 24px (space-6)
- **Content Margin**: 16px (space-4) - 32px (space-8)
- **Section Gap**: 32px (space-8) - 48px (space-12)
- **Page Padding**: 20px (space-5) - 40px (space-10)

### 레이아웃 간격
```css
/* Container */
--container-padding-mobile: var(--space-4);  /* 16px */
--container-padding-tablet: var(--space-6);  /* 24px */
--container-padding-desktop: var(--space-8); /* 32px */

/* Grid Gap */
--grid-gap-small: var(--space-4);   /* 16px */
--grid-gap-medium: var(--space-6);  /* 24px */
--grid-gap-large: var(--space-8);   /* 32px */
```

---

## 컴포넌트 스타일

### 버튼 (Button)
```css
/* Base Button */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-3) var(--space-6);
  border-radius: 8px;
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  line-height: 1.5;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
  border: none;
  min-height: 44px; /* 터치 접근성 */
}

/* Primary Button */
.btn-primary {
  background-color: var(--primary-500);
  color: white;
}
.btn-primary:hover {
  background-color: var(--primary-600);
}
.btn-primary:disabled {
  background-color: var(--gray-300);
  color: var(--gray-500);
  cursor: not-allowed;
}

/* Secondary Button */
.btn-secondary {
  background-color: white;
  color: var(--gray-700);
  border: 1px solid var(--gray-300);
}
.btn-secondary:hover {
  background-color: var(--gray-50);
  border-color: var(--gray-400);
}

/* Danger Button */
.btn-danger {
  background-color: var(--error-500);
  color: white;
}
.btn-danger:hover {
  background-color: var(--error-600);
}
```

### 카드 (Card)
```css
.card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: var(--space-6);
  border: 1px solid var(--gray-200);
}

.card-header {
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--gray-200);
}

.card-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--gray-900);
  margin: 0;
}

.card-content {
  color: var(--gray-700);
}
```

### 폼 요소 (Form)
```css
/* Input */
.input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: var(--text-base);
  line-height: 1.5;
  transition: border-color 0.2s ease-in-out;
  min-height: 44px;
}

.input:focus {
  outline: none;
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.1);
}

.input.error {
  border-color: var(--error-500);
}

/* Label */
.label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--gray-700);
  margin-bottom: var(--space-2);
}

/* Select */
.select {
  appearance: none;
  background-image: url("data:image/svg+xml,..."); /* 드롭다운 아이콘 */
  background-repeat: no-repeat;
  background-position: right var(--space-3) center;
  background-size: 16px;
}
```

### 알림 메시지 (Alert)
```css
.alert {
  padding: var(--space-4);
  border-radius: 8px;
  border-left: 4px solid;
  margin-bottom: var(--space-4);
}

.alert-success {
  background-color: var(--success-50);
  border-color: var(--success-500);
  color: var(--success-800);
}

.alert-warning {
  background-color: var(--warning-50);
  border-color: var(--warning-500);
  color: var(--warning-800);
}

.alert-error {
  background-color: var(--error-50);
  border-color: var(--error-500);
  color: var(--error-800);
}

.alert-info {
  background-color: var(--info-50);
  border-color: var(--info-500);
  color: var(--info-800);
}
```

---

## 반응형 브레이크포인트

### 브레이크포인트 정의
```css
/* Mobile First Approach */
:root {
  --breakpoint-sm: 640px;   /* Small devices */
  --breakpoint-md: 768px;   /* Medium devices */
  --breakpoint-lg: 1024px;  /* Large devices */
  --breakpoint-xl: 1280px;  /* Extra large devices */
}

/* Media Query Mixins */
@media (min-width: 640px) { /* sm */ }
@media (min-width: 768px) { /* md */ }
@media (min-width: 1024px) { /* lg */ }
@media (min-width: 1280px) { /* xl */ }
```

### 반응형 컨테이너
```css
.container {
  width: 100%;
  margin: 0 auto;
  padding: 0 var(--space-4);
}

@media (min-width: 640px) {
  .container {
    max-width: 640px;
    padding: 0 var(--space-6);
  }
}

@media (min-width: 768px) {
  .container {
    max-width: 768px;
  }
}

@media (min-width: 1024px) {
  .container {
    max-width: 1024px;
    padding: 0 var(--space-8);
  }
}
```

### 반응형 그리드
```css
.grid {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: 1fr; /* Mobile: 1 column */
}

@media (min-width: 768px) {
  .grid-md-2 {
    grid-template-columns: repeat(2, 1fr); /* Tablet: 2 columns */
  }
}

@media (min-width: 1024px) {
  .grid-lg-3 {
    grid-template-columns: repeat(3, 1fr); /* Desktop: 3 columns */
  }
}
```

---

## 대상 서비스 특화 컴포넌트

### 요금 정보 카드 (Bill Card)
```css
.bill-card {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  color: white;
  padding: var(--space-6);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(49, 130, 206, 0.2);
}

.bill-amount {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--space-2);
}

.bill-period {
  font-size: var(--text-sm);
  opacity: 0.8;
}

.bill-details {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: var(--space-4);
  margin-top: var(--space-4);
}
```

### 상품 비교 카드 (Product Card)
```css
.product-card {
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: var(--space-6);
  transition: all 0.3s ease;
  position: relative;
}

.product-card.selected {
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.1);
}

.product-card.current {
  background-color: var(--success-50);
  border-color: var(--success-500);
}

.product-badge {
  position: absolute;
  top: -8px;
  right: var(--space-4);
  background-color: var(--primary-500);
  color: white;
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

.product-price {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--primary-600);
}
```

### 진행 상태 표시 (Progress)
```css
.progress-container {
  background-color: var(--gray-100);
  border-radius: 999px;
  height: 8px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-500) 0%, var(--primary-400) 100%);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.progress-step {
  display: flex;
  align-items: center;
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.progress-step.active {
  color: var(--primary-600);
  font-weight: var(--font-medium);
}

.progress-step.completed {
  color: var(--success-600);
}
```

### 상태 뱃지 (Status Badge)
```css
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge.processing {
  background-color: var(--warning-100);
  color: var(--warning-800);
}

.status-badge.completed {
  background-color: var(--success-100);
  color: var(--success-800);
}

.status-badge.failed {
  background-color: var(--error-100);
  color: var(--error-800);
}

.status-badge::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: currentColor;
  margin-right: var(--space-2);
}
```

---

## 인터랙션 패턴

### 애니메이션 타이밍
```css
:root {
  --duration-fast: 0.15s;
  --duration-normal: 0.3s;
  --duration-slow: 0.5s;
  
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
}
```

### 호버 효과
```css
.interactive:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all var(--duration-normal) var(--ease-out);
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
```

### 로딩 상태
```css
.loading {
  position: relative;
  pointer-events: none;
  opacity: 0.6;
}

.loading::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border: 2px solid var(--gray-300);
  border-top: 2px solid var(--primary-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}
```

### 포커스 상태
```css
.focusable:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.3);
  border-radius: 8px;
}

.focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 2px;
}
```

### 상태 전환
```css
.fade-enter {
  opacity: 0;
}
.fade-enter-active {
  opacity: 1;
  transition: opacity var(--duration-normal) var(--ease-out);
}

.slide-enter {
  transform: translateX(100%);
}
.slide-enter-active {
  transform: translateX(0);
  transition: transform var(--duration-normal) var(--ease-out);
}
```

---

## 변경 이력

| 버전 | 날짜 | 변경사항 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2025-01-05 | 초기 스타일 가이드 작성 | 박화면 |

---

## 스타일 가이드 활용 방법

### CSS 변수 사용
모든 스타일 정의에서 CSS 변수를 사용하여 일관성을 유지하고 쉬운 테마 변경을 지원합니다.

### 컴포넌트 기반 설계
재사용 가능한 컴포넌트 스타일을 정의하여 개발 효율성과 일관성을 높입니다.

### 접근성 고려
모든 컴포넌트는 WCAG 2.1 AA 기준을 준수하여 접근성을 보장합니다.

### 반응형 우선
Mobile First 접근 방식으로 모든 디바이스에서 최적의 사용자 경험을 제공합니다.