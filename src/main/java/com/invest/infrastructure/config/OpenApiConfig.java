package com.invest.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI investmentsOpportunityMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Investments Opportunity Monitor API")
                        .description("REST API for monitoring FII investment opportunities, managing rules, alerts, and asset tracking")
                        .version("1.0.0"));
    }
}
