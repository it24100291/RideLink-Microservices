package com.example.ridelink_driver_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "vehicles")
public class Vehicle {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private Long driverId;
    @NotBlank(message = "Registration number is required")
    @Size(min = 4, max = 15, message = "Registration number must contain between 4 and 15 characters")
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is required")
    @Size(max = 30, message = "Vehicle type must not exceed 30 characters")
    private String vehicleType;

    @NotBlank(message = "Vehicle model is required")
    @Size(max = 100, message = "Vehicle model must not exceed 100 characters")
    private String model;

    public Vehicle() {
    }

    public Vehicle(Long id, Long driverId, String registrationNumber,
                   String vehicleType, String model) {
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
