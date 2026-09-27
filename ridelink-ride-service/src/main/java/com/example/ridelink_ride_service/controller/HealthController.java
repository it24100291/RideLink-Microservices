package com.example.ridelink_ride_service.controller;
import org.springframework.web.bind.annotation.*;
@RestController
public class HealthController {
    @GetMapping("/health") public String health() { return "OK"; }
}
