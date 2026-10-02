param(
    [Parameter(Mandatory=$true)][ValidateSet('Account','Driver','Ride','Payment')][string]$Service,
    [ValidateSet('Connected','Standalone')][string]$Mode = 'Connected',
    [switch]$NoBuild
)
. "$PSScriptRoot/Common.ps1"
if (!$NoBuild) { Build-Services $Service }
Assert-FreePort $script:Ports[$Service]
Initialize-LocalConfig
$saved = Set-ServiceEnvironment $Service
try {
    $arguments = Get-ServiceArguments $Service $Mode
    Write-Host "Starting $Service on port $($script:Ports[$Service]) in $Mode mode. Ctrl+C stops it."
    & java @arguments
    if ($LASTEXITCODE -ne 0) { throw "$Service exited with code $LASTEXITCODE" }
} finally { Restore-ServiceEnvironment $saved }
