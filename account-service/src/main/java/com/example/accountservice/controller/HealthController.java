package com.example.accountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Operation(
            summary = "Check service health",
            description = "Returns a simple OK response to confirm the service is running."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(type = "string", example = "OK")
            )
    )
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
