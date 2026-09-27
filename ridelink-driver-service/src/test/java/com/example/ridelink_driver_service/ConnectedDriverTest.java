package com.example.ridelink_driver_service;
import com.example.ridelink_driver_service.model.Driver;
import com.example.ridelink_driver_service.repository.*;
import com.example.ridelink_driver_service.security.*;
import com.example.ridelink_driver_service.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="integration.ride-service-key=test-ride-key")
@AutoConfigureMockMvc
class ConnectedDriverTest {
    @Autowired MockMvc mvc;
    @Autowired DriverService drivers;
    @Autowired ReservationService reservations;
    @Autowired ReservationRepository reservationStore;
    @Autowired DriverStore driverStore;
    @Autowired VehicleStore vehicleStore;
    @MockBean AccountClient accounts;
    @BeforeEach void setup(){
        reservationStore.deleteAll(); vehicleStore.deleteAll(); driverStore.deleteAll();
        when(accounts.authenticate("Bearer driver")).thenReturn(new Identity(20L,"Driver","DRIVER"));
        when(accounts.authenticate("Bearer other")).thenReturn(new Identity(21L,"Other","DRIVER"));
        when(accounts.authenticate("Bearer passenger")).thenReturn(new Identity(10L,"Passenger","PASSENGER"));
        when(accounts.authenticate(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    Driver driver(){
        var d=new Driver(null,"Test Driver","LICENSE123",true); d.setAccountId(20L);
        return drivers.createDriver(d);
    }
    @Test void ownershipAndInputIdentityAreEnforced() throws Exception {
        mvc.perform(post("/api/drivers").header("Authorization","Bearer driver").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Driver\",\"licenseNumber\":\"LICENSE123\",\"accountId\":999,\"id\":999,\"available\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value(20));
        var d=driverStore.findByAccountId(20L).orElseThrow();
        mvc.perform(patch("/api/drivers/{id}/availability",d.getId()).param("available","false").header("Authorization","Bearer other"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/drivers").header("Authorization","Bearer passenger").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Passenger\",\"licenseNumber\":\"LICENSE456\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/drivers/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/drivers/{id}/vehicles",d.getId()).header("Authorization","Bearer other")
                .contentType(MediaType.APPLICATION_JSON).content("{\"registrationNumber\":\"ABC1234\",\"vehicleType\":\"CAR\",\"model\":\"Toyota\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void reservationRequiresServiceCredentialAndBlocksAvailabilityOverride() throws Exception {
        var d=driver(); String id=UUID.randomUUID().toString();
        mvc.perform(put("/internal/reservations/{id}",id)).andExpect(status().isForbidden());
        mvc.perform(put("/internal/reservations/{id}",id).header("X-Service-Key","test-ride-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.driverAccountId").value(20));
        mvc.perform(patch("/api/drivers/{id}/availability",d.getId()).param("available","true").header("Authorization","Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/internal/reservations/{id}",id).header("X-Service-Key","test-ride-key")).andExpect(status().isNoContent());
        assertThat(drivers.get(d.getId()).isAvailable()).isTrue();
    }
    @Test void competingBookingsOnlyReserveOneDriverAndRetriesAreIdempotent() throws Exception {
        var d=driver();
        try(var pool=Executors.newFixedThreadPool(2)){
            var barrier=new CyclicBarrier(2);
            List<Callable<Boolean>> tasks=new ArrayList<>();
            for(int i=0;i<2;i++) tasks.add(()->{
                barrier.await();
                try { reservations.reserve(UUID.randomUUID().toString()); return true; }
                catch(ResponseStatusException ex){assertThat(ex.getStatusCode().value()).isEqualTo(409);return false;}
            });
            var results=pool.invokeAll(tasks);
            assertThat(results.stream().filter(f->{try{return f.get();}catch(Exception ex){throw new RuntimeException(ex);}}).count()).isEqualTo(1);
        }
        String id=drivers.get(d.getId()).getReservationId();
        assertThat(reservations.reserve(id).getDriverId()).isEqualTo(d.getId());
        reservations.release(id); reservations.release(id);
        assertThat(drivers.get(d.getId()).isAvailable()).isTrue();
        String cancelled=UUID.randomUUID().toString();
        reservations.release(cancelled);
        assertThatThrownBy(()->reservations.reserve(cancelled)).isInstanceOf(ResponseStatusException.class);
    }
}
