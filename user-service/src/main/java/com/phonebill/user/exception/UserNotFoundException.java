package com.phonebill.user.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UserNotFoundException byUserId(String userId) {
        return new UserNotFoundException("사용자를 찾을 수 없습니다. userId: " + userId);
    }
    
}