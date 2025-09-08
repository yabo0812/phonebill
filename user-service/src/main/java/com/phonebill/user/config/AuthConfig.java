package com.phonebill.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Auth Service 특화 설정 프로퍼티
 */
@Configuration
@ConfigurationProperties(prefix = "auth")
@Getter
@Setter
public class AuthConfig {
    
    private Login login = new Login();
    private Session session = new Session();
    private Password password = new Password();
    
    @Getter
    @Setter
    public static class Login {
        private int maxFailedAttempts = 5;
        private long lockoutDuration = 1800000; // 30분 (milliseconds)
        
        public int getLockoutDurationInSeconds() {
            return (int) (lockoutDuration / 1000);
        }
    }
    
    @Getter
    @Setter
    public static class Session {
        private long defaultTimeout = 1800000;   // 30분 (milliseconds)
        private long autoLoginTimeout = 86400000; // 24시간 (milliseconds)
        
        public int getDefaultTimeoutInSeconds() {
            return (int) (defaultTimeout / 1000);
        }
        
        public int getAutoLoginTimeoutInSeconds() {
            return (int) (autoLoginTimeout / 1000);
        }
    }
    
    @Getter
    @Setter
    public static class Password {
        private int bcryptStrength = 12;
    }
}