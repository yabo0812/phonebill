package com.unicorn.phonebill.product.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.phonebill.product.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 권한 부족 시 처리하는 Handler
 * 
 * 주요 기능:
 * - 권한이 부족한 요청에 대한 응답 처리
 * - 403 Forbidden 응답 생성
 * - 표준화된 에러 응답 포맷 적용
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "anonymous";
        
        logger.error("권한이 부족한 요청입니다. User: {}, URI: {}, Error: {}", 
                     userId, request.getRequestURI(), accessDeniedException.getMessage());
        
        // 에러 응답 생성
        ErrorResponse errorResponse = createErrorResponse(request, accessDeniedException, userId);
        
        // HTTP 응답 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        // 응답 본문 작성
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    /**
     * 권한 오류 응답 생성
     */
    private ErrorResponse createErrorResponse(HttpServletRequest request, 
                                            AccessDeniedException accessDeniedException, 
                                            String userId) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        String message = "요청한 리소스에 접근할 권한이 없습니다";
        String details = String.format("사용자 '%s'는 '%s %s' 리소스에 접근할 권한이 없습니다", 
                                       userId, method, path);
        
        return ErrorResponse.of("FORBIDDEN", message, details, path);
    }
}