package com.unicorn.phonebill.product.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.phonebill.product.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 실패 시 처리하는 EntryPoint
 * 
 * 주요 기능:
 * - 인증되지 않은 요청에 대한 응답 처리
 * - 401 Unauthorized 응답 생성
 * - 표준화된 에러 응답 포맷 적용
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        logger.error("인증되지 않은 요청입니다. URI: {}, Error: {}", 
                     request.getRequestURI(), authException.getMessage());
        
        // 에러 응답 생성
        ErrorResponse errorResponse = createErrorResponse(request, authException);
        
        // HTTP 응답 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 응답 본문 작성
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    /**
     * 인증 오류 응답 생성
     */
    private ErrorResponse createErrorResponse(HttpServletRequest request, AuthenticationException authException) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 요청 컨텍스트에 따른 오류 메시지 생성
        String message = determineErrorMessage(request, authException);
        String details = String.format("요청한 리소스에 접근하기 위해서는 인증이 필요합니다. [%s %s]", method, path);
        
        return ErrorResponse.of("UNAUTHORIZED", message, details, path);
    }

    /**
     * 인증 오류 메시지 결정
     */
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        String authHeader = request.getHeader("Authorization");
        
        // Authorization 헤더가 없는 경우
        if (authHeader == null) {
            return "인증 토큰이 제공되지 않았습니다";
        }
        
        // Bearer 토큰 형식이 아닌 경우
        if (!authHeader.startsWith("Bearer ")) {
            return "올바르지 않은 인증 토큰 형식입니다. Bearer 토큰이 필요합니다";
        }
        
        // 토큰은 있지만 유효하지 않은 경우
        return "제공된 인증 토큰이 유효하지 않습니다";
    }
}