param([switch]$NoBuild)
. "$PSScriptRoot/Common.ps1"
$script:Modules.Payment = 'payment-service'
$script:Ports.Payment = 8084
$services = @('Account','Driver','Ride','Payment')

function Wait-PaymentService($Process) {
    for ($i=0; $i -lt 120; $i++) {
        if ($Process.HasExited) { throw "Payment exited. See logs in $script:LocalHome/logs." }
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$($script:Ports.Payment)/actuator/health" -TimeoutSec 1
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    throw "Payment did not become healthy. See logs in $script:LocalHome/logs."
}

foreach ($service in $services) { Assert-FreePort $script:Ports[$service] }
if (!$NoBuild) { Build-Services }
Initialize-LocalConfig
$statePath = Join-Path $script:LocalHome 'running.json'
if (Test-Path -LiteralPath $statePath) { throw 'A startup record already exists. Run Stop-All.ps1 before starting again.' }
$started = @()
try {
    foreach ($service in $services) {
        $saved = Set-ServiceEnvironment $service
        try {
            if ($service -eq 'Payment') {
                $saved.RIDE_SERVICE_URL = [Environment]::GetEnvironmentVariable('RIDE_SERVICE_URL', 'Process')
                [Environment]::SetEnvironmentVariable('RIDE_SERVICE_URL', "http://127.0.0.1:$($script:Ports.Ride)", 'Process')
            }
            $arguments = Get-ServiceArguments $service 'Connected'
            $quoted = ($arguments | ForEach-Object { '"' + $_ + '"' }) -join ' '
            $process = Start-Process -FilePath (Get-Command java).Source -ArgumentList $quoted -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:LocalHome "logs/$service.out.log") -RedirectStandardError (Join-Path $script:LocalHome "logs/$service.err.log")
            $started += @{ Service=$service; Pid=$process.Id; StartTime=$process.StartTime.ToUniversalTime().ToString('o'); Jar=(Get-ServiceJar $service) }
            if ($service -eq 'Payment') { Wait-PaymentService $process }
            else { Wait-Service $service $process }
            Write-Host "$service ready on http://localhost:$($script:Ports[$service])"
        } finally { Restore-ServiceEnvironment $saved }
    }
    $started | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8
    Write-Host "All services are running. Logs and persistent local databases: $script:LocalHome"
    Write-Host 'Stop them with: .\scripts\Stop-All.ps1'
} catch {
    foreach ($entry in $started) { Stop-Process -Id $entry.Pid -ErrorAction SilentlyContinue }
    throw
}
