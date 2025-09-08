package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 공통 오류 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    @Builder.Default
    private Boolean success = false;

    @Valid
    private ErrorData error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorData {

        @NotNull(message = "오류 코드는 필수입니다")
        private String code;

        @NotNull(message = "오류 메시지는 필수입니다")
        private String message;

        private String details;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();

        private String path;
    }

    /**
     * 오류 응답 생성
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.of(code, message, null, null);
    }

    /**
     * 상세 오류 응답 생성
     */
    public static ErrorResponse of(String code, String message, String details, String path) {
        ErrorData errorData = ErrorData.builder()
                .code(code)
                .message(message)
                .details(details)
                .path(path)
                .build();

        return ErrorResponse.builder()
                .error(errorData)
                .build();
    }

    /**
     * 검증 오류 응답 생성
     */
    public static ErrorResponse validationError(String message) {
        return of("INVALID_REQUEST", message);
    }

    /**
     * 인증 오류 응답 생성
     */
    public static ErrorResponse unauthorized(String message) {
        return of("UNAUTHORIZED", message != null ? message : "인증이 필요합니다");
    }

    /**
     * 권한 오류 응답 생성
     */
    public static ErrorResponse forbidden(String message) {
        return of("FORBIDDEN", message != null ? message : "서비스 이용 권한이 없습니다");
    }

    /**
     * 서버 오류 응답 생성
     */
    public static ErrorResponse internalServerError(String message) {
        return of("INTERNAL_SERVER_ERROR", message != null ? message : "서버 내부 오류가 발생했습니다");
    }
}