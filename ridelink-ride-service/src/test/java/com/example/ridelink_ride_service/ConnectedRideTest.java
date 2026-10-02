package com.example.ridelink_ride_service;
import com.example.ridelink_ride_service.client.DriverGateway;
import com.example.ridelink_ride_service.client.PaymentGateway;
import com.example.ridelink_ride_service.repository.RideRepository;
import com.example.ridelink_ride_service.model.Ride;
import com.example.ridelink_ride_service.model.RideStatus;
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
import java.math.BigDecimal;
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
    @MockBean PaymentGateway payments;
    @MockBean AccountClient accounts;
    final Identity passenger=new Identity(1L,"Passenger","PASSENGER");
    final Identity driver=new Identity(2L,"Driver","DRIVER");
    @BeforeEach void setup(){
        repository.deleteAll();
        when(accounts.authenticate("Bearer passenger")).thenReturn(passenger);
        when(accounts.authenticate("Bearer driver")).thenReturn(driver);
        when(accounts.authenticate("Bearer stranger")).thenReturn(new Identity(99L,"Other","PASSENGER"));
        when(accounts.authenticate("Bearer other-driver")).thenReturn(new Identity(3L,"Other Driver","DRIVER"));
        when(accounts.authenticate(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        when(drivers.reserve(anyString())).thenAnswer(call->new DriverGateway.Reservation(call.getArgument(0),10L,2L,false));
        when(payments.create(anyLong(), any(), anyInt(), any())).thenAnswer(call ->
                new PaymentGateway.Payment(77L, call.getArgument(0), new BigDecimal("22.00"),
                        PaymentGateway.PaymentMethod.CARD, PaymentGateway.PaymentStatus.PENDING, "ref-77"));
        when(payments.get(anyLong())).thenAnswer(call ->
                new PaymentGateway.Payment(77L, call.getArgument(0), new BigDecimal("22.00"),
                        PaymentGateway.PaymentMethod.CARD, PaymentGateway.PaymentStatus.PENDING, "ref-77"));
        when(payments.updateStatus(anyLong(), any())).thenAnswer(call ->
                new PaymentGateway.Payment(77L, call.getArgument(0), new BigDecimal("22.00"),
                        PaymentGateway.PaymentMethod.CARD, call.getArgument(1), "ref-77"));
        when(payments.getReceipt(anyLong())).thenAnswer(call ->
                new PaymentGateway.Receipt(77L, "receipt-77", call.getArgument(0), new BigDecimal("22.00"), "ref-77", "2026-01-01T00:00:00Z"));
    }
    @Test void lifecycleOwnershipAndIdempotency() throws Exception {
        UUID key=UUID.randomUUID();
        var ride=rides.create(passenger,key,"A","B");
        assertThat(ride.getStatus()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(ride.getDriverAccountId()).isEqualTo(driver.accountId());
        assertThat(rides.create(passenger,key,"A","B").getId()).isEqualTo(ride.getId());
        verify(drivers,times(1)).reserve(anyString());
        mvc.perform(get("/api/rides/{id}",ride.getId()).header("Authorization","Bearer stranger")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/accept",ride.getId())).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/rides/{id}/accept",ride.getId()).header("Authorization","Bearer passenger"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/accept",ride.getId()).header("Authorization","Bearer other-driver"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/start",ride.getId()).header("Authorization","Bearer passenger")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/start",ride.getId())).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/rides/{id}/accept",ride.getId()).header("Authorization","Bearer other-driver"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/accept",ride.getId()).header("Authorization","Bearer driver"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(patch("/api/rides/{id}/accept",ride.getId()).header("Authorization","Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/complete",ride.getId()).header("Authorization","Bearer driver")).andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/complete",ride.getId()).header("Authorization","Bearer passenger")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/rides/{id}/complete",ride.getId())).andExpect(status().isUnauthorized());
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
        assertThat(recovered.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(recovered.isReleasePending()).isFalse();
    }
    @Test void bookingFailureCompensatesPotentialRemoteReservation(){
        when(drivers.reserve(anyString())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(()->rides.create(passenger,UUID.randomUUID(),"A","B")).isInstanceOf(ResponseStatusException.class);
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getStatus()).isEqualTo(RideStatus.CANCELLED);
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
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test void bookingPersistsRequestedBeforeDriverReservationAndThenAssigns() {
        doAnswer(call -> {
            Ride persisted = repository.findByReservationId(call.getArgument(0)).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(RideStatus.REQUESTED);
            return new DriverGateway.Reservation(call.getArgument(0), 10L, 2L, false);
        }).when(drivers).reserve(anyString());

        Ride ride = rides.create(passenger, UUID.randomUUID(), "A", "B");

        assertThat(ride.getStatus()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(repository.findById(ride.getId()).orElseThrow().getStatus()).isEqualTo(RideStatus.ASSIGNED);
    }

    @Test void onlyValidLifecycleTransitionsAreAccepted() throws Exception {
        Ride requested = new Ride("requested-test", passenger.accountId(), passenger.name(), "A", "B");
        requested.setDriverAccountId(driver.accountId());
        requested = repository.saveAndFlush(requested);
        mvc.perform(patch("/api/rides/{id}/accept", requested.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/start", requested.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(patch("/api/rides/{id}/complete", requested.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());

        Ride assigned = rides.create(passenger, UUID.randomUUID(), "A", "B");
        mvc.perform(patch("/api/rides/{id}/start", assigned.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/complete", assigned.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        rides.transition(assigned.getId(), driver, "accept");
        mvc.perform(patch("/api/rides/{id}/complete", assigned.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        rides.transition(assigned.getId(), driver, "start");
        mvc.perform(patch("/api/rides/{id}/accept", assigned.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        rides.transition(assigned.getId(), driver, "complete");
        mvc.perform(patch("/api/rides/{id}/cancel", assigned.getId()).header("Authorization", "Bearer passenger"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/start", assigned.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/complete", assigned.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());

        Ride cancelled = rides.create(passenger, UUID.randomUUID(), "A", "B");
        rides.transition(cancelled.getId(), passenger, "cancel");
        mvc.perform(patch("/api/rides/{id}/accept", cancelled.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/start", cancelled.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/rides/{id}/complete", cancelled.getId()).header("Authorization", "Bearer driver"))
                .andExpect(status().isConflict());
    }

    @Test void legacyStoredStatusesAreExposedAsTheirEquivalentActiveStates() {
        Ride oldPending = new Ride("legacy-pending", passenger.accountId(), passenger.name(), "A", "B");
        org.springframework.test.util.ReflectionTestUtils.setField(oldPending, "status", "PENDING");
        Ride oldConfirmed = new Ride("legacy-confirmed", passenger.accountId(), passenger.name(), "A", "B");
        org.springframework.test.util.ReflectionTestUtils.setField(oldConfirmed, "status", "CONFIRMED");
        repository.saveAllAndFlush(java.util.List.of(oldPending, oldConfirmed));

        assertThat(repository.findByReservationId("legacy-pending").orElseThrow().getStatus())
                .isEqualTo(RideStatus.REQUESTED);
        assertThat(repository.findByReservationId("legacy-confirmed").orElseThrow().getStatus())
                .isEqualTo(RideStatus.ASSIGNED);
    }

    @Test void acceptedRideCanBeCancelledAndReleasesItsDriver() {
        Ride ride = rides.create(passenger, UUID.randomUUID(), "A", "B");
        rides.transition(ride.getId(), driver, "accept");

        Ride cancelled = rides.transition(ride.getId(), passenger, "cancel");

        assertThat(cancelled.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(cancelled.isReleasePending()).isFalse();
        verify(drivers).release(ride.getReservationId());
    }

    @Test void requestedRideCanBeCancelledAndReservationReleaseIsIdempotentlyQueued() {
        Ride requested = new Ride("requested-cancel-test", passenger.accountId(), passenger.name(), "A", "B");
        requested = repository.saveAndFlush(requested);

        Ride cancelled = rides.transition(requested.getId(), passenger, "cancel");

        assertThat(cancelled.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(cancelled.isReleasePending()).isFalse();
        verify(drivers).release("requested-cancel-test");
    }

    @Test void completedRideUsesRideIdForPaymentAndCanRetrieveReceipt() throws Exception {
        var ride = rides.create(passenger, UUID.randomUUID(), "A", "B");
        rides.transition(ride.getId(), driver, "accept");
        rides.transition(ride.getId(), driver, "start");
        rides.transition(ride.getId(), driver, "complete");

        mvc.perform(post("/api/rides/{id}/payment", ride.getId()).header("Authorization", "Bearer passenger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distanceKm\":10,\"durationMinutes\":20,\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rideId").value(ride.getId()))
                .andExpect(jsonPath("$.amount").value(22.00));
        verify(payments).create(eq(ride.getId()), eq(new BigDecimal("10")), eq(20), eq(PaymentGateway.PaymentMethod.CARD));

        mvc.perform(get("/api/rides/{id}/payment", ride.getId()).header("Authorization", "Bearer passenger"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rideId").value(ride.getId()));
        mvc.perform(put("/api/rides/{id}/payment/status", ride.getId()).header("Authorization", "Bearer passenger")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(get("/api/rides/{id}/payment/receipt", ride.getId()).header("Authorization", "Bearer passenger"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.receiptNumber").value("receipt-77"));
    }

    @Test void paymentOutageDoesNotUndoCompletedRide() throws Exception {
        var ride = rides.create(passenger, UUID.randomUUID(), "A", "B");
        rides.transition(ride.getId(), driver, "accept");
        rides.transition(ride.getId(), driver, "start");
        rides.transition(ride.getId(), driver, "complete");
        when(payments.create(anyLong(), any(), anyInt(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment service unavailable"));

        mvc.perform(post("/api/rides/{id}/payment", ride.getId()).header("Authorization", "Bearer passenger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distanceKm\":10,\"durationMinutes\":20,\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(repository.findById(ride.getId()).orElseThrow().getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    @Test void paymentCannotBeCreatedBeforeRideCompletionAndInvalidFareIsRejected() throws Exception {
        var ride = rides.create(passenger, UUID.randomUUID(), "A", "B");
        mvc.perform(post("/api/rides/{id}/payment", ride.getId()).header("Authorization", "Bearer passenger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distanceKm\":10,\"durationMinutes\":20,\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isConflict());
        rides.transition(ride.getId(), driver, "accept");
        rides.transition(ride.getId(), driver, "start");
        rides.transition(ride.getId(), driver, "complete");
        mvc.perform(post("/api/rides/{id}/payment", ride.getId()).header("Authorization", "Bearer passenger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distanceKm\":0,\"durationMinutes\":-1,\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isBadRequest());
        verify(payments, never()).create(anyLong(), any(), anyInt(), any());
    }
}
