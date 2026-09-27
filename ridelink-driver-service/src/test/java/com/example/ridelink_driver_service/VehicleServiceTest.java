package com.example.ridelink_driver_service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.model.Vehicle;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.repository.VehicleRepository;
import com.example.ridelink_driver_service.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class VehicleServiceTest {

    @Test
    void shouldCreateVehicleForExistingDriver() {
        DriverRepository driverRepository = org.mockito.Mockito.mock(DriverRepository.class);
        org.mockito.Mockito.when(driverRepository.findById(1L)).thenReturn(new Driver(1L, "Test Driver", "ABC-123", true));

        VehicleRepository vehicleRepository = org.mockito.Mockito.mock(VehicleRepository.class);
        org.mockito.Mockito.when(vehicleRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Vehicle saved = invocation.getArgument(0); saved.setId(1L); return saved;
        });
        VehicleService vehicleService = new VehicleService(driverRepository, vehicleRepository);

        Vehicle vehicle = vehicleService.createVehicle(1L, new Vehicle(null, null, "CAB-1234", "CAR", "Toyota Prius"));

        assertNotNull(vehicle);
        assertEquals(1L, vehicle.getId());
        assertEquals(1L, vehicle.getDriverId());
        assertEquals("CAB-1234", vehicle.getRegistrationNumber());
    }

    @Test
    void shouldThrowWhenDriverDoesNotExist() {
        DriverRepository driverRepository = org.mockito.Mockito.mock(DriverRepository.class);
        VehicleRepository vehicleRepository = org.mockito.Mockito.mock(VehicleRepository.class);
        VehicleService vehicleService = new VehicleService(driverRepository, vehicleRepository);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleService.createVehicle(99L, new Vehicle(null, null, "CAB-1234", "CAR", "Toyota Prius")));

        assertEquals(404, exception.getStatusCode().value());
    }
}
