$ErrorActionPreference = 'Stop'

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$repoRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
$workspace = Join-Path $repoRoot 'spring-boot-iot-ui'
$logDir = Join-Path $repoRoot 'logs'
$logFile = Join-Path $logDir 'frontend-acceptance.log'

$nodeCmd = (Get-Command node -ErrorAction SilentlyContinue).Source
if (-not $nodeCmd) {
    throw 'Node executable was not found in PATH.'
}

$npmCmd = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
if (-not $npmCmd) {
    $npmCmd = (Get-Command npm -ErrorAction SilentlyContinue).Source
}
if (-not $npmCmd) {
    throw 'npm executable was not found in PATH.'
}

New-Item -ItemType Directory -Path $logDir -Force | Out-Null
Set-Location $workspace
$env:npm_config_cache = Join-Path $workspace '.npm-cache'

"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] starting frontend acceptance process" | Tee-Object -FilePath $logFile -Append
& $nodeCmd -v 2>&1 | Tee-Object -FilePath $logFile -Append
& $npmCmd -v 2>&1 | Tee-Object -FilePath $logFile -Append
& $npmCmd install 2>&1 | Tee-Object -FilePath $logFile -Append
& $npmCmd run acceptance:dev 2>&1 | Tee-Object -FilePath $logFile -Append
