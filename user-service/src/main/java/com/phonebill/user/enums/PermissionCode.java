package com.phonebill.user.enums;

/**
 * 권한 코드 열거형
 * 시스템에서 사용 가능한 모든 권한 코드를 정의
 */
public enum PermissionCode {
    
    /**
     * 요금 조회 서비스 권한
     */
    BILL_INQUIRY("요금 조회 서비스 권한"),
    
    /**
     * 상품 변경 서비스 권한  
     */
    PRODUCT_CHANGE("상품 변경 서비스 권한"),
    
    /**
     * 관리자 권한
     */
    ADMIN("관리자 권한"),
    
    /**
     * 사용자 관리 권한
     */
    USER_MANAGEMENT("사용자 관리 권한");
    
    private final String description;
    
    PermissionCode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCode() {
        return this.name();
    }
    
    /**
     * 권한 코드 문자열로부터 enum 객체를 찾는다
     * @param code 권한 코드 문자열
     * @return PermissionCode enum 객체
     * @throws IllegalArgumentException 유효하지 않은 권한 코드인 경우
     */
    public static PermissionCode fromCode(String code) {
        try {
            return PermissionCode.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 권한 코드입니다: " + code);
        }
    }
    
    /**
     * 모든 권한 코드 문자열을 배열로 반환
     * @return 권한 코드 문자열 배열
     */
    public static String[] getAllCodes() {
        PermissionCode[] values = PermissionCode.values();
        String[] codes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            codes[i] = values[i].name();
        }
        return codes;
    }
}