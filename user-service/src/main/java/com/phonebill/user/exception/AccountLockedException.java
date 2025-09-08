package com.phonebill.user.exception;

import java.time.LocalDateTime;

/**
 * 계정이 잠겨있을 때 발생하는 예외
 */
public class AccountLockedException extends RuntimeException {
    
    private final LocalDateTime lockedUntil;
    
    public AccountLockedException(String message, LocalDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }
    
    public AccountLockedException(String message, Throwable cause, LocalDateTime lockedUntil) {
        super(message, cause);
        this.lockedUntil = lockedUntil;
    }
    
    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
    
    public static AccountLockedException create(LocalDateTime lockedUntil) {
        return new AccountLockedException("계정이 잠금 상태입니다. 잠금 해제 시간: " + lockedUntil, lockedUntil);
    }
    
    public static AccountLockedException create(String userId, LocalDateTime lockedUntil) {
        return new AccountLockedException(
            String.format("계정이 잠금 상태입니다. userId: %s, 잠금 해제 시간: %s", userId, lockedUntil), 
            lockedUntil
        );
    }
}