package com.example.ridelink_driver_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RideLink Driver & Vehicle Service API",
                description = "REST API for managing RideLink drivers, vehicles, availability, service areas, and simulated driver locations.",
                version = "1.0"
        )
)
public class OpenApiConfig {
}