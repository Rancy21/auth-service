package com.larr.auth.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    public OpenAPI OpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service")
                        .version("1.0").description("Authentication service - register, login, OAuth, password reset"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(
                        new Components().addSecuritySchemes("Bearer",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer").bearerFormat("JWT")
                                        .description("Enter your JWT access token")));
    }
}
