package com.example.ridelink_driver_service.model;

public class Driver {

    private Long id;
    private String name;
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