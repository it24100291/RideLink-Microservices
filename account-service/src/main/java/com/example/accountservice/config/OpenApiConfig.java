package com.example.accountservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RideLink Account Service API",
                description = "Account registration, login, self-service profile routes, and admin status management for RideLink.",
                version = "1.0"
        )
)
public class OpenApiConfig {
}
