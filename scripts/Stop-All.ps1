param()
. "$PSScriptRoot/Common.ps1"
$statePath = Join-Path $script:LocalHome 'running.json'
if (!(Test-Path -LiteralPath $statePath)) { Write-Host 'No combined startup record found.'; return }
$entries = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
foreach ($entry in $entries) {
    $process = Get-Process -Id $entry.Pid -ErrorAction SilentlyContinue
    if (!$process) { continue }
    $command = Get-CimInstance Win32_Process -Filter "ProcessId = $($entry.Pid)"
    if ($process.StartTime.ToUniversalTime().ToString('o') -ne $entry.StartTime -or !$command.CommandLine.Contains($entry.Jar)) {
        throw "Process $($entry.Pid) does not match the recorded service; it was not stopped."
    }
    Stop-Process -Id $entry.Pid
    Write-Host "$($entry.Service) stopped."
}
Remove-Item -LiteralPath $statePath
