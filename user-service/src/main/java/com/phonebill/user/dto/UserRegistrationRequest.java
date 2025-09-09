package com.phonebill.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 등록 요청")
public class UserRegistrationRequest {
    
    @JsonProperty("userId")
    @Schema(description = "사용자 ID", example = "mvno001")
    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(min = 3, max = 20, message = "사용자 ID는 3자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "사용자 ID는 영문, 숫자, _, - 만 사용 가능합니다")
    private String userId;
    
    @JsonProperty("customerId")
    @Schema(description = "고객 ID", example = "CU202401001")
    @NotBlank(message = "고객 ID는 필수입니다")
    @Size(max = 20, message = "고객 ID는 20자 이하여야 합니다")
    private String customerId;
    
    @JsonProperty("lineNumber")
    @Schema(description = "회선번호", example = "010-1234-5678")
    @NotBlank(message = "회선번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "회선번호는 010-XXXX-XXXX 형식이어야 합니다")
    private String lineNumber;
    
    @JsonProperty("userName")
    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(max = 50, message = "사용자 이름은 50자 이하여야 합니다")
    private String userName;
    
    @JsonProperty("password")
    @Schema(description = "비밀번호", example = "securePassword123!")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
    )
    private String password;
    
    @JsonProperty("permissions")
    @Schema(description = "권한 목록", example = "[\"BILL_INQUIRY\", \"PRODUCT_CHANGE\"]")
    @NotEmpty(message = "권한은 최소 1개 이상 필요합니다")
    private List<String> permissions;
}