package com.unicorn.phonebill.product.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정
 * Product Service API 문서화를 위한 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addServersItem(new Server()
                        .url("http://localhost:8083")
                        .description("Local Development"))
                .addServersItem(new Server()
                        .url("{protocol}://{host}:{port}")
                        .description("Custom Server")
                        .variables(new io.swagger.v3.oas.models.servers.ServerVariables()
                                .addServerVariable("protocol", new io.swagger.v3.oas.models.servers.ServerVariable()
                                        ._default("http")
                                        .description("Protocol (http or https)")
                                        .addEnumItem("http")
                                        .addEnumItem("https"))
                                .addServerVariable("host", new io.swagger.v3.oas.models.servers.ServerVariable()
                                        ._default("localhost")
                                        .description("Server host"))
                                .addServerVariable("port", new io.swagger.v3.oas.models.servers.ServerVariable()
                                        ._default("8083")
                                        .description("Server port"))))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Product Service API")
                .description("통신요금 관리 서비스 - 상품 변경 및 관리 API")
                .version("1.0.0")
                .contact(new Contact()
                        .name("PhoneBill Development Team")
                        .email("dev@phonebill.com"));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}