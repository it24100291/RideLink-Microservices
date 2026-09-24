
package com.example.ridelink_driver_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Driver {

    private Long id;

    @NotBlank(message = "Driver name is required")
    @Size(min = 2, max = 100,
          message = "Driver name must contain between 2 and 100 characters")
    private String name;

    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 20,
          message = "License number must contain between 5 and 20 characters")
    private String licenseNumber;

    private boolean available;

    public Driver() {
    }

    public Driver(Long id, String name, String licenseNumber, boolean available) {
        this.id = id;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.available = available;
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
}