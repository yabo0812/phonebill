package com.phonebill.kosmock.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Bean
    public OpenAPI kosMockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KOS Mock Service API")
                        .description("KT 통신사 시스템(KOS-Order)을 모방한 Mock 서비스 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("개발팀")
                                .email("dev@phonebill.com"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("http://www.phonebill.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("개발 환경"),
                        new Server()
                                .url("https://kos-mock.phonebill.com" + contextPath)
                                .description("운영 환경")
                ));
    }
}