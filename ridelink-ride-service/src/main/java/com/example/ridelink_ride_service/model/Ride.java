package com.example.ridelink_ride_service.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="rides")
public class Ride {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private String reservationId;
    private Long passengerAccountId;
    private String passengerName;
    private String pickup;
    private String destination;
    private Long driverId;
    private Long driverAccountId;
    @Column(nullable=false)
    private String status;
    private boolean releasePending;
    private Instant createdAt;
    public Ride() { }
    public Ride(String reservationId,Long passengerAccountId,String passengerName,String pickup,String destination) {
        this.reservationId=reservationId; this.passengerAccountId=passengerAccountId; this.passengerName=passengerName;
        this.pickup=pickup; this.destination=destination; status=RideStatus.REQUESTED.name(); createdAt=Instant.now();
    }
    public Long getId(){return id;}
    public String getReservationId(){return reservationId;}
    public Long getPassengerAccountId(){return passengerAccountId;}
    public String getPassengerName(){return passengerName;}
    public String getPickup(){return pickup;}
    public String getDestination(){return destination;}
    public Long getDriverId(){return driverId;}
    public Long getDriverAccountId(){return driverAccountId;}
    public RideStatus getStatus(){return RideStatus.fromStoredValue(status);}
    public boolean isReleasePending(){return releasePending;}
    public Instant getCreatedAt(){return createdAt;}
    public void setDriverId(Long v){driverId=v;}
    public void setDriverAccountId(Long v){driverAccountId=v;}
    public void setStatus(RideStatus value){status=java.util.Objects.requireNonNull(value).name();}
    public void setReleasePending(boolean v){releasePending=v;}
}
