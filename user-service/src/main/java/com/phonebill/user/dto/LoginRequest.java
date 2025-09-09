package com.phonebill.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @JsonProperty("userId")
    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(min = 3, max = 20, message = "사용자 ID는 3-20자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "사용자 ID는 영문, 숫자, '_', '-'만 사용 가능합니다")
    private String userId;
    
    @JsonProperty("password")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 50, message = "비밀번호는 8-50자 사이여야 합니다")
    private String password;
    
    @Builder.Default
    private Boolean autoLogin = false;
    
    // 보안을 위해 toString에서 비밀번호 제외
    @Override
    public String toString() {
        return "LoginRequest{" +
                "userId='" + userId + '\'' +
                ", autoLogin=" + autoLogin +
                '}';
    }
}