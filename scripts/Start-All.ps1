param([switch]$NoBuild)
. "$PSScriptRoot/Common.ps1"
foreach ($service in @('Account','Driver','Ride')) { Assert-FreePort $script:Ports[$service] }
if (!$NoBuild) { Build-Services }
Initialize-LocalConfig
$statePath = Join-Path $script:LocalHome 'running.json'
if (Test-Path -LiteralPath $statePath) { throw 'A startup record already exists. Run Stop-All.ps1 before starting again.' }
$started = @()
try {
    foreach ($service in @('Account','Driver','Ride')) {
        $saved = Set-ServiceEnvironment $service
        try {
            $arguments = Get-ServiceArguments $service 'Connected'
            $quoted = ($arguments | ForEach-Object { '"' + $_ + '"' }) -join ' '
            $process = Start-Process -FilePath (Get-Command java).Source -ArgumentList $quoted -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:LocalHome "logs/$service.out.log") -RedirectStandardError (Join-Path $script:LocalHome "logs/$service.err.log")
            $started += @{ Service=$service; Pid=$process.Id; StartTime=$process.StartTime.ToUniversalTime().ToString('o'); Jar=(Get-ServiceJar $service) }
            Wait-Service $service $process
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
