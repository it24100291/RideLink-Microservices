package com.ridelink.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI rideLinkPaymentOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("ServiceKey",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER)
                                .name("X-Service-Key")))
                .addSecurityItem(new SecurityRequirement().addList("ServiceKey"))
                .info(new Info()
                .title("RideLink Fare & Payment API")
                .version("v1")
                .description("Fare estimation, simulated payment recording and status management, and receipt retrieval. " +
                        "Fare formula: base fare + (distance in km × per-km rate) + " +
                        "(duration in minutes × per-minute rate).")
                .contact(new Contact().name("RideLink API")));
    }
}
