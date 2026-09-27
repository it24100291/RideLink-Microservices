package com.example.ridelink_driver_service.service;
import com.example.ridelink_driver_service.model.Reservation;
import com.example.ridelink_driver_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
    private final DriverRepository drivers;
    private final ReservationRepository reservations;
    private final TransactionTemplate transactions;
    private final DriverService driverService;
    public ReservationService(DriverRepository drivers, ReservationRepository reservations,
            PlatformTransactionManager manager, DriverService driverService) {
        this.drivers=drivers; this.reservations=reservations;
        this.transactions=new TransactionTemplate(manager); this.driverService=driverService;
    }
    // One Driver instance owns its embedded database. Share the same monitor as availability updates.
    public Reservation reserve(String id) {
        synchronized (driverService) {
            return transactions.execute(tx -> {
                var previous = reservations.findById(id).orElse(null);
                if (previous != null) {
                    if (previous.isReleased()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation was already released");
                    return previous;
                }
                var available = drivers.findAvailable();
                if (available.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "No available drivers");
                var driver = available.get(0);
                driver.setAvailable(false); driver.setReservationId(id);
                drivers.save(driver);
                return reservations.saveAndFlush(new Reservation(id, driver.getId(), driver.getAccountId(), false));
            });
        }
    }
    public void release(String id) {
        synchronized (driverService) {
            transactions.executeWithoutResult(tx -> {
                var reservation = reservations.findById(id).orElse(null);
                if (reservation == null) {
                    // A tombstone also blocks a delayed reserve request after a network timeout.
                    reservations.saveAndFlush(new Reservation(id, null, null, true));
                    return;
                }
                if (reservation.isReleased()) return;
                var driver = drivers.findById(reservation.getDriverId());
                if (driver != null && id.equals(driver.getReservationId())) {
                    driver.setReservationId(null); driver.setAvailable(true); drivers.save(driver);
                }
                reservation.setReleased(true); reservations.saveAndFlush(reservation);
            });
        }
    }
}
