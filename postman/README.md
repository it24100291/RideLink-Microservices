# RideLink Postman demo

This collection demonstrates account registration and JWT login, driver setup, fare calculation, the ride lifecycle, Ride's Payment proxy, and negative authorization/state checks. It uses fictional `example.test` accounts and a demo password; do not reuse real credentials.

## Start the services

From the `RideLink-Microservices` directory, use a fresh local data directory for this demo. That keeps the driver reservation deterministic if you have run the services before:

```powershell
$rideLinkDemoHome = Join-Path $env:TEMP ("ridelink-postman-" + [guid]::NewGuid().ToString('N'))
$env:RIDELINK_HOME = $rideLinkDemoHome
.\scripts\Start-All.ps1
```

This builds and tests the Maven reactor, creates local development credentials and fresh H2 files in that temporary directory, then starts the four services. Stop them when finished:

```powershell
.\scripts\Stop-All.ps1
Remove-Item Env:\RIDELINK_HOME
```

After stopping all services, you can remove the temporary directory with `Remove-Item -LiteralPath $rideLinkDemoHome -Recurse -Force` if you no longer need its local demo databases and generated credentials.

Default local URLs from the service configuration are Account `http://localhost:8083`, Driver `http://localhost:8081`, Ride `http://localhost:8082`, and Payment `http://localhost:8084`.

## Import and environment

Import `RideLink.postman_collection.json` and `RideLink.postman_environment.json` into Postman, then select **RideLink Local**. The environment contains the four service URLs and blank fields for captured IDs and JWTs. The collection scripts populate IDs/tokens as you run requests.

The Fare API is an internal Payment endpoint protected by `X-Service-Key`. Its clearly labeled request uses the blank `paymentServiceKey` environment variable. To run it, copy the local development key from `$rideLinkDemoHome/payment-service.key` into that variable in Postman. Do not commit or export an environment after entering the key. Normal payment requests do not use this variable: they call Ride's JWT-protected proxy, and Ride adds its internal key itself.

## Recommended execution order

1. **Account:** run Register Passenger, Register Driver Account, Register Other Passenger, then the three Login requests. Finish with Get Passenger Profile. Registration scripts generate fresh demo emails; login requests capture their Bearer tokens.
2. **Driver & Vehicle:** create the driver profile, add a vehicle, set availability false then true, and list available drivers.
3. **Ride Lifecycle:** Request Ride, Get Ride, Accept Assigned Ride, Start Ride, and Complete Ride in order.
4. **Fare:** to demonstrate fare estimation, set the local-only key as described above, then run Calculate Fare Estimate for the captured `rideId`. If `paymentServiceKey` is blank, Payment correctly returns 401; skip this request when you do not want to load the local key into Postman.
5. **Payment:** create the payment for the completed ride, retrieve it, simulate `SUCCESS`, then retrieve the receipt.
6. **Negative Scenarios:** run the folder in order. It creates a separate assigned ride, tests missing JWT, invalid input, wrong role/owner, invalid transition, payment before completion, then cancels that separate ride. Run Duplicate Payment after the successful payment was created in the Payment folder.

A successful booking is saved as `REQUESTED` before driver reservation. Booking reserves an available driver synchronously, so the create response is normally already `ASSIGNED`. The remaining demonstrated transitions are `ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED`; cancellation is demonstrated on a separate assigned ride.

## Expected negative results

- Missing Ride JWT: **401**.
- Empty pickup in ride booking: **400**.
- Passenger attempts driver accept action: **403**.
- Another passenger requests the negative-test ride's payment: **403**.
- Start before accept, or create payment before completion: **409**.
- Second payment for the completed ride: **409** (one payment per ride).
- Cancelling the separate assigned ride: **200**, with `CANCELLED` and driver release complete.

## Assignment evidence screenshots

Capture screenshots showing:

- Account registration and successful login responses, with the bearer token value hidden.
- Driver profile, vehicle, and available-driver responses.
- Fare response (hide the internal key if the header is visible).
- Ride responses at `ASSIGNED`, `ACCEPTED`, `IN_PROGRESS`, and `COMPLETED`.
- Successful Ride payment creation, `SUCCESS` status, and receipt.
- Representative negative responses with their status codes: 401, 403, 400, and 409.

Do not include passwords, JWTs, or `X-Service-Key` values in submitted screenshots or exports.
