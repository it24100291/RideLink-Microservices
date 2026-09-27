
package com.example.ridelink_driver_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "drivers")
public class Driver {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(unique = true)
    private Long accountId;
    private String reservationId;
    private String serviceArea;
    private String currentLocation;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long value) { accountId = value; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String value) { reservationId = value; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String value) { serviceArea = value; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String value) { currentLocation = value; }

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
