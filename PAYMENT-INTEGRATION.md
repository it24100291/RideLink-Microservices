# Ride to Payment integration

Ride and Payment remain separate Spring Boot applications with separate databases. Ride calls Payment over HTTP using `payment.service.base-url` (default `http://localhost:8084`, override with `PAYMENT_SERVICE_URL`). Ride never reads Payment tables or repositories.

## Flow

1. The assigned driver accepts the ride, starts it, and completes it using the existing Ride lifecycle.
2. Submit the final distance and duration to `POST /api/rides/{rideId}/payment`. Ride passes its persisted ride ID to Payment's `POST /api/payments`; Payment calculates the fare and stores one `PENDING` payment for that ride.
3. Read that payment from `GET /api/rides/{rideId}/payment`.
4. Simulate a result with `PUT /api/rides/{rideId}/payment/status` and `{"status":"SUCCESS"}` or `{"status":"FAILED"}`.
5. Read a successful-payment receipt from `GET /api/rides/{rideId}/payment/receipt`.

Payment retains its standalone fare estimate at `POST /api/fares/calculate`. Creating a payment calculates and stores the fare.

## Failure and access behavior

Ride completion is persisted before payment is attempted and is not rolled back if Payment later becomes unavailable. A Payment outage returns HTTP 503 from the Ride payment endpoint. After an uncertain timeout, check `GET /api/rides/{rideId}/payment` before retrying. Payment enforces one payment record per ride with a database unique constraint; repeated creation returns HTTP 409. Invalid fare/status bodies return HTTP 400; unknown rides or payment records return HTTP 404. Invalid payment state transitions return HTTP 409. A receipt is created only after Payment accepts a transition to `SUCCESS`.

Ride's payment routes use the existing Account-issued bearer-token authentication and ride ownership checks. Payment's direct API remains unauthenticated and is a security task for later review. Payment results are simulated; no external payment provider is used.

## Starting services

From this directory, run `mvn clean test` to build and test the four-module reactor. Start all four in the background with `./scripts/Start-All.ps1`; stop them with `./scripts/Stop-All.ps1`. To run Payment alone: `mvn -pl payment-service spring-boot:run`. Other standalone commands are `mvn -pl account-service spring-boot:run`, `mvn -pl ridelink-driver-service spring-boot:run`, and `mvn -pl ridelink-ride-service spring-boot:run`.
