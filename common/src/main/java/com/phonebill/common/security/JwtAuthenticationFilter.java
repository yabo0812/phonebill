package com.phonebill.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 * HTTP 요청에서 JWT 토큰을 추출하여 인증을 수행
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String token = jwtTokenProvider.resolveToken(request);
        
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserId(token);
            String username = null;
            String authority = null;
            
            try {
                username = jwtTokenProvider.getUsername(token);
            } catch (Exception e) {
                log.debug("JWT에 username 클레임이 없음: {}", e.getMessage());
            }
            
            try {
                authority = jwtTokenProvider.getAuthority(token);
            } catch (Exception e) {
                log.debug("JWT에 authority 클레임이 없음: {}", e.getMessage());
            }
            
            if (StringUtils.hasText(userId)) {
                // UserPrincipal 객체 생성 (username과 authority가 없어도 동작)
                UserPrincipal userPrincipal = UserPrincipal.builder()
                    .userId(userId)
                    .username(username != null ? username : "unknown")
                    .authority(authority != null ? authority : "USER")
                    .build();
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userPrincipal, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority(authority != null ? authority : "USER"))
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("인증된 사용자: {} ({})", userPrincipal.getUsername(), userId);
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") || 
               path.equals("/health");
    }
}