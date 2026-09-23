package com.example.ridelink_driver_service.model;

public class Vehicle {

    private Long id;
    private Long driverId;
    private String registrationNumber;
    private String vehicleType;
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