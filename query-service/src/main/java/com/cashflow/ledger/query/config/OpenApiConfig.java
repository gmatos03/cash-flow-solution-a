package com.cashflow.ledger.query.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Publishes /v3/api-docs and /swagger-ui.html for this service (Appendix H).
 * Declares the same bearer JWT scheme used by {@code JwtAuthenticationFilter}
 * so "Authorize" in Swagger UI can drive a real request end to end.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cash Flow Control - Query Service")
                        .description("Read side of Solution A. Serves balances, statements, and daily cash-flow logs from the projected read model, with Redis cache-aside. See Appendix F.3.")
                        .version("1.0.0")
                        .contact(new Contact().name("Cash Flow Control - Solution A")))
                .servers(List.of(new Server().url("/").description("Local instance")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Obtain a token from POST /auth/token (demo/demo123), then paste it here.")));
    }
}
