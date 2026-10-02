package com.example.ridelink_ride_service.service;
import com.example.ridelink_ride_service.client.DriverGateway;
import com.example.ridelink_ride_service.client.PaymentGateway;
import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.model.RideStatus;
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
    private final PaymentGateway payments;
    public RideService(RideRepository rides,DriverGateway drivers,PaymentGateway payments){this.rides=rides;this.drivers=drivers;this.payments=payments;}
    public synchronized Ride create(Identity passenger,UUID key,String pickup,String destination) {
        passenger.require("PASSENGER");
        String reservation=UUID.nameUUIDFromBytes((passenger.accountId()+":"+key).getBytes(StandardCharsets.UTF_8)).toString();
        Ride existing=rides.findByReservationId(reservation).orElse(null);
        if(existing!=null) {
            if(!existing.getPickup().equals(pickup.trim()) || !existing.getDestination().equals(destination.trim()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking key was already used with different details");
            if(existing.getStatus() == RideStatus.CANCELLED || existing.getStatus() == RideStatus.REQUESTED)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking was cancelled or is recovering. Retry with a new key.");
            return existing;
        }
        Ride ride=rides.saveAndFlush(new Ride(reservation,passenger.accountId(),passenger.name(),pickup.trim(),destination.trim()));
        try {
            var driver=drivers.reserve(reservation);
            ride.setDriverId(driver.driverId()); ride.setDriverAccountId(driver.driverAccountId());
            ride.setStatus(RideStatus.ASSIGNED);
            return rides.saveAndFlush(ride);
        } catch(RuntimeException ex) {
            ride.setStatus(RideStatus.CANCELLED); ride.setReleasePending(true);
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
        RideStatus status=ride.getStatus();
        switch(action) {
            case "accept" -> {
                identity.require("DRIVER");
                requireStatus(status, RideStatus.ASSIGNED);
                requireAssignedDriver(identity, ride);
                ride.setStatus(RideStatus.ACCEPTED);
            }
            case "start" -> {
                identity.require("DRIVER");
                requireStatus(status, RideStatus.ACCEPTED);
                requireAssignedDriver(identity, ride);
                ride.setStatus(RideStatus.IN_PROGRESS);
            }
            case "complete" -> {
                identity.require("DRIVER");
                requireStatus(status, RideStatus.IN_PROGRESS);
                requireAssignedDriver(identity, ride);
                ride.setStatus(RideStatus.COMPLETED); ride.setReleasePending(true);
            }
            case "cancel" -> {
                if (!"ADMIN".equals(identity.role())) identity.require("PASSENGER");
                requireOneOf(status, RideStatus.REQUESTED, RideStatus.ASSIGNED, RideStatus.ACCEPTED);
                ride.setStatus(RideStatus.CANCELLED); ride.setReleasePending(true);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown ride action");
        }
        rides.saveAndFlush(ride);
        release(ride);
        return ride;
    }
    public PaymentGateway.Payment createPayment(Long id, Identity identity, java.math.BigDecimal distanceKm,
            Integer durationMinutes, PaymentGateway.PaymentMethod method) {
        Ride ride = get(id, identity);
        identity.require("PASSENGER");
        if (ride.getStatus() != RideStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment can be created only for a completed ride");
        return payments.create(ride.getId(), distanceKm, durationMinutes, method);
    }
    public PaymentGateway.Payment getPayment(Long id, Identity identity) {
        Ride ride = get(id, identity);
        return payments.get(ride.getId());
    }
    public PaymentGateway.Payment updatePaymentStatus(Long id, Identity identity, PaymentGateway.PaymentStatus status) {
        Ride ride = get(id, identity);
        identity.require("PASSENGER");
        if (ride.getStatus() != RideStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment status is available only for a completed ride");
        return payments.updateStatus(ride.getId(), status);
    }
    public PaymentGateway.Receipt getReceipt(Long id, Identity identity) {
        Ride ride = get(id, identity);
        return payments.getReceipt(ride.getId());
    }
    private void requireAssignedDriver(Identity identity, Ride ride) {
        if (!identity.accountId().equals(ride.getDriverAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned driver can perform this action");
        }
    }
    private void requireStatus(RideStatus actual, RideStatus expected) {
        if (actual != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ride must be " + expected + " for this action; current status is " + actual);
        }
    }
    private void requireOneOf(RideStatus actual, RideStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(status -> status == actual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ride cannot be cancelled from status " + actual);
        }
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
        for(Ride ride:rides.findByReleasePendingTrueOrStatusIn(List.of("REQUESTED", "PENDING"))) {
            try {
                if(ride.getStatus() == RideStatus.REQUESTED) {
                    if(ride.getCreatedAt().isAfter(Instant.now().minusSeconds(30))) continue;
                    ride.setStatus(RideStatus.CANCELLED); ride.setReleasePending(true); rides.saveAndFlush(ride);
                }
                release(ride);
            } catch(RuntimeException ex) {
                LoggerFactory.getLogger(getClass()).warn("Recovery pending for ride {}",ride.getId());
            }
        }
    }
}
