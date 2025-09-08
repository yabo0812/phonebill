package com.phonebill.common.exception;

/**
 * 인증되지 않은 요청에 대한 예외
 * JWT 토큰이 유효하지 않거나 만료된 경우 발생
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 401);
    }
    
    public UnauthorizedException() {
        super("인증이 필요합니다.", "UNAUTHORIZED", 401);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, "UNAUTHORIZED", 401, cause);
    }
}