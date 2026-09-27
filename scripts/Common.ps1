$ErrorActionPreference = 'Stop'
$script:RepoRoot = Split-Path $PSScriptRoot -Parent
$script:LocalHome = if ($env:RIDELINK_HOME) { [IO.Path]::GetFullPath($env:RIDELINK_HOME) } else { Join-Path $env:USERPROFILE '.ridelink-connected' }
$script:Modules = @{ Account = 'account-service'; Driver = 'ridelink-driver-service'; Ride = 'ridelink-ride-service' }
$script:Ports = @{ Account = 8083; Driver = 8081; Ride = 8082 }
foreach ($serviceName in @('Account','Driver','Ride')) {
    $configuredPort = [Environment]::GetEnvironmentVariable("RIDELINK_$($serviceName.ToUpper())_PORT")
    if ($configuredPort) { $script:Ports[$serviceName] = [int]$configuredPort }
}

function Initialize-LocalConfig {
    & java (Join-Path $PSScriptRoot 'LocalSetup.java') $script:LocalHome
    if ($LASTEXITCODE -ne 0) { throw 'Local credential setup failed. Java 21 is required.' }
}
function Build-Services([string]$Service) {
    $pom = if ($Service) { Join-Path $script:RepoRoot "$($script:Modules[$Service])/pom.xml" } else { Join-Path $script:RepoRoot 'pom.xml' }
    & mvn -f $pom verify
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed; services were not started.' }
}
function Assert-FreePort([int]$Port) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $client.Connect('127.0.0.1', $Port)
        throw "Port $Port is already in use. Stop that service before starting another instance."
    } catch [Net.Sockets.SocketException] {
        # Connection refused means the port is available.
    } finally { $client.Dispose() }
}
function Get-ServiceJar([string]$Service) {
    $jar = Join-Path $script:RepoRoot "$($script:Modules[$Service])/target/$($script:Modules[$Service])-0.0.1-SNAPSHOT.jar"
    if (!(Test-Path -LiteralPath $jar)) { throw "Build required: missing $jar" }
    return $jar
}
function Set-ServiceEnvironment([string]$Service) {
    $values = @{
        ACCOUNT_SERVICE_KEY = [IO.File]::ReadAllText((Join-Path $script:LocalHome 'account-service.key')).Trim()
        RIDE_SERVICE_KEY = [IO.File]::ReadAllText((Join-Path $script:LocalHome 'ride-service.key')).Trim()
        ACCOUNT_SERVICE_URL = "http://127.0.0.1:$($script:Ports.Account)"
        DRIVER_SERVICE_URL = "http://127.0.0.1:$($script:Ports.Driver)"
        ACCOUNT_JWT_PRIVATE_KEY = $null
        ACCOUNT_JWT_PUBLIC_KEY = $null
        SPRING_PROFILES_ACTIVE = $null
    }
    if ($Service -eq 'Account') {
        $values.ACCOUNT_JWT_PRIVATE_KEY = [IO.File]::ReadAllText((Join-Path $script:LocalHome 'private.pem'))
        $values.ACCOUNT_JWT_PUBLIC_KEY = [IO.File]::ReadAllText((Join-Path $script:LocalHome 'public.pem'))
        $values.RIDE_SERVICE_KEY = $null
    }
    $saved = @{}
    foreach ($name in $values.Keys) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process')
    }
    return $saved
}
function Restore-ServiceEnvironment($Saved) {
    foreach ($name in $Saved.Keys) { [Environment]::SetEnvironmentVariable($name, $Saved[$name], 'Process') }
}
function Get-ServiceArguments([string]$Service, [string]$Mode) {
    $jar = Get-ServiceJar $Service
    $arguments = @('-jar', $jar, "--server.port=$($script:Ports[$Service])", '--server.address=127.0.0.1')
    if ($Mode -eq 'Standalone' -and $Service -ne 'Account') {
        $arguments += '--spring.profiles.active=standalone'
    } else {
        $data = (Join-Path $script:LocalHome "data/$($Service.ToLower())").Replace('\','/')
        $arguments += "--spring.datasource.url=jdbc:h2:file:$data"
    }
    return $arguments
}
function Wait-Service([string]$Service, $Process) {
    for ($i=0; $i -lt 120; $i++) {
        if ($Process.HasExited) { throw "$Service exited. See logs in $script:LocalHome/logs." }
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$($script:Ports[$Service])/health" -TimeoutSec 1
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    throw "$Service did not become healthy. See logs in $script:LocalHome/logs."
}
