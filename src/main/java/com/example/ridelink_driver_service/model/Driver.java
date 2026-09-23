package com.example.ridelink_driver_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Generated driver identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Schema(description = "Driver's full name", example = "Ayesha Perera")
    private String name;

    @NotBlank(message = "License number is required")
    @Schema(description = "Driver's license number", example = "LIC-1234")
    private String licenseNumber;

    @Schema(description = "Whether the driver is currently available", example = "true")
    private boolean available;

    @NotBlank(message = "Service area is required")
    @Schema(description = "Primary service area", example = "Malabe")
    private String serviceArea;

    @NotBlank(message = "Current location is required")
    @Schema(description = "Simulated current driver location", example = "SLIIT Malabe")
    private String currentLocation;

    public Driver() {
    }

    public Driver(Long id, String name, String licenseNumber, boolean available) {
        this.id = id;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.available = available;
    }

    public Driver(Long id, String name, String licenseNumber, boolean available, String serviceArea, String currentLocation) {
        this.id = id;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.available = available;
        this.serviceArea = serviceArea;
        this.currentLocation = currentLocation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
}