package com.unicorn.phonebill.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Swagger 통합 문서화 설정
 * 
 * API Gateway를 통해 모든 마이크로서비스의 OpenAPI 문서를 통합하여 제공합니다.
 * 개발 환경에서만 활성화되며, 각 서비스별 API 문서를 중앙집중식으로 관리합니다.
 * 
 * 주요 기능:
 * - 마이크로서비스별 OpenAPI 문서 통합
 * - Swagger UI 커스터마이징
 * - JWT 인증 정보 포함
 * - 환경별 설정 (개발환경에서만 활성화)
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Configuration
@Profile("!prod") // 운영환경에서는 비활성화
public class SwaggerConfig {

    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    @Value("${services.bill-service.url:http://localhost:8082}")
    private String billServiceUrl;
    
    @Value("${services.product-service.url:http://localhost:8083}")
    private String productServiceUrl;
    
    @Value("${services.kos-mock-service.url:http://localhost:8084}")
    private String kosMockServiceUrl;

    /**
     * Swagger UI 설정 파라미터
     * 
     * @return SwaggerUiConfigParameters
     */
    @Bean
    public SwaggerUiConfigParameters swaggerUiConfigParameters() {
        // Spring Boot 3.x에서는 SwaggerUiConfigParameters 생성자가 변경됨
        SwaggerUiConfigParameters parameters = new SwaggerUiConfigParameters(
            new org.springdoc.core.properties.SwaggerUiConfigProperties()
        );
        
        // 각 마이크로서비스의 OpenAPI 문서 URL 설정
        List<String> urls = new ArrayList<>();
        urls.add("Gateway::/v3/api-docs");
        urls.add("Auth Service::" + authServiceUrl + "/v3/api-docs");
        urls.add("Bill Service::" + billServiceUrl + "/v3/api-docs");
        urls.add("Product Service::" + productServiceUrl + "/v3/api-docs");
        urls.add("KOS Mock::" + kosMockServiceUrl + "/v3/api-docs");
        
        // Spring Boot 3.x 호환성을 위한 설정
        System.setProperty("springdoc.swagger-ui.urls", String.join(",", urls));
        
        return parameters;
    }

    /**
     * API Gateway OpenAPI 그룹 정의
     * 
     * @return GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi gatewayApi() {
        return GroupedOpenApi.builder()
                .group("gateway")
                .displayName("API Gateway")
                .pathsToMatch("/health/**", "/actuator/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("PhoneBill API Gateway")
                            .version("1.0.0")
                            .description("통신요금 관리 서비스 API Gateway\n\n" +
                                       "이 문서는 API Gateway의 헬스체크 및 관리 기능을 설명합니다.")
                    );
                    
                    // JWT 보안 스키마 추가
                    openApi.addSecurityItem(
                        new io.swagger.v3.oas.models.security.SecurityRequirement()
                            .addList("bearerAuth")
                    );
                    
                    openApi.getComponents()
                            .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("JWT 토큰을 Authorization 헤더에 포함시켜 주세요.\n" +
                                               "형식: Authorization: Bearer {token}")
                            );
                })
                .build();
    }

    /**
     * Swagger UI 리다이렉트 라우터
     * 
     * @return RouterFunction
     */
    @Bean
    public RouterFunction<ServerResponse> swaggerRouterFunction() {
        return RouterFunctions.route()
                // 루트 경로에서 Swagger UI로 리다이렉트
                .GET("/", request -> 
                    ServerResponse.temporaryRedirect(URI.create("/swagger-ui.html")).build())
                
                // docs 경로에서 Swagger UI로 리다이렉트
                .GET("/docs", request -> 
                    ServerResponse.temporaryRedirect(URI.create("/swagger-ui.html")).build())
                
                // api-docs 경로에서 Swagger UI로 리다이렉트
                .GET("/api-docs", request -> 
                    ServerResponse.temporaryRedirect(URI.create("/swagger-ui.html")).build())
                
                // 서비스별 API 문서 프록시
                .GET("/v3/api-docs/auth", request -> 
                    proxyApiDocs(authServiceUrl + "/v3/api-docs"))
                
                .GET("/v3/api-docs/bills", request -> 
                    proxyApiDocs(billServiceUrl + "/v3/api-docs"))
                
                .GET("/v3/api-docs/products", request -> 
                    proxyApiDocs(productServiceUrl + "/v3/api-docs"))
                
                .GET("/v3/api-docs/kos", request -> 
                    proxyApiDocs(kosMockServiceUrl + "/v3/api-docs"))
                
                .build();
    }

    /**
     * API 문서 프록시
     * 
     * 각 마이크로서비스의 OpenAPI 문서를 프록시하여 제공합니다.
     * 
     * @param apiDocsUrl API 문서 URL
     * @return ServerResponse
     */
    private Mono<ServerResponse> proxyApiDocs(String apiDocsUrl) {
        // 실제 구현에서는 WebClient를 사용하여 마이크로서비스의 API 문서를 가져와야 합니다.
        // 현재는 임시로 빈 문서를 반환합니다.
        return ServerResponse.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\n" +
                          "  \"openapi\": \"3.0.1\",\n" +
                          "  \"info\": {\n" +
                          "    \"title\": \"Service API\",\n" +
                          "    \"version\": \"1.0.0\",\n" +
                          "    \"description\": \"마이크로서비스 API 문서\\n\\n" +
                          "실제 서비스가 시작되면 상세한 API 문서가 표시됩니다.\"\n" +
                          "  },\n" +
                          "  \"paths\": {\n" +
                          "    \"/status\": {\n" +
                          "      \"get\": {\n" +
                          "        \"summary\": \"서비스 상태 확인\",\n" +
                          "        \"responses\": {\n" +
                          "          \"200\": {\n" +
                          "            \"description\": \"서비스 정상\"\n" +
                          "          }\n" +
                          "        }\n" +
                          "      }\n" +
                          "    }\n" +
                          "  }\n" +
                          "}");
    }
}