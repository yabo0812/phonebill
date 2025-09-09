package com.phonebill.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그아웃 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    
    @JsonProperty("userId")
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @JsonProperty("refreshToken")
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;
    
    // 보안을 위해 toString에서 토큰 일부만 표시
    @Override
    public String toString() {
        return "LogoutRequest{" +
                "userId='" + userId + '\'' +
                ", refreshToken='" + (refreshToken != null ? refreshToken.substring(0, Math.min(refreshToken.length(), 10)) + "..." : "null") +
                '}';
    }
}
