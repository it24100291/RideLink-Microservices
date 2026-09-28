package com.example.ridelink_driver_service.controller;
import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.service.DriverService;
import com.example.ridelink_driver_service.security.Identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Validated
public class DriverController {
    private final DriverService service;
    public DriverController(DriverService service) { this.service=service; }
    @PostMapping
    public Driver create(@RequestAttribute("identity") Identity identity, @Valid @RequestBody CreateDriver request) {
        identity.require("DRIVER");
        Driver driver = new Driver(null, request.name(), request.licenseNumber(), request.available());
        driver.setAccountId(identity.accountId());
        driver.setServiceArea(request.serviceArea()); driver.setCurrentLocation(request.currentLocation());
        return service.createDriver(driver);
    }
    @GetMapping("/me")
    public Driver mine(@RequestAttribute("identity") Identity identity) { return service.mine(identity); }
    @GetMapping("/available")
    public List<AvailableDriver> available(@RequestParam(name="serviceArea", required=false) String serviceArea) {
        return service.getAvailableDrivers(serviceArea).stream().map(d -> new AvailableDriver(d.getId(), d.getName(), d.getServiceArea(), d.getCurrentLocation())).toList();
    }
    @PatchMapping("/{id}/availability")
    public Driver availability(@RequestAttribute("identity") Identity identity, @PathVariable @Positive Long id,
            @RequestParam boolean available) {
        service.owned(id, identity); return service.updateAvailability(id, available);
    }
    @PatchMapping("/{id}/location")
    public Driver location(@RequestAttribute("identity") Identity identity, @PathVariable @Positive Long id,
            @Valid @RequestBody Location request) {
        service.owned(id, identity); return service.updateLocation(id, request.currentLocation());
    }
    public record CreateDriver(@NotBlank @Size(min=2,max=100) String name,
            @NotBlank @Size(min=5,max=20) String licenseNumber, boolean available,
            @Size(max=255) String serviceArea, @Size(max=255) String currentLocation) { }
    public record Location(@NotBlank @Size(max=255) String currentLocation) { }
    public record AvailableDriver(Long id, String name, String serviceArea, String currentLocation) { }
}
