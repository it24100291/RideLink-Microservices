package com.example.ridelink_ride_service.service;
import com.example.ridelink_ride_service.client.DriverGateway;
import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.repository.RideRepository;
import com.example.ridelink_ride_service.security.Identity;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class RideService {
    private final RideRepository rides;
    private final DriverGateway drivers;
    public RideService(RideRepository rides,DriverGateway drivers){this.rides=rides;this.drivers=drivers;}
    public synchronized Ride create(Identity passenger,UUID key,String pickup,String destination) {
        passenger.require("PASSENGER");
        String reservation=UUID.nameUUIDFromBytes((passenger.accountId()+":"+key).getBytes(StandardCharsets.UTF_8)).toString();
        Ride existing=rides.findByReservationId(reservation).orElse(null);
        if(existing!=null) {
            if(!existing.getPickup().equals(pickup.trim()) || !existing.getDestination().equals(destination.trim()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking key was already used with different details");
            if("CANCELLED".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking was cancelled or is recovering. Retry with a new key.");
            return existing;
        }
        Ride ride=rides.saveAndFlush(new Ride(reservation,passenger.accountId(),passenger.name(),pickup.trim(),destination.trim()));
        try {
            var driver=drivers.reserve(reservation);
            ride.setDriverId(driver.driverId()); ride.setDriverAccountId(driver.driverAccountId());
            ride.setStatus("CONFIRMED");
            return rides.saveAndFlush(ride);
        } catch(RuntimeException ex) {
            ride.setStatus("CANCELLED"); ride.setReleasePending(true);
            rides.saveAndFlush(ride);
            release(ride);
            throw ex;
        }
    }
    public Ride get(Long id,Identity identity) {
        Ride ride=rides.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ride not found"));
        boolean passenger="PASSENGER".equals(identity.role()) && identity.accountId().equals(ride.getPassengerAccountId());
        boolean driver="DRIVER".equals(identity.role()) && identity.accountId().equals(ride.getDriverAccountId());
        if(!passenger && !driver && !"ADMIN".equals(identity.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This ride belongs to another account");
        return ride;
    }
    public List<Ride> mine(Identity identity) {
        return switch(identity.role()) {
            case "PASSENGER" -> rides.findByPassengerAccountIdOrderByIdDesc(identity.accountId());
            case "DRIVER" -> rides.findByDriverAccountIdOrderByIdDesc(identity.accountId());
            case "ADMIN" -> rides.findAll();
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Unsupported role");
        };
    }
    public synchronized Ride transition(Long id,Identity identity,String action) {
        Ride ride=get(id,identity);
        String status=ride.getStatus();
        switch(action) {
            case "start" -> {
                identity.require("DRIVER");
                if(!identity.accountId().equals(ride.getDriverAccountId())) forbidden();
                if("IN_PROGRESS".equals(status)) return ride;
                requireStatus(status,"CONFIRMED");
                ride.setStatus("IN_PROGRESS");
            }
            case "complete" -> {
                identity.require("DRIVER");
                if(!identity.accountId().equals(ride.getDriverAccountId())) forbidden();
                if("COMPLETED".equals(status)) { release(ride); return ride; }
                requireStatus(status,"IN_PROGRESS");
                ride.setStatus("COMPLETED"); ride.setReleasePending(true);
            }
            case "cancel" -> {
                if("CANCELLED".equals(status)) { release(ride); return ride; }
                requireStatus(status,"CONFIRMED");
                ride.setStatus("CANCELLED"); ride.setReleasePending(true);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown ride action");
        }
        rides.saveAndFlush(ride);
        release(ride);
        return ride;
    }
    private void forbidden(){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned driver can perform this action");}
    private void requireStatus(String actual,String expected) {
        if(!actual.equals(expected)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ride must be "+expected+" for this action");
    }
    private void release(Ride ride) {
        if(!ride.isReleasePending()) return;
        try { drivers.release(ride.getReservationId()); }
        catch(RuntimeException ex) {
            LoggerFactory.getLogger(getClass()).warn("Release pending for ride {}; retrying in background",ride.getId());
            return;
        }
        ride.setReleasePending(false); rides.saveAndFlush(ride);
    }
    @Scheduled(fixedDelayString="${ride.recovery-delay-ms:5000}")
    public synchronized void recover() {
        for(Ride ride:rides.findByReleasePendingTrueOrStatus("PENDING")) {
            try {
                if("PENDING".equals(ride.getStatus())) {
                    if(ride.getCreatedAt().isAfter(Instant.now().minusSeconds(30))) continue;
                    ride.setStatus("CANCELLED"); ride.setReleasePending(true); rides.saveAndFlush(ride);
                }
                release(ride);
            } catch(RuntimeException ex) {
                LoggerFactory.getLogger(getClass()).warn("Recovery pending for ride {}",ride.getId());
            }
        }
    }
}
