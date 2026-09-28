package com.ridelink.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI rideLinkPaymentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("RideLink Fare & Payment API")
                .version("v1")
                .description("Fare estimation, simulated payment recording and status management, and receipt retrieval. " +
                        "Fare formula: base fare + (distance in km × per-km rate) + " +
                        "(duration in minutes × per-minute rate).")
                .contact(new Contact().name("RideLink API")));
    }
}
