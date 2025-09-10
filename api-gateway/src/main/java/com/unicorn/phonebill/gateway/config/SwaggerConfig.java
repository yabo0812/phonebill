package com.unicorn.phonebill.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.net.URI;

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
    
    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;
    
    @Value("${services.bill-service.url:http://localhost:8082}")
    private String billServiceUrl;
    
    @Value("${services.product-service.url:http://localhost:8083}")
    private String productServiceUrl;
    
    @Value("${services.kos-mock.url:http://localhost:8084}")
    private String kosMockUrl;
    
    private final WebClient webClient;
    
    public SwaggerConfig() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * Swagger UI 설정 파라미터
     * 
     * SpringDoc WebFlux에서는 기본 설정을 사용하고 필요시 커스터마이징합니다.
     * 
     * @return SwaggerUiConfigParameters
     */
    @Bean
    public SwaggerUiConfigParameters swaggerUiConfigParameters() {
        return new SwaggerUiConfigParameters(
            new org.springdoc.core.properties.SwaggerUiConfigProperties()
        );
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
                .displayName("🌐 API Gateway")
                .pathsToMatch("/health/**", "/actuator/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("PhoneBill API Gateway")
                            .version("1.0.0")
                            .description("통신요금 관리 서비스 API Gateway\n\n" +
                                       "이 문서는 API Gateway의 헬스체크 및 관리 기능을 설명합니다.\n\n" +
                                       "**주요 기능:**\n" +
                                       "- 마이크로서비스 라우팅\n" +
                                       "- JWT 인증/인가\n" +
                                       "- Circuit Breaker\n" +
                                       "- CORS 처리")
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
     * User Service OpenAPI 그룹 정의
     * 
     * @return GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi userServiceApi() {
        return GroupedOpenApi.builder()
                .group("user-service")
                .displayName("📱 User Service")
                .pathsToMatch("/api/auth/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("User Service API")
                            .version("1.0.0")
                            .description("사용자 인증 및 관리 서비스\n\n" +
                                       "**주요 기능:**\n" +
                                       "- 사용자 로그인/로그아웃\n" +
                                       "- JWT 토큰 발급/갱신\n" +
                                       "- 사용자 정보 관리")
                    );
                })
                .build();
    }
    
    /**
     * Bill Service OpenAPI 그룹 정의
     * 
     * @return GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi billServiceApi() {
        return GroupedOpenApi.builder()
                .group("bill-service")
                .displayName("💰 Bill Service")
                .pathsToMatch("/api/bills/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("Bill Inquiry Service API")
                            .version("1.0.0")
                            .description("통신요금 조회 서비스\n\n" +
                                       "**주요 기능:**\n" +
                                       "- 월별 요금 조회\n" +
                                       "- 요금 상세 내역\n" +
                                       "- 조회 이력 관리")
                    );
                })
                .build();
    }
    
    /**
     * Product Service OpenAPI 그룹 정의
     * 
     * @return GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi productServiceApi() {
        return GroupedOpenApi.builder()
                .group("product-service")
                .displayName("📦 Product Service")
                .pathsToMatch("/api/products/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("Product Change Service API")
                            .version("1.0.0")
                            .description("통신상품 변경 서비스\n\n" +
                                       "**주요 기능:**\n" +
                                       "- 상품 목록 조회\n" +
                                       "- 상품 변경 신청\n" +
                                       "- 변경 이력 관리")
                    );
                })
                .build();
    }
    
    /**
     * KOS Mock Service OpenAPI 그룹 정의
     * 
     * @return GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi kosMockServiceApi() {
        return GroupedOpenApi.builder()
                .group("kos-mock")
                .displayName("🔧 KOS Mock Service")
                .pathsToMatch("/api/kos/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("KOS Mock Service API")
                            .version("1.0.0")
                            .description("KOS 외부 연동 목업 서비스\n\n" +
                                       "**주요 기능:**\n" +
                                       "- 요금 조회 목업\n" +
                                       "- 상품 변경 목업\n" +
                                       "- 테스트 데이터 제공")
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
                
                // Gateway API 문서 직접 제공
                .GET("/v3/api-docs/gateway", request -> 
                    ServerResponse.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .bodyValue(getGatewayApiDoc()))
                
                // 서비스별 API 문서 프록시
                .GET("/v3/api-docs/user", request -> 
                    proxyApiDocs(userServiceUrl + "/v3/api-docs"))
                
                .GET("/v3/api-docs/bill", request -> 
                    proxyApiDocs(billServiceUrl + "/v3/api-docs"))
                
                .GET("/v3/api-docs/product", request -> 
                    proxyApiDocs(productServiceUrl + "/v3/api-docs"))
                
                .GET("/v3/api-docs/kos", request -> 
                    proxyApiDocs(kosMockUrl + "/v3/api-docs"))

                .build();
    }

    /**
     * API 문서 프록시
     * 
     * 각 마이크로서비스의 OpenAPI 문서를 프록시하여 제공합니다.
     * Gateway 경로로 서버 정보를 수정하여 반환합니다.
     * 
     * @param apiDocsUrl API 문서 URL
     * @return ServerResponse
     */
    private Mono<ServerResponse> proxyApiDocs(String apiDocsUrl) {
        return webClient.get()
                .uri(apiDocsUrl)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> 
                    Mono.error(new RuntimeException("Service unavailable")))
                .bodyToMono(String.class)
                .map(this::modifyOpenApiServers)
                .onErrorReturn(getDefaultApiDoc(apiDocsUrl))
                .flatMap(body -> 
                    ServerResponse.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                );
    }
    
    /**
     * OpenAPI 문서의 서버 정보를 Gateway 경로로 수정
     * 
     * @param openApiJson 원본 OpenAPI JSON
     * @return 수정된 OpenAPI JSON
     */
    private String modifyOpenApiServers(String openApiJson) {
        try {
            // JSON 파싱을 위한 간단한 문자열 치환
            // 실제 프로덕션에서는 Jackson ObjectMapper 사용 권장
            String modified = openApiJson;
            
            // 서버 정보를 Gateway 기반으로 수정
            if (openApiJson.contains("user-service") || openApiJson.contains("8081")) {
                modified = addGatewayServerInfo(modified, "/api/auth", "User Service");
            } else if (openApiJson.contains("bill-service") || openApiJson.contains("8082")) {
                modified = addGatewayServerInfo(modified, "/api/bills", "Bill Service");
            } else if (openApiJson.contains("product-service") || openApiJson.contains("8083")) {
                modified = addGatewayServerInfo(modified, "/api/products", "Product Service");
            } else if (openApiJson.contains("kos-mock") || openApiJson.contains("8084")) {
                modified = addGatewayServerInfo(modified, "/api/kos", "KOS Mock Service");
            }
            
            return modified;
        } catch (Exception e) {
            // JSON 수정 실패 시 원본 반환
            return openApiJson;
        }
    }
    
    /**
     * OpenAPI JSON에 Gateway 서버 정보 추가
     * 
     * @param openApiJson 원본 OpenAPI JSON
     * @param basePath Gateway 기반 경로
     * @param serviceName 서비스명
     * @return 수정된 OpenAPI JSON
     */
    private String addGatewayServerInfo(String openApiJson, String basePath, String serviceName) {
        // servers 섹션을 Gateway 정보로 교체
        String serverInfo = "\"servers\": [" +
                "    {" +
                "      \"url\": \"" + basePath + "\"," +
                "      \"description\": \"" + serviceName + " via API Gateway\"" +
                "    }" +
                "  ],";
        
        // 기존 servers 정보가 있으면 교체, 없으면 info 다음에 추가
        if (openApiJson.contains("\"servers\"")) {
            return openApiJson.replaceFirst(
                "\"servers\":\\s*\\[[^\\]]*\\],?", 
                serverInfo
            );
        } else {
            return openApiJson.replaceFirst(
                "(\"info\":\\s*\\{[^}]*\\},?)", 
                "$1\n  " + serverInfo
            );
        }
    }
    
    /**
     * Gateway API 문서 생성
     * 
     * Gateway 자체의 OpenAPI 문서를 생성합니다.
     * 
     * @return Gateway API 문서 JSON
     */
    private String getGatewayApiDoc() {
        return "{\n" +
                "  \"openapi\": \"3.0.1\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"PhoneBill API Gateway\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"description\": \"통신요금 관리 서비스 API Gateway\\n\\n" +
                "이 문서는 API Gateway의 헬스체크 및 관리 기능을 설명합니다.\"\n" +
                "  },\n" +
                "  \"paths\": {\n" +
                "    \"/health\": {\n" +
                "      \"get\": {\n" +
                "        \"summary\": \"헬스 체크\",\n" +
                "        \"description\": \"API Gateway 서비스 상태를 확인합니다.\",\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"서비스 정상\",\n" +
                "            \"content\": {\n" +
                "              \"application/json\": {\n" +
                "                \"schema\": {\n" +
                "                  \"type\": \"object\",\n" +
                "                  \"properties\": {\n" +
                "                    \"status\": { \"type\": \"string\" }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"/actuator/health\": {\n" +
                "      \"get\": {\n" +
                "        \"summary\": \"Actuator 헬스 체크\",\n" +
                "        \"description\": \"Spring Boot Actuator 헬스 체크 엔드포인트\",\n" +
                "        \"responses\": {\n" +
                "          \"200\": {\n" +
                "            \"description\": \"헬스 체크 결과\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"components\": {\n" +
                "    \"securitySchemes\": {\n" +
                "      \"bearerAuth\": {\n" +
                "        \"type\": \"http\",\n" +
                "        \"scheme\": \"bearer\",\n" +
                "        \"bearerFormat\": \"JWT\",\n" +
                "        \"description\": \"JWT 토큰을 Authorization 헤더에 포함시켜 주세요.\\nFormat: Authorization: Bearer {token}\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
    
    /**
     * 기본 API 문서 생성
     * 
     * 서비스에 접근할 수 없을 때 반환할 기본 문서를 생성합니다.
     * 
     * @param apiDocsUrl API 문서 URL
     * @return 기본 API 문서 JSON
     */
    private String getDefaultApiDoc(String apiDocsUrl) {
        String serviceName = extractServiceName(apiDocsUrl);
        return "{\n" +
                "  \"openapi\": \"3.0.1\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"" + serviceName + " API\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"description\": \"" + serviceName + " 마이크로서비스 API 문서\\n\\n" +
                "서비스가 시작되지 않았거나 연결할 수 없습니다.\"\n" +
                "  },\n" +
                "  \"paths\": {},\n" +
                "  \"components\": {}\n" +
                "}";
    }
    
    /**
     * URL에서 서비스명 추출
     * 
     * @param apiDocsUrl API 문서 URL
     * @return 서비스명
     */
    private String extractServiceName(String apiDocsUrl) {
        if (apiDocsUrl.contains("8081")) return "User Service";
        if (apiDocsUrl.contains("8082")) return "Bill Service";
        if (apiDocsUrl.contains("8083")) return "Product Service";
        if (apiDocsUrl.contains("8084")) return "KOS Mock Service";
        return "Unknown Service";
    }
}