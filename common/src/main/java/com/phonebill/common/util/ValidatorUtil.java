package com.phonebill.common.util;

import java.util.regex.Pattern;

/**
 * 검증 유틸리티
 * 입력값 검증 관련 공통 기능을 제공합니다.
 */
public class ValidatorUtil {
    
    // 전화번호 패턴 (010-1234-5678, 01012345678)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$");
    
    // 이메일 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    // 사용자 ID 패턴 (영문, 숫자, 3-20자)
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,20}$");
    
    // 비밀번호 패턴 (영문, 숫자, 특수문자 포함 8-20자)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$");
    
    /**
     * 전화번호 형식 검증
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }
    
    /**
     * 이메일 형식 검증
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * 사용자 ID 형식 검증
     */
    public static boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        return USER_ID_PATTERN.matcher(userId.trim()).matches();
    }
    
    /**
     * 비밀번호 형식 검증
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * 문자열이 null이거나 비어있는지 검증
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이거나 비어있지 않은지 검증
     */
    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }
    
    /**
     * 문자열 길이 검증
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength == 0;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * 숫자 문자열 검증
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 양수 검증
     */
    public static boolean isPositiveNumber(String str) {
        if (!isNumeric(str)) {
            return false;
        }
        try {
            long number = Long.parseLong(str.trim());
            return number > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
