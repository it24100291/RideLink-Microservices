package com.example.ridelink_ride_service;
import com.example.ridelink_ride_service.client.DriverGateway;
import com.example.ridelink_ride_service.repository.RideRepository;
import com.example.ridelink_ride_service.security.*;
import com.example.ridelink_ride_service.service.RideService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="ride.recovery-delay-ms=3600000")
@AutoConfigureMockMvc
class ConnectedRideTest {
    @Autowired MockMvc mvc;
    @Autowired RideService rides;
    @Autowired RideRepository repository;
    @MockBean DriverGateway drivers;
    @MockBean AccountClient accounts;
    final Identity passenger=new Identity(1L,"Passenger","PASSENGER");
    final Identity driver=new Identity(2L,"Driver","DRIVER");
    @BeforeEach void setup(){
        repository.deleteAll();
        when(accounts.authenticate("Bearer passenger")).thenReturn(passenger);
        when(accounts.authenticate("Bearer driver")).thenReturn(driver);
        when(accounts.authenticate("Bearer stranger")).thenReturn(new Identity(99L,"Other","PASSENGER"));
        when(accounts.authenticate(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        when(drivers.reserve(anyString())).thenAnswer(call->new DriverGateway.Reservation(call.getArgument(0),10L,2L,false));
    }
    @Test void lifecycleOwnershipAndIdempotency() throws Exception {
        UUID key=UUID.randomUUID();
        var ride=rides.create(passenger,key,"A","B");
        assertThat(rides.create(passenger,key,"A","B").getId()).isEqualTo(ride.getId());
        verify(drivers,times(1)).reserve(anyString());
        mvc.perform(get("/api/rides/{id}",ride.getId()).header("Authorization","Bearer stranger")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/start",ride.getId()).header("Authorization","Bearer passenger")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/complete",ride.getId()).header("Authorization","Bearer driver")).andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/start",ride.getId()).header("Authorization","Bearer driver"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(patch("/api/rides/{id}/complete",ride.getId()).header("Authorization","Bearer driver"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.releasePending").value(false));
        verify(drivers).release(ride.getReservationId());
        assertThatThrownBy(()->rides.transition(ride.getId(),passenger,"cancel")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void cancellationPersistsReleaseForRetryAfterDriverOutage(){
        var ride=rides.create(passenger,UUID.randomUUID(),"A","B");
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).doNothing().when(drivers).release(ride.getReservationId());
        var cancelled=rides.transition(ride.getId(),passenger,"cancel");
        assertThat(cancelled.isReleasePending()).isTrue();
        rides.recover();
        var recovered=repository.findById(ride.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo("CANCELLED");
        assertThat(recovered.isReleasePending()).isFalse();
    }
    @Test void bookingFailureCompensatesPotentialRemoteReservation(){
        when(drivers.reserve(anyString())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(()->rides.create(passenger,UUID.randomUUID(),"A","B")).isInstanceOf(ResponseStatusException.class);
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getStatus()).isEqualTo("CANCELLED");
        verify(drivers).release(anyString());
    }
    @Test void bookingRejectsMissingAuthWrongRolesAndInvalidInput() throws Exception {
        mvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON).content("{\"pickup\":\"A\",\"destination\":\"B\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/rides").header("Authorization","Bearer driver").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickup\":\"A\",\"destination\":\"B\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/rides").header("Authorization","Bearer passenger").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickup\":\"\",\"destination\":\"B\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/rides").header("Authorization","Bearer passenger").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickup\":\"A\",\"destination\":\"B\",\"passengerAccountId\":999,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passengerAccountId").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
