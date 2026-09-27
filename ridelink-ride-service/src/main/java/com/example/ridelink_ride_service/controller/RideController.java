package com.example.ridelink_ride_service.controller;
import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.service.RideService;
import com.example.ridelink_ride_service.security.Identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RideService rides;
    public RideController(RideService rides){this.rides=rides;}
    @PostMapping
    public Ride create(@RequestAttribute("identity") Identity identity,
            @RequestHeader(value="Idempotency-Key",required=false) UUID key,@Valid @RequestBody Booking request) {
        return rides.create(identity,key==null?UUID.randomUUID():key,request.pickup(),request.destination());
    }
    @GetMapping public List<Ride> mine(@RequestAttribute("identity") Identity identity){return rides.mine(identity);}
    @GetMapping("/{id}") public Ride get(@RequestAttribute("identity") Identity identity,@PathVariable Long id){return rides.get(id,identity);}
    @PatchMapping("/{id}/{action}")
    public Ride transition(@RequestAttribute("identity") Identity identity,@PathVariable Long id,@PathVariable String action) {
        return rides.transition(id,identity,action);
    }
    public record Booking(@NotBlank @Size(max=255) String pickup,@NotBlank @Size(max=255) String destination){ }
}
