package com.example.ridelink_driver_service;

import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.DriverRepository;
import com.example.ridelink_driver_service.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void createsDriverThroughRepository() {
        Driver driver = new Driver(null, "Alex Driver", "DL12345", true);
        when(driverRepository.findByLicenseNumber("DL12345")).thenReturn(null);
        when(driverRepository.save(driver)).thenReturn(driver);

        assertSame(driver, driverService.createDriver(driver));
        verify(driverRepository).save(driver);
    }

    @Test
    void rejectsDuplicateLicenseNumber() {
        Driver existing = new Driver(1L, "Existing Driver", "DL12345", true);
        Driver duplicate = new Driver(null, "Another Driver", "dl12345", true);
        when(driverRepository.findByLicenseNumber("dl12345")).thenReturn(existing);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> driverService.createDriver(duplicate));

        assertEquals(409, exception.getStatusCode().value());
        verify(driverRepository, org.mockito.Mockito.never()).save(duplicate);
    }

    @Test
    void missingDriverForAvailabilityUpdateReturnsNotFound() {
        when(driverRepository.findById(999L)).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> driverService.updateAvailability(999L, true));

        assertEquals(404, exception.getStatusCode().value());
    }
}
