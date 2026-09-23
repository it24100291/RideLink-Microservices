package com.example.ridelink_driver_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Generated vehicle identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Identifier of the owning driver", example = "1")
    private Long driverId;

    @NotBlank(message = "Registration number is required")
    @Schema(description = "Vehicle registration number", example = "ABC-1234")
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is required")
    @Schema(description = "Type of vehicle", example = "Car")
    private String vehicleType;

    @NotBlank(message = "Model is required")
    @Schema(description = "Vehicle model", example = "Toyota Prius")
    private String model;

    public Vehicle() {
    }

    public Vehicle(Long id, Long driverId, String registrationNumber, String vehicleType, String model) {
        this.id = id;
        this.driverId = driverId;
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.model = model;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
