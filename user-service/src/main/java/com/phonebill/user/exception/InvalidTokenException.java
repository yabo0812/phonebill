package com.phonebill.user.exception;

/**
 * 유효하지 않은 토큰으로 인한 예외
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InvalidTokenException expired() {
        return new InvalidTokenException("토큰이 만료되었습니다.");
    }
    
    public static InvalidTokenException invalid() {
        return new InvalidTokenException("유효하지 않은 토큰입니다.");
    }
    
    public static InvalidTokenException malformed() {
        return new InvalidTokenException("잘못된 형식의 토큰입니다.");
    }
    
    public static InvalidTokenException signatureInvalid() {
        return new InvalidTokenException("토큰 서명이 유효하지 않습니다.");
    }
    
    public static InvalidTokenException notAccessToken() {
        return new InvalidTokenException("Access Token이 아닙니다.");
    }
    
    public static InvalidTokenException notRefreshToken() {
        return new InvalidTokenException("Refresh Token이 아닙니다.");
    }
}