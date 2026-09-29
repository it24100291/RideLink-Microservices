param()
. "$PSScriptRoot/Common.ps1"
$accountUrl = "http://127.0.0.1:$($script:Ports.Account)"
$driverUrl = "http://127.0.0.1:$($script:Ports.Driver)"
$rideUrl = "http://127.0.0.1:$($script:Ports.Ride)"
function Api([string]$Method, [string]$Url, $Body, [string]$Token, [string]$Key) {
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    if ($Key) { $headers['Idempotency-Key'] = $Key }
    $options = @{ Method=$Method; Uri=$Url; Headers=$headers; ContentType='application/json'; TimeoutSec=10 }
    if ($null -ne $Body) { $options.Body = ($Body | ConvertTo-Json -Compress) }
    Invoke-RestMethod @options
}
function Assert-Equal($Actual, $Expected, [string]$Description) {
    if ($Actual -ne $Expected) { throw "$Description expected '$Expected', received '$Actual'." }
}
function Expect-Status([int]$Expected, [scriptblock]$Action) {
    try { & $Action | Out-Null }
    catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq $Expected) { return }
        throw
    }
    throw "Expected HTTP $Expected but the request succeeded."
}
$suffix = [guid]::NewGuid().ToString('N').Substring(0,10)
$password = 'WorkflowTest123!'
$driverEmail = "driver-$suffix@example.com"
$passengerEmail = "passenger-$suffix@example.com"
$otherEmail = "other-$suffix@example.com"
$driverAccount = Api POST "$accountUrl/api/accounts/register" @{ name='Workflow Driver';email=$driverEmail;password=$password;role='DRIVER' }
$passengerAccount = Api POST "$accountUrl/api/accounts/register" @{ name='Workflow Passenger';email=$passengerEmail;password=$password;role='PASSENGER' }
$null = Api POST "$accountUrl/api/accounts/register" @{ name='Other Passenger';email=$otherEmail;password=$password;role='PASSENGER' }
$driverToken = (Api POST "$accountUrl/api/auth/login" @{email=$driverEmail;password=$password}).accessToken
$passengerToken = (Api POST "$accountUrl/api/auth/login" @{email=$passengerEmail;password=$password}).accessToken
$otherToken = (Api POST "$accountUrl/api/auth/login" @{email=$otherEmail;password=$password}).accessToken
Expect-Status 401 { Api GET "$driverUrl/api/drivers/me" }
Expect-Status 401 { Api GET "$rideUrl/api/rides" }
$driver = Api POST "$driverUrl/api/drivers" @{name='Workflow Driver';licenseNumber="L-$suffix";available=$true;serviceArea='Colombo';currentLocation='Fort';accountId=999} $driverToken
Assert-Equal $driver.accountId $driverAccount.id 'Driver account ownership'
$null = Api POST "$driverUrl/api/drivers/$($driver.id)/vehicles" @{registrationNumber="V-$suffix";vehicleType='CAR';model='Toyota'} $driverToken
$available = @(Api GET "$driverUrl/api/drivers/available" $null $passengerToken)
if ($available.Count -ne 1) { throw 'Run this deterministic smoke test using a fresh RIDELINK_HOME with no other available drivers.' }
$key = [guid]::NewGuid().ToString()
$ride = Api POST "$rideUrl/api/rides" @{pickup='Fort';destination='SLIIT';passengerAccountId=999;status='COMPLETED'} $passengerToken $key
Assert-Equal $ride.status 'ASSIGNED' 'Booking status'
Assert-Equal $ride.passengerAccountId $passengerAccount.id 'Passenger identity'
Assert-Equal $ride.driverAccountId $driverAccount.id 'Assigned driver identity'
$retry = Api POST "$rideUrl/api/rides" @{pickup='Fort';destination='SLIIT'} $passengerToken $key
Assert-Equal $retry.id $ride.id 'Idempotent booking'
Expect-Status 403 { Api GET "$rideUrl/api/rides/$($ride.id)" $null $otherToken }
Expect-Status 403 { Api PATCH "$rideUrl/api/rides/$($ride.id)/start" $null $passengerToken }
Expect-Status 409 { Api PATCH "$rideUrl/api/rides/$($ride.id)/start" $null $driverToken }
Expect-Status 409 { Api POST "$rideUrl/api/rides" @{pickup='A';destination='B'} $otherToken ([guid]::NewGuid().ToString()) }
Expect-Status 409 { Api PATCH "$driverUrl/api/drivers/$($driver.id)/availability?available=true" $null $driverToken }
$accepted = Api PATCH "$rideUrl/api/rides/$($ride.id)/accept" $null $driverToken
Assert-Equal $accepted.status 'ACCEPTED' 'Ride acceptance'
$started = Api PATCH "$rideUrl/api/rides/$($ride.id)/start" $null $driverToken
Assert-Equal $started.status 'IN_PROGRESS' 'Ride start'
$completed = Api PATCH "$rideUrl/api/rides/$($ride.id)/complete" $null $driverToken
Assert-Equal $completed.status 'COMPLETED' 'Ride completion'
Assert-Equal $completed.releasePending $false 'Driver released'
$driver = Api GET "$driverUrl/api/drivers/me" $null $driverToken
Assert-Equal $driver.available $true 'Driver available again'
Expect-Status 401 { Api POST "$rideUrl/api/rides/$($ride.id)/payment" @{distanceKm=10;durationMinutes=20;paymentMethod='CARD'} }
Expect-Status 403 { Api POST "$rideUrl/api/rides/$($ride.id)/payment" @{distanceKm=10;durationMinutes=20;paymentMethod='CARD'} $otherToken }
$payment = Api POST "$rideUrl/api/rides/$($ride.id)/payment" @{distanceKm=10;durationMinutes=20;paymentMethod='CARD'} $passengerToken
Assert-Equal $payment.rideId $ride.id 'Payment ride link'
Assert-Equal $payment.amount 22.00 'Simulated fare'
Assert-Equal $payment.status 'PENDING' 'Payment initial status'
$payment = Api GET "$rideUrl/api/rides/$($ride.id)/payment" $null $passengerToken
Assert-Equal $payment.rideId $ride.id 'Payment retrieval'
$payment = Api PUT "$rideUrl/api/rides/$($ride.id)/payment/status" @{status='SUCCESS'} $passengerToken
Assert-Equal $payment.status 'SUCCESS' 'Simulated payment result'
$receipt = Api GET "$rideUrl/api/rides/$($ride.id)/payment/receipt" $null $passengerToken
Assert-Equal $receipt.rideId $ride.id 'Payment receipt'
$second = Api POST "$rideUrl/api/rides" @{pickup='Fort';destination='Airport'} $passengerToken ([guid]::NewGuid().ToString())
$cancelled = Api PATCH "$rideUrl/api/rides/$($second.id)/cancel" $null $passengerToken
Assert-Equal $cancelled.status 'CANCELLED' 'Cancellation'
$null = Api PATCH "$accountUrl/api/accounts/me/password" @{currentPassword=$password;newPassword='WorkflowChanged456!'} $passengerToken
Expect-Status 401 { Api GET "$rideUrl/api/rides" $null $passengerToken }
$passengerToken = (Api POST "$accountUrl/api/auth/login" @{email=$passengerEmail;password='WorkflowChanged456!'}).accessToken
$null = Api GET "$rideUrl/api/rides/$($ride.id)" $null $passengerToken
Write-Host 'PASS: registration, login, identity links, vehicle, booking, retry, ownership, reservation, completion, Ride-to-Payment HTTP integration, payment authorization, cancellation, and cross-service token revocation.'
Write-Host "Completed ride ID: $($ride.id). Test accounts use suffix $suffix."
