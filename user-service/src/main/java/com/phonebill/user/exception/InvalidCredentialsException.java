package com.phonebill.user.exception;

/**
 * 잘못된 인증 정보로 인한 예외
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InvalidCredentialsException create() {
        return new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
    
    public static InvalidCredentialsException invalidPassword() {
        return new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
    }
}