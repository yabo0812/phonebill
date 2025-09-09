package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User Service API 응답 DTO
 * /api/v1/users API에서 반환하는 사용자 정보
 */
public class UserResponseDto {
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("customer_id")
    private String customerId;
    
    @JsonProperty("line_number")
    private String lineNumber;
    
    @JsonProperty("user_name")
    private String userName;
    
    @JsonProperty("account_status")
    private String accountStatus;
    
    @JsonProperty("last_login_at")
    private LocalDateTime lastLoginAt;
    
    @JsonProperty("permissions")
    private List<String> permissions;
    
    // 기본 생성자
    public UserResponseDto() {}
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    @Override
    public String toString() {
        return "UserResponseDto{" +
                "userId='" + userId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", lineNumber='" + lineNumber + '\'' +
                ", userName='" + userName + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", lastLoginAt=" + lastLoginAt +
                ", permissions=" + permissions +
                '}';
    }
}