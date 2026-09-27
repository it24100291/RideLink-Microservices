package com.example.ridelink_driver_service.controller;
import org.springframework.web.bind.annotation.*;
@RestController
public class HealthController {
    @GetMapping("/health") public String health() { return "OK"; }
}
