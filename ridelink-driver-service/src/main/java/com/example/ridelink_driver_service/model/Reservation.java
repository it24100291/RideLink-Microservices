package com.example.ridelink_driver_service.model;
import jakarta.persistence.*;
@Entity
public class Reservation {
    @Id private String id;
    private Long driverId;
    private Long driverAccountId;
    private boolean released;
    public Reservation() { }
    public Reservation(String id, Long driverId, Long driverAccountId, boolean released) {
        this.id=id; this.driverId=driverId; this.driverAccountId=driverAccountId; this.released=released;
    }
    public String getId() { return id; }
    public Long getDriverId() { return driverId; }
    public Long getDriverAccountId() { return driverAccountId; }
    public boolean isReleased() { return released; }
    public void setReleased(boolean value) { released=value; }
}
