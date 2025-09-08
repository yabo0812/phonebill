package com.phonebill.bill.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정
 * 
 * JWT 기반 인증/인가 시스템 구성
 * - Stateless 인증 방식
 * - API 엔드포인트별 접근 권한 설정
 * - CORS 설정
 * - 예외 처리 설정
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 보안 필터 체인 구성
     * 
     * @param http HTTP 보안 설정
     * @return 보안 필터 체인
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Security Filter Chain 구성 시작");

        http
            // CSRF 비활성화 (REST API는 CSRF 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정 활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 관리 - Stateless (JWT 사용)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청별 인증/인가 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트 - 인증 불필요
                .requestMatchers(
                    // Health Check
                    "/actuator/**",
                    // Swagger UI
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    // 정적 리소스
                    "/favicon.ico",
                    "/error"
                ).permitAll()
                
                // OPTIONS 요청은 모두 허용 (CORS Preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 요금 조회 API - 인증 필요
                .requestMatchers("/api/bills/**").authenticated()
                
                // 나머지 모든 요청 - 인증 필요
                .anyRequest().authenticated()
            )
            
            // JWT 인증 필터 추가
            // TODO: JWT 필터 구현 후 활성화
            // .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // 예외 처리
            .exceptionHandling(exception -> exception
                // 인증 실패 시 처리
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("인증 실패 - URI: {}, 오류: {}", 
                        request.getRequestURI(), authException.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                          "success": false,
                          "message": "인증이 필요합니다",
                          "timestamp": "%s"
                        }
                        """.formatted(java.time.LocalDateTime.now()));
                })
                
                // 권한 부족 시 처리
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("접근 거부 - URI: {}, 오류: {}", 
                        request.getRequestURI(), accessDeniedException.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                          "success": false,
                          "message": "접근 권한이 없습니다",
                          "timestamp": "%s"
                        }
                        """.formatted(java.time.LocalDateTime.now()));
                })
            );

        log.info("Security Filter Chain 구성 완료");
        return http.build();
    }

    /**
     * CORS 설정
     * 
     * @return CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("CORS 설정 구성 시작");

        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정 (개발환경)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "http://127.0.0.1:*",
            "https://127.0.0.1:*"
            // TODO: 운영환경 도메인 추가
        ));
        
        // 허용할 HTTP 메소드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // 자격 증명 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.debug("CORS 설정 구성 완료");
        return source;
    }

    /**
     * 비밀번호 인코더 구성
     * 
     * @return BCrypt 패스워드 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Password Encoder 구성 - BCrypt 사용");
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 매니저 구성
     * 
     * @param config 인증 설정
     * @return 인증 매니저
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.debug("Authentication Manager 구성");
        return config.getAuthenticationManager();
    }

    /**
     * JWT 인증 필터 구성
     * 
     * TODO: JWT 토큰 검증 필터 구현
     * 
     * @return JWT 인증 필터
     */
    // @Bean
    // public JwtAuthenticationFilter jwtAuthenticationFilter() {
    //     return new JwtAuthenticationFilter();
    // }

    /**
     * JWT 토큰 제공자 구성
     * 
     * TODO: JWT 토큰 생성/검증 서비스 구현
     * 
     * @return JWT 토큰 제공자
     */
    // @Bean
    // public JwtTokenProvider jwtTokenProvider() {
    //     return new JwtTokenProvider();
    // }
}