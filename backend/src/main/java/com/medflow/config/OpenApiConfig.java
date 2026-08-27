package com.medflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI medFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MedFlow API")
                        .description("REST API for MedFlow EHR and Clinic Management System")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Use the JWT accessToken returned by register or login. Swagger sends it as Authorization: Bearer <token>.")));
    }
}
