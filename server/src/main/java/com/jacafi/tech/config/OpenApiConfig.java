package com.jacafi.tech.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Declares the API document and the bearer scheme the endpoints reference.
 *
 * <p>Lives in the shared config package because the document covers every slice; each slice
 * annotates its own controllers.
 */
@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "SINATES",
                        version = "v1",
                        description = "Sistema Integrado de Atendimento e Execucao de Servicos"))
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Token obtained from POST /auth/login")
public class OpenApiConfig {}
